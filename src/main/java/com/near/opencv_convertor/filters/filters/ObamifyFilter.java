package com.near.opencv_convertor.filters.filters;

import com.near.opencv_convertor.dto.ResponseImage;
import com.near.opencv_convertor.filters.FilterParams;
import com.near.opencv_convertor.filters.ImageFilter;
import com.near.opencv_convertor.filters.params.ObamifyParams;
import com.near.opencv_convertor.filters.services.ObamifyTempPresetService;
import org.opencv.core.Mat;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.MediaType;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageTypeSpecifier;
import javax.imageio.ImageWriter;
import javax.imageio.metadata.IIOMetadata;
import javax.imageio.metadata.IIOMetadataNode;
import javax.imageio.stream.ImageOutputStream;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.regex.Pattern;

public class ObamifyFilter implements ImageFilter {

    private static final int DEFAULT_RESOLUTION = 128;
    private static final int DEFAULT_PROXIMITY_IMPORTANCE = 13;

    private static final int SWAPS_PER_GENERATION_PER_PIXEL = 128;

    private static final int GIF_RESOLUTION = 512;
    private static final int GIF_FRAMES = 160;
    private static final int GIF_DELAY_MS = 25;

    private static final String DEFAULT_PRESET = "obama";
    private static final String TEMP_PRESET_PREFIX = "tmp:";

    private static final Pattern PRESET_NAME_PATTERN = Pattern.compile("^[a-zA-Z0-9_-]+$");

    private final ObamifyTempPresetService obamifyTempPresetService;

    public ObamifyFilter() {
        this(null);
    }

    public ObamifyFilter(ObamifyTempPresetService obamifyTempPresetService) {
        this.obamifyTempPresetService = obamifyTempPresetService;
    }

    @Override
    public Mat apply(Mat image, FilterParams params) {
        throw new UnsupportedOperationException(
                "OBAMIFY filter works with source MultipartFile and returns animated GIF"
        );
    }

