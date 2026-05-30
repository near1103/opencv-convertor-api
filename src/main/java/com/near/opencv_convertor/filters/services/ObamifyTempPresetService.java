package com.near.opencv_convertor.filters.services;

import com.near.opencv_convertor.dto.ObamifyTempPresetDto;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfFloat;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
public class ObamifyTempPresetService {

    private static final String TMP_PREFIX = "tmp:";
    private static final int[] PRESET_SIZES = {128, 256};

    private final Path tempPresetRoot = Path.of("runtime-data", "obamify", "tmp");

    public ObamifyTempPresetDto createTempPreset(
            MultipartFile file,
            String mode,
            String priority,
            String priorityRegions,
            String priorityPolygons,
            MultipartFile priorityMask
    ){
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Preset source image is empty");
        }

        String resolvedMode = isBlank(mode) ? "object_text" : mode.trim();
        String resolvedPriority = isBlank(priority) ? "none" : priority.trim();

        String presetId = UUID.randomUUID().toString().replace("-", "");
        Path presetDir = tempPresetRoot.resolve(presetId);

        try {
            Files.createDirectories(presetDir);

            BufferedImage original = ImageIO.read(file.getInputStream());

            if (original == null) {
                throw new IllegalArgumentException("Failed to decode preset source image");
            }

            for (int size : PRESET_SIZES) {
                PresetBuildResult result = buildPresetImages(
                        original,
                        size,
                        resolvedMode,
                        resolvedPriority,
                        priorityRegions,
                        priorityPolygons,
                        priorityMask
                );

                ImageIO.write(
                        matToBufferedImage(result.target()),
                        "png",
                        presetDir.resolve("target" + size + ".png").toFile()
                );

                ImageIO.write(
                        matToBufferedImage(result.weights()),
                        "png",
                        presetDir.resolve("weights" + size + ".png").toFile()
                );

                result.release();
            }

            return new ObamifyTempPresetDto(
                    TMP_PREFIX + presetId,
                    presetId,
                    "Temporary preset",
                    "Generated from uploaded image"
            );
        } catch (Exception e) {
            throw new RuntimeException("Failed to create temporary obamify preset: " + e.getMessage(), e);
        }
    }

    public BufferedImage readTempPresetImage(String presetId, String filename) throws IOException {
        if (isBlank(presetId) || !presetId.matches("^[a-zA-Z0-9_-]+$")) {
            throw new IllegalArgumentException("Invalid temporary preset id: " + presetId);
        }

        if (isBlank(filename) || !filename.matches("^target\\d+\\.png$|^weights\\d+\\.png$")) {
            throw new IllegalArgumentException("Invalid preset filename: " + filename);
        }

        Path imagePath = tempPresetRoot.resolve(presetId).resolve(filename).normalize();

        if (!imagePath.startsWith(tempPresetRoot.normalize())) {
            throw new IllegalArgumentException("Invalid preset image path");
        }

        if (!Files.exists(imagePath)) {
            throw new IOException("Temporary preset image not found: " + imagePath);
        }

        BufferedImage image = ImageIO.read(imagePath.toFile());

        if (image == null) {
            throw new IOException("Failed to read temporary preset image: " + imagePath);
        }

        return image;
    }

    private PresetBuildResult buildPresetImages(
            BufferedImage original,
            int size,
            String mode,
            String priority,
            String priorityRegions,
            String priorityPolygons,
            MultipartFile priorityMaskFile
    ) {
        ModeConfig config = ModeConfig.from(mode);

        Mat originalBgr = bufferedImageToBgrMat(original);

        Mat target = "cover".equals(config.fit())
                ? resizeCover(originalBgr, size)
                : resizeContain(originalBgr, size);

        Mat foregroundMask = buildForegroundMask(target, config);
        Mat textMask = buildTextMask(target, config);

        Mat priorityMask = buildPriorityMask(
                originalBgr,
                size,
                config.fit(),
                priority,
                priorityRegions,
                priorityPolygons,
                priorityMaskFile,
                config.priorityDilate()
        );

        Mat combinedMask = new Mat();
        Core.bitwise_or(foregroundMask, textMask, combinedMask);

        if (!"none".equals(priority)) {
            Core.bitwise_or(combinedMask, priorityMask, combinedMask);
        }

        Mat weights = makeWeightMap(combinedMask, config.weightStrength());

        originalBgr.release();
        foregroundMask.release();
        textMask.release();
        priorityMask.release();
        combinedMask.release();

        return new PresetBuildResult(target, weights);
    }

    private Mat resizeContain(Mat source, int size) {
        int width = source.cols();
        int height = source.rows();

        double scale = (double) size / Math.max(width, height);

        int newWidth = Math.max(1, (int) Math.round(width * scale));
        int newHeight = Math.max(1, (int) Math.round(height * scale));

        Mat resized = new Mat();
        Imgproc.resize(source, resized, new Size(newWidth, newHeight), 0, 0, Imgproc.INTER_AREA);

        Mat canvas = Mat.zeros(size, size, source.type());

        int x = (size - newWidth) / 2;
        int y = (size - newHeight) / 2;

        Rect roi = new Rect(x, y, newWidth, newHeight);
        resized.copyTo(canvas.submat(roi));

        resized.release();

        return canvas;
    }

    private Mat resizeCover(Mat source, int size) {
        int width = source.cols();
        int height = source.rows();

        double scale = (double) size / Math.min(width, height);

        int newWidth = Math.max(1, (int) Math.round(width * scale));
        int newHeight = Math.max(1, (int) Math.round(height * scale));

        Mat resized = new Mat();
        Imgproc.resize(source, resized, new Size(newWidth, newHeight), 0, 0, Imgproc.INTER_AREA);

        int x = Math.max((newWidth - size) / 2, 0);
        int y = Math.max((newHeight - size) / 2, 0);

        Rect crop = new Rect(x, y, size, size);

        Mat result = resized.submat(crop).clone();

        resized.release();

        return result;
    }

    private Mat buildForegroundMask(Mat target, ModeConfig config) {
        if ("full".equals(config.foreground())) {
            return buildFullMask(target.rows(), target.cols(), 0.0);
        }

        return buildGrabCutMask(target, config);
    }

    private Mat buildFullMask(int height, int width, double borderMargin) {
        Mat mask = Mat.zeros(height, width, CvType.CV_8UC1);

        int mx = Math.max(0, (int) Math.round(width * borderMargin));
        int my = Math.max(0, (int) Math.round(height * borderMargin));

        Imgproc.rectangle(
                mask,
                new org.opencv.core.Point(mx, my),
                new org.opencv.core.Point(width - mx - 1, height - my - 1),
                new Scalar(255),
                -1
        );

        return mask;
    }

    private Mat buildGrabCutMask(Mat target, ModeConfig config) {
        int width = target.cols();
        int height = target.rows();

        Mat grabCutMask = Mat.zeros(height, width, CvType.CV_8UC1);
        Mat bgModel = new Mat();
        Mat fgModel = new Mat();

        int marginX = Math.max(4, (int) Math.round(width * config.grabCutMargin()));
        int marginY = Math.max(4, (int) Math.round(height * config.grabCutMargin()));

        Rect rect = new Rect(
                marginX,
                marginY,
                Math.max(1, width - marginX * 2),
                Math.max(1, height - marginY * 2)
        );

        try {
            Imgproc.grabCut(
                    target,
                    grabCutMask,
                    rect,
                    bgModel,
                    fgModel,
                    config.grabCutIterations(),
                    Imgproc.GC_INIT_WITH_RECT
            );

            Mat result = Mat.zeros(height, width, CvType.CV_8UC1);

            byte[] data = new byte[(int) grabCutMask.total()];
            grabCutMask.get(0, 0, data);

            byte[] out = new byte[data.length];

            for (int i = 0; i < data.length; i++) {
                int value = data[i] & 0xFF;

                boolean foreground =
                        value == Imgproc.GC_FGD ||
                                value == Imgproc.GC_PR_FGD;

                out[i] = (byte) (foreground ? 255 : 0);
            }

            result.put(0, 0, out);

            result = morphologyClose(result, 2, 5);
            result = morphologyOpen(result, 1, 5);

            if (config.maskDilate() > 0) {
                result = dilate(result, config.maskDilate(), 5);
            }

            if (config.maskErode() > 0) {
                result = erode(result, config.maskErode(), 5);
            }

            return result;
        } catch (Exception e) {
            Mat fallback = new Mat();
            Imgproc.cvtColor(target, fallback, Imgproc.COLOR_BGR2GRAY);
            Imgproc.threshold(fallback, fallback, 15, 255, Imgproc.THRESH_BINARY);
            return fallback;
        } finally {
            grabCutMask.release();
            bgModel.release();
            fgModel.release();
        }
    }

    private Mat buildTextMask(Mat target, ModeConfig config) {
        if (!"auto".equals(config.text())) {
            return Mat.zeros(target.rows(), target.cols(), CvType.CV_8UC1);
        }

        Mat hsv = new Mat();
        Imgproc.cvtColor(target, hsv, Imgproc.COLOR_BGR2HSV);

        Mat white = new Mat();
        Mat bright = new Mat();

        Core.inRange(
                hsv,
                new Scalar(0, 0, 150),
                new Scalar(179, 90, 255),
                white
        );

        Core.inRange(
                hsv,
                new Scalar(0, 70, 185),
                new Scalar(179, 255, 255),
                bright
        );

        Mat candidate = new Mat();
        Core.bitwise_or(white, bright, candidate);

        candidate = morphologyOpen(candidate, 1, 2);

        Mat labels = new Mat();
        Mat stats = new Mat();
        Mat centroids = new Mat();

        int count = Imgproc.connectedComponentsWithStats(candidate, labels, stats, centroids, 8);

        Mat textMask = Mat.zeros(target.rows(), target.cols(), CvType.CV_8UC1);

        int imgH = target.rows();
        int imgW = target.cols();
        int imgArea = imgH * imgW;

        for (int i = 1; i < count; i++) {
            int x = (int) stats.get(i, Imgproc.CC_STAT_LEFT)[0];
            int y = (int) stats.get(i, Imgproc.CC_STAT_TOP)[0];
            int w = (int) stats.get(i, Imgproc.CC_STAT_WIDTH)[0];
            int h = (int) stats.get(i, Imgproc.CC_STAT_HEIGHT)[0];
            int area = (int) stats.get(i, Imgproc.CC_STAT_AREA)[0];

            if (area < 8) {
                continue;
            }

            if (area > imgArea * 0.12) {
                continue;
            }

            if (w < 3 || h < 3) {
                continue;
            }

            boolean isTopOrBottom = y < imgH * 0.35 || y + h > imgH * 0.65;
            boolean isTextSized = area < imgArea * 0.035;

            if (isTopOrBottom || isTextSized) {
                Mat componentMask = new Mat();
                Core.compare(labels, new Scalar(i), componentMask, Core.CMP_EQ);
                Core.bitwise_or(textMask, componentMask, textMask);
                componentMask.release();
            }
        }

        textMask = dilate(textMask, 1, 5);

        if (config.textDilate() > 0) {
            textMask = dilate(textMask, config.textDilate(), 7);
        }

        hsv.release();
        white.release();
        bright.release();
        candidate.release();
        labels.release();
        stats.release();
        centroids.release();

        return textMask;
    }

    private Mat buildPriorityMask(
            Mat original,
            int size,
            String fit,
            String priority,
            String priorityRegions,
            String priorityPolygons,
            MultipartFile priorityMaskFile,
            int priorityDilate
    ) {
        if (isBlank(priority) || "none".equals(priority)) {
            return Mat.zeros(size, size, CvType.CV_8UC1);
        }

        if ("all".equals(priority)) {
            return buildFullMask(size, size, 0.0);
        }

        if ("mask".equals(priority)) {
            if (priorityMaskFile == null || priorityMaskFile.isEmpty()) {
                return Mat.zeros(size, size, CvType.CV_8UC1);
            }

            try {
                BufferedImage maskImage = ImageIO.read(priorityMaskFile.getInputStream());

                if (maskImage == null) {
                    throw new IllegalArgumentException("Failed to decode priority mask");
                }

                Mat maskBgr = bufferedImageToBgrMat(maskImage);

                Mat gray = new Mat();
                Imgproc.cvtColor(maskBgr, gray, Imgproc.COLOR_BGR2GRAY);

                Mat resized = new Mat();
                Imgproc.resize(gray, resized, new Size(size, size), 0, 0, Imgproc.INTER_NEAREST);

                Imgproc.threshold(resized, resized, 10, 255, Imgproc.THRESH_BINARY);

                if (priorityDilate > 0) {
                    resized = dilate(resized, priorityDilate, 5);
                }

                maskBgr.release();
                gray.release();

                return resized;
            } catch (Exception e) {
                throw new RuntimeException("Failed to read priority mask: " + e.getMessage(), e);
            }
        }

        Mat mask = Mat.zeros(size, size, CvType.CV_8UC1);
        Transform transform = getTransform(original, fit, size);

        if ("regions".equals(priority)) {
            List<Region> regions = parseRegions(priorityRegions);

            for (Region region : regions) {
                int x1 = clamp((int) Math.round(region.x() * transform.scale() + transform.offsetX()), 0, size - 1);
                int y1 = clamp((int) Math.round(region.y() * transform.scale() + transform.offsetY()), 0, size - 1);
                int x2 = clamp((int) Math.round((region.x() + region.w()) * transform.scale() + transform.offsetX()), 0, size - 1);
                int y2 = clamp((int) Math.round((region.y() + region.h()) * transform.scale() + transform.offsetY()), 0, size - 1);

                Imgproc.rectangle(
                        mask,
                        new org.opencv.core.Point(x1, y1),
                        new org.opencv.core.Point(x2, y2),
                        new Scalar(255),
                        -1
                );
            }
        }

        if ("polygons".equals(priority)) {
            List<List<PriorityPoint>> polygons = parsePolygons(priorityPolygons);

            for (List<PriorityPoint> polygon : polygons) {
                MatOfFloat points = new MatOfFloat();
                List<org.opencv.core.Point> cvPoints = new ArrayList<>();

                for (PriorityPoint point : polygon) {
                    int x = clamp((int) Math.round(point.x() * transform.scale() + transform.offsetX()), 0, size - 1);
                    int y = clamp((int) Math.round(point.y() * transform.scale() + transform.offsetY()), 0, size - 1);

                    cvPoints.add(new org.opencv.core.Point(x, y));
                }

                org.opencv.core.MatOfPoint matOfPoint = new org.opencv.core.MatOfPoint();
                matOfPoint.fromList(cvPoints);

                List<org.opencv.core.MatOfPoint> fill = List.of(matOfPoint);
                Imgproc.fillPoly(mask, fill, new Scalar(255));

                points.release();
                matOfPoint.release();
            }
        }

        if (priorityDilate > 0) {
            mask = dilate(mask, priorityDilate, 5);
        }

        return mask;
    }

    private Transform getTransform(Mat original, String fit, int size) {
        int originalWidth = original.cols();
        int originalHeight = original.rows();

        if ("contain".equals(fit)) {
            double scale = (double) size / Math.max(originalWidth, originalHeight);

            int newWidth = (int) Math.round(originalWidth * scale);
            int newHeight = (int) Math.round(originalHeight * scale);

            int offsetX = (size - newWidth) / 2;
            int offsetY = (size - newHeight) / 2;

            return new Transform(scale, offsetX, offsetY);
        }

        double scale = (double) size / Math.min(originalWidth, originalHeight);

        int newWidth = (int) Math.round(originalWidth * scale);
        int newHeight = (int) Math.round(originalHeight * scale);

        int cropX = Math.max(0, (newWidth - size) / 2);
        int cropY = Math.max(0, (newHeight - size) / 2);

        return new Transform(scale, -cropX, -cropY);
    }

    private Mat makeWeightMap(Mat inputMask, double strength) {
        Mat mask = new Mat();
        Imgproc.threshold(inputMask, mask, 0, 255, Imgproc.THRESH_BINARY);

        mask = morphologyClose(mask, 2, 5);

        int size = mask.rows();

        Mat blur1 = new Mat();
        Mat blur2 = new Mat();

        Imgproc.GaussianBlur(
                mask,
                blur1,
                new Size(0, 0),
                size * 0.025,
                size * 0.025
        );

        Imgproc.GaussianBlur(
                mask,
                blur2,
                new Size(0, 0),
                size * 0.055,
                size * 0.055
        );

        Mat dist = new Mat();
        Imgproc.distanceTransform(mask, dist, Imgproc.DIST_L2, 5);

        Core.MinMaxLocResult minMax = Core.minMaxLoc(dist);

        Mat core = Mat.zeros(mask.rows(), mask.cols(), CvType.CV_32FC1);

        if (minMax.maxVal > 0) {
            Core.divide(dist, new Scalar(minMax.maxVal), core);
            Core.pow(core, 0.45, core);
            Core.multiply(core, new Scalar(255.0), core);
        }

        Mat blur1Float = new Mat();
        Mat blur2Float = new Mat();

        blur1.convertTo(blur1Float, CvType.CV_32FC1);
        blur2.convertTo(blur2Float, CvType.CV_32FC1);

        Core.multiply(blur1Float, new Scalar(0.85), blur1Float);
        Core.multiply(blur2Float, new Scalar(0.55), blur2Float);

        Mat soft = new Mat();
        Core.max(blur1Float, blur2Float, soft);

        Mat weightsFloat = new Mat();
        Core.max(soft, core, weightsFloat);

        Core.multiply(weightsFloat, new Scalar(strength), weightsFloat);

        Mat weights = new Mat();
        weightsFloat.convertTo(weights, CvType.CV_8UC1);

        mask.release();
        blur1.release();
        blur2.release();
        dist.release();
        core.release();
        blur1Float.release();
        blur2Float.release();
        soft.release();
        weightsFloat.release();

        return weights;
    }

    private Mat morphologyClose(Mat source, int iterations, int kernelSize) {
        Mat result = source.clone();

        Mat kernel = Imgproc.getStructuringElement(
                Imgproc.MORPH_RECT,
                new Size(kernelSize, kernelSize)
        );

        Imgproc.morphologyEx(result, result, Imgproc.MORPH_CLOSE, kernel, new org.opencv.core.Point(-1, -1), iterations);

        kernel.release();

        return result;
    }

    private Mat morphologyOpen(Mat source, int iterations, int kernelSize) {
        Mat result = source.clone();

        Mat kernel = Imgproc.getStructuringElement(
                Imgproc.MORPH_RECT,
                new Size(kernelSize, kernelSize)
        );

        Imgproc.morphologyEx(result, result, Imgproc.MORPH_OPEN, kernel, new org.opencv.core.Point(-1, -1), iterations);

        kernel.release();

        return result;
    }

    private Mat dilate(Mat source, int iterations, int kernelSize) {
        Mat result = source.clone();

        Mat kernel = Imgproc.getStructuringElement(
                Imgproc.MORPH_RECT,
                new Size(kernelSize, kernelSize)
        );

        Imgproc.dilate(result, result, kernel, new org.opencv.core.Point(-1, -1), iterations);

        kernel.release();

        return result;
    }

    private Mat erode(Mat source, int iterations, int kernelSize) {
        Mat result = source.clone();

        Mat kernel = Imgproc.getStructuringElement(
                Imgproc.MORPH_RECT,
                new Size(kernelSize, kernelSize)
        );

        Imgproc.erode(result, result, kernel, new org.opencv.core.Point(-1, -1), iterations);

        kernel.release();

        return result;
    }

    private Mat bufferedImageToBgrMat(BufferedImage source) {
        BufferedImage image = new BufferedImage(
                source.getWidth(),
                source.getHeight(),
                BufferedImage.TYPE_3BYTE_BGR
        );

        Graphics2D g = image.createGraphics();
        g.drawImage(source, 0, 0, null);
        g.dispose();

        byte[] data = ((DataBufferByte) image.getRaster().getDataBuffer()).getData();

        Mat mat = new Mat(image.getHeight(), image.getWidth(), CvType.CV_8UC3);
        mat.put(0, 0, data);

        return mat;
    }

    private BufferedImage matToBufferedImage(Mat mat) {
        if (mat.channels() == 1) {
            BufferedImage image = new BufferedImage(mat.cols(), mat.rows(), BufferedImage.TYPE_BYTE_GRAY);
            byte[] data = new byte[(int) (mat.total() * mat.channels())];
            mat.get(0, 0, data);
            image.getRaster().setDataElements(0, 0, mat.cols(), mat.rows(), data);
            return image;
        }

        Mat rgb = new Mat();
        Imgproc.cvtColor(mat, rgb, Imgproc.COLOR_BGR2RGB);

        BufferedImage image = new BufferedImage(mat.cols(), mat.rows(), BufferedImage.TYPE_3BYTE_BGR);

        byte[] data = new byte[(int) (rgb.total() * rgb.channels())];
        rgb.get(0, 0, data);

        image.getRaster().setDataElements(0, 0, rgb.cols(), rgb.rows(), data);

        rgb.release();

        return image;
    }

    private List<Region> parseRegions(String value) {
        List<Region> regions = new ArrayList<>();

        if (isBlank(value)) {
            return regions;
        }

        for (String part : value.split(";")) {
            if (isBlank(part)) {
                continue;
            }

            String[] split = part.split(",");

            if (split.length != 4) {
                throw new IllegalArgumentException("Invalid priority region: " + part);
            }

            int x = Integer.parseInt(split[0].trim());
            int y = Integer.parseInt(split[1].trim());
            int w = Integer.parseInt(split[2].trim());
            int h = Integer.parseInt(split[3].trim());

            if (w <= 0 || h <= 0) {
                throw new IllegalArgumentException("Priority region width and height must be positive");
            }

            regions.add(new Region(x, y, w, h));
        }

        return regions;
    }

    private List<List<PriorityPoint>> parsePolygons(String value) {
        List<List<PriorityPoint>> polygons = new ArrayList<>();

        if (isBlank(value)) {
            return polygons;
        }

        for (String polygonPart : value.split("\\|")) {
            if (isBlank(polygonPart)) {
                continue;
            }

            List<PriorityPoint> points = new ArrayList<>();

            for (String pointPart : polygonPart.split(";")) {
                if (isBlank(pointPart)) {
                    continue;
                }

                String[] split = pointPart.split(",");

                if (split.length != 2) {
                    throw new IllegalArgumentException("Invalid priority polygon point: " + pointPart);
                }

                int x = Integer.parseInt(split[0].trim());
                int y = Integer.parseInt(split[1].trim());

                points.add(new PriorityPoint(x, y));
            }

            if (points.size() < 3) {
                throw new IllegalArgumentException("Priority polygon must have at least 3 points");
            }

            polygons.add(points);
        }

        return polygons;
    }

    private int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private record PresetBuildResult(Mat target, Mat weights) {
        private void release() {
            if (target != null) {
                target.release();
            }

            if (weights != null) {
                weights.release();
            }
        }
    }

    private record Transform(double scale, int offsetX, int offsetY) {
    }

    private record Region(int x, int y, int w, int h) {
    }

    private record PriorityPoint(int x, int y) {
    }

    private record ModeConfig(
            String fit,
            String foreground,
            String text,
            double weightStrength,
            double grabCutMargin,
            int grabCutIterations,
            int maskDilate,
            int maskErode,
            int textDilate,
            int priorityDilate
    ) {
        private static ModeConfig from(String mode) {
            if ("full".equals(mode)) {
                return new ModeConfig(
                        "contain",
                        "full",
                        "off",
                        0.90,
                        0.02,
                        3,
                        0,
                        0,
                        0,
                        0
                );
            }

            if ("object".equals(mode)) {
                return new ModeConfig(
                        "cover",
                        "grabcut",
                        "off",
                        1.00,
                        0.06,
                        5,
                        1,
                        0,
                        0,
                        1
                );
            }

            return new ModeConfig(
                    "contain",
                    "grabcut",
                    "auto",
                    1.05,
                    0.05,
                    5,
                    1,
                    0,
                    2,
                    1
            );
        }
    }
}