    public ResponseImage applyToFile(MultipartFile file, FilterParams params) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Image file is empty");
        }

        ObamifyParams obamifyParams = (ObamifyParams) params;

        Integer resolutionRaw = obamifyParams.getResolution();
        Integer proximityImportanceRaw = obamifyParams.getProximityImportance();

        String preset = resolvePreset(obamifyParams.getPreset());

        int resolution = resolutionRaw == null ? DEFAULT_RESOLUTION : resolutionRaw;
        int proximityImportance = proximityImportanceRaw == null
                ? DEFAULT_PROXIMITY_IMPORTANCE
                : proximityImportanceRaw;

        resolution = Math.max(32, Math.min(192, resolution));
        proximityImportance = Math.max(0, Math.min(50, proximityImportance));

        try {
            BufferedImage sourceRaw = ImageIO.read(file.getInputStream());

            if (sourceRaw == null) {
                throw new IllegalArgumentException("Failed to decode source image");
            }

            BufferedImage targetRaw = readPresetImage(preset, "target256.png");
            BufferedImage weightsRaw = readPresetImage(preset, "weights256.png");

            BufferedImage source = cropResizeToSquare(sourceRaw, resolution);
            BufferedImage target = cropResizeToSquare(targetRaw, resolution);
            BufferedImage weightsImage = cropResizeToSquare(weightsRaw, resolution);

            int[] sourcePixels = readRgbPixels(source);
            int[] targetPixels = readRgbPixels(target);
            int[] weights = readWeights(weightsImage);

            int[] assignments = calculateGeneticAssignments(
                    sourcePixels,
                    targetPixels,
                    weights,
                    resolution,
                    proximityImportance
            );

            List<BufferedImage> frames = renderObamifyAnimation(
                    sourcePixels,
                    assignments,
                    resolution,
                    GIF_RESOLUTION,
                    GIF_FRAMES
            );

            BufferedImage lastFrame = frames.get(frames.size() - 1);

            for (int i = 0; i < 20; i++) {
                frames.add(copyImage(lastFrame));
            }

            byte[] gifBytes = encodeGif(frames, GIF_DELAY_MS);

            String outputFilename = buildOutputFilename(file.getOriginalFilename());

            return new ResponseImage(
                    new InputStreamResource(new ByteArrayInputStream(gifBytes)),
                    gifBytes.length,
                    outputFilename,
                    MediaType.IMAGE_GIF
            );
        } catch (Exception e) {
            throw new RuntimeException("Failed to obamify image: " + e.getMessage(), e);
        }
    }

    private BufferedImage readPresetImage(String preset, String filename) throws IOException {
        if (isTempPreset(preset)) {
            if (obamifyTempPresetService == null) {
                throw new IllegalStateException(
                        "Temporary obamify preset requested, but ObamifyTempPresetService is not configured"
                );
            }

            String presetId = preset.substring(TEMP_PRESET_PREFIX.length());

            return obamifyTempPresetService.readTempPresetImage(presetId, filename);
        }

        return readClasspathImage("/obamify/" + preset + "/" + filename);
    }

    private String resolvePreset(String presetRaw) {
        String preset = presetRaw == null || presetRaw.isBlank()
                ? DEFAULT_PRESET
                : presetRaw.trim();

        if (isTempPreset(preset)) {
            String presetId = preset.substring(TEMP_PRESET_PREFIX.length());

            if (!PRESET_NAME_PATTERN.matcher(presetId).matches()) {
                throw new IllegalArgumentException(
                        "Invalid temporary obamify preset id: " + presetId
                );
            }

            return preset;
        }

        if (!PRESET_NAME_PATTERN.matcher(preset).matches()) {
            throw new IllegalArgumentException(
                    "Invalid obamify preset name: " + preset
            );
        }

        return preset;
    }

    private boolean isTempPreset(String preset) {
        return preset != null && preset.startsWith(TEMP_PRESET_PREFIX);
    }

    private int[] calculateGeneticAssignments(
            int[] sourcePixels,
            int[] targetPixels,
            int[] weights,
            int sidelen,
            int proximityImportance
    ) {
        Pixel[] pixels = new Pixel[sourcePixels.length];

        for (int i = 0; i < sourcePixels.length; i++) {
            int x = i % sidelen;
            int y = i / sidelen;

            Pixel pixel = new Pixel(x, y, sourcePixels[i], 0);

            int h = heuristic(
                    x,
                    y,
                    x,
                    y,
                    pixel.rgb,
                    targetPixels[i],
                    weights[i],
                    proximityImportance
            );

            pixel.h = h;
            pixels[i] = pixel;
        }

        Random random = new Random(12345);
        int swapsPerGeneration = SWAPS_PER_GENERATION_PER_PIXEL * pixels.length;

        int maxDist = sidelen;
        int generation = 0;
        int maxGenerations = 2500;

        while (generation < maxGenerations) {
            int swapsMade = 0;

            for (int i = 0; i < swapsPerGeneration; i++) {
                int apos = random.nextInt(pixels.length);

                int ax = apos % sidelen;
                int ay = apos / sidelen;

                int bx = clamp(
                        ax + random.nextInt(maxDist * 2 + 1) - maxDist,
                        0,
                        sidelen - 1
                );

                int by = clamp(
                        ay + random.nextInt(maxDist * 2 + 1) - maxDist,
                        0,
                        sidelen - 1
                );

                int bpos = by * sidelen + bx;

                Pixel a = pixels[apos];
                Pixel b = pixels[bpos];

                int targetA = targetPixels[apos];
                int targetB = targetPixels[bpos];

                int aOnBH = heuristic(
                        a.srcX,
                        a.srcY,
                        bx,
                        by,
                        a.rgb,
                        targetB,
                        weights[bpos],
                        proximityImportance
                );

                int bOnAH = heuristic(
                        b.srcX,
                        b.srcY,
                        ax,
                        ay,
                        b.rgb,
                        targetA,
                        weights[apos],
                        proximityImportance
                );

                int improvementA = a.h - bOnAH;
                int improvementB = b.h - aOnBH;

                if (improvementA + improvementB > 0) {
                    pixels[apos] = b;
                    pixels[bpos] = a;

                    pixels[apos].h = bOnAH;
                    pixels[bpos].h = aOnBH;

                    swapsMade++;
                }
            }

            if (maxDist < 4 && swapsMade < 10) {
                break;
            }

            maxDist = Math.max(2, (int) (maxDist * 0.99f));
            generation++;
        }

        int[] assignments = new int[pixels.length];

        for (int targetIndex = 0; targetIndex < pixels.length; targetIndex++) {
            Pixel pixel = pixels[targetIndex];
            assignments[targetIndex] = pixel.srcY * sidelen + pixel.srcX;
        }

        return assignments;
    }

    private int heuristic(
            int sourceX,
            int sourceY,
            int targetX,
            int targetY,
            int sourceRgb,
            int targetRgb,
            int colorWeight,
            int spatialWeight
    ) {
        int sr = red(sourceRgb);
        int sg = green(sourceRgb);
        int sb = blue(sourceRgb);

        int tr = red(targetRgb);
        int tg = green(targetRgb);
        int tb = blue(targetRgb);

        int dx = sourceX - targetX;
        int dy = sourceY - targetY;

        int spatial = dx * dx + dy * dy;

        int dr = sr - tr;
        int dg = sg - tg;
        int db = sb - tb;

        int color = dr * dr + dg * dg + db * db;

        long result = (long) color * colorWeight
                + squareLong((long) spatial * spatialWeight);

        return result > Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) result;
    }

    private List<BufferedImage> renderObamifyAnimation(
            int[] sourcePixels,
            int[] assignments,
            int sidelen,
            int gifResolution,
            int framesCount
    ) {
        float scale = (float) gifResolution / sidelen;

        int[] sourceToTarget = new int[assignments.length];

        for (int targetIndex = 0; targetIndex < assignments.length; targetIndex++) {
            int sourceIndex = assignments[targetIndex];
            sourceToTarget[sourceIndex] = targetIndex;
        }

        List<BufferedImage> frames = new ArrayList<>();

        for (int frameIndex = 0; frameIndex < framesCount; frameIndex++) {
            float rawT = framesCount <= 1
                    ? 1.0f
                    : (float) frameIndex / (float) (framesCount - 1);

            float t = easeInOutCubic(rawT);

            BufferedImage frame = new BufferedImage(
                    gifResolution,
                    gifResolution,
                    BufferedImage.TYPE_INT_RGB
            );

            Graphics2D g = frame.createGraphics();

            g.setColor(Color.WHITE);
            g.fillRect(0, 0, gifResolution, gifResolution);

            g.setRenderingHint(
                    RenderingHints.KEY_ANTIALIASING,
                    RenderingHints.VALUE_ANTIALIAS_OFF
            );

            int cellSize = Math.max(1, (int) Math.ceil(scale));

            for (int sourceIndex = 0; sourceIndex < sourcePixels.length; sourceIndex++) {
                int targetIndex = sourceToTarget[sourceIndex];

                int sx = sourceIndex % sidelen;
                int sy = sourceIndex / sidelen;

                int tx = targetIndex % sidelen;
                int ty = targetIndex / sidelen;

                float x = lerp(sx, tx, t) * scale;
                float y = lerp(sy, ty, t) * scale;

                g.setColor(new Color(sourcePixels[sourceIndex]));

                g.fillRect(
                        Math.round(x),
                        Math.round(y),
                        cellSize,
                        cellSize
                );
            }

            g.dispose();
            frames.add(frame);
        }

        return frames;
    }

    private byte[] encodeGif(List<BufferedImage> frames, int delayMs) throws IOException {
        if (frames == null || frames.isEmpty()) {
            throw new IllegalArgumentException("No frames to encode");
        }

        ByteArrayOutputStream output = new ByteArrayOutputStream();

        ImageWriter writer = ImageIO.getImageWritersByFormatName("gif").next();

        try (ImageOutputStream ios = ImageIO.createImageOutputStream(output)) {
            writer.setOutput(ios);
            writer.prepareWriteSequence(null);

            for (int i = 0; i < frames.size(); i++) {
                BufferedImage frame = frames.get(i);

                IIOMetadata metadata = writer.getDefaultImageMetadata(
                        ImageTypeSpecifier.createFromBufferedImageType(BufferedImage.TYPE_INT_RGB),
                        null
                );

                configureGifMetadata(metadata, delayMs, i == 0);

                writer.writeToSequence(new IIOImage(frame, null, metadata), null);
            }

            writer.endWriteSequence();
        } finally {
            writer.dispose();
        }

        return output.toByteArray();
    }

    private void configureGifMetadata(
            IIOMetadata metadata,
            int delayMs,
            boolean firstFrame
    ) throws IOException {
        String metaFormat = metadata.getNativeMetadataFormatName();

        IIOMetadataNode root = (IIOMetadataNode) metadata.getAsTree(metaFormat);

        IIOMetadataNode graphicControlExtension = getOrCreateChild(
                root,
                "GraphicControlExtension"
        );

        graphicControlExtension.setAttribute("disposalMethod", "none");
        graphicControlExtension.setAttribute("userInputFlag", "FALSE");
        graphicControlExtension.setAttribute("transparentColorFlag", "FALSE");
        graphicControlExtension.setAttribute("delayTime", Integer.toString(delayMs / 10));
        graphicControlExtension.setAttribute("transparentColorIndex", "0");

        if (firstFrame) {
            IIOMetadataNode appExtensions = getOrCreateChild(
                    root,
                    "ApplicationExtensions"
            );

            IIOMetadataNode appExtension = new IIOMetadataNode("ApplicationExtension");
            appExtension.setAttribute("applicationID", "NETSCAPE");
            appExtension.setAttribute("authenticationCode", "2.0");
            appExtension.setUserObject(new byte[]{0x1, 0x0, 0x0});

            appExtensions.appendChild(appExtension);
        }

        metadata.setFromTree(metaFormat, root);
    }

    private IIOMetadataNode getOrCreateChild(IIOMetadataNode root, String name) {
        for (int i = 0; i < root.getLength(); i++) {
            if (name.equals(root.item(i).getNodeName())) {
                return (IIOMetadataNode) root.item(i);
            }
        }

        IIOMetadataNode node = new IIOMetadataNode(name);
        root.appendChild(node);

        return node;
    }

    private BufferedImage readClasspathImage(String path) throws IOException {
        try (InputStream inputStream = getClass().getResourceAsStream(path)) {
            if (inputStream == null) {
                throw new FileNotFoundException("Classpath image not found: " + path);
            }

            BufferedImage image = ImageIO.read(inputStream);

            if (image == null) {
                throw new IllegalArgumentException("Failed to read image: " + path);
            }

            return image;
        }
    }

    private BufferedImage cropResizeToSquare(BufferedImage source, int sidelen) {
        int width = source.getWidth();
        int height = source.getHeight();

        int side = Math.min(width, height);

        int x = (width - side) / 2;
        int y = (height - side) / 2;

        BufferedImage cropped = source.getSubimage(x, y, side, side);

        BufferedImage resized = new BufferedImage(
                sidelen,
                sidelen,
                BufferedImage.TYPE_INT_RGB
        );

        Graphics2D g = resized.createGraphics();

        g.setRenderingHint(
                RenderingHints.KEY_INTERPOLATION,
                RenderingHints.VALUE_INTERPOLATION_BICUBIC
        );

        g.drawImage(cropped, 0, 0, sidelen, sidelen, null);
        g.dispose();

        return resized;
    }

    private int[] readRgbPixels(BufferedImage image) {
        int[] pixels = new int[image.getWidth() * image.getHeight()];

        int index = 0;

        for (int y = 0; y < image.getHeight(); y++) {
            for (int x = 0; x < image.getWidth(); x++) {
                pixels[index++] = image.getRGB(x, y) & 0x00FFFFFF;
            }
        }

        return pixels;
    }

    private int[] readWeights(BufferedImage image) {
        int[] weights = new int[image.getWidth() * image.getHeight()];

        int index = 0;

        for (int y = 0; y < image.getHeight(); y++) {
            for (int x = 0; x < image.getWidth(); x++) {
                int rgb = image.getRGB(x, y);
                weights[index++] = red(rgb);
            }
        }

        return weights;
    }

    private BufferedImage copyImage(BufferedImage source) {
        BufferedImage copy = new BufferedImage(
                source.getWidth(),
                source.getHeight(),
                source.getType()
        );

        Graphics2D g = copy.createGraphics();
        g.drawImage(source, 0, 0, null);
        g.dispose();

        return copy;
    }

    private String buildOutputFilename(String originalFilename) {
        String baseName = originalFilename == null ? "image" : originalFilename;

        int dotIndex = baseName.lastIndexOf('.');
        if (dotIndex > 0) {
            baseName = baseName.substring(0, dotIndex);
        }

        return baseName + "_obamified.gif";
    }

    private float lerp(float a, float b, float t) {
        return a + (b - a) * t;
    }

    private float easeInOutCubic(float t) {
        if (t < 0.5f) {
            return 4.0f * t * t * t;
        }

        float f = -2.0f * t + 2.0f;
        return 1.0f - (f * f * f) / 2.0f;
    }

    private int red(int rgb) {
        return (rgb >> 16) & 0xFF;
    }

    private int green(int rgb) {
        return (rgb >> 8) & 0xFF;
    }

    private int blue(int rgb) {
        return rgb & 0xFF;
    }

    private long squareLong(long value) {
        return value * value;
    }

    private int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    private static class Pixel {
        private final int srcX;
        private final int srcY;
        private final int rgb;
        private int h;

        private Pixel(int srcX, int srcY, int rgb, int h) {
            this.srcX = srcX;
            this.srcY = srcY;
            this.rgb = rgb;
            this.h = h;
        }
    }
}