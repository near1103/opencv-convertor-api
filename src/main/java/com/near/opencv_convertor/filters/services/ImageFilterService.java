package com.near.opencv_convertor.filters.services;

import com.near.opencv_convertor.converters.enums.SupportedImageFormat;
import com.near.opencv_convertor.converters.services.ImageFormatConverter;
import com.near.opencv_convertor.dto.GifFrame;
import com.near.opencv_convertor.dto.GifMeta;
import com.near.opencv_convertor.dto.GifStreamMeta;
import com.near.opencv_convertor.dto.ResponseImage;
import com.near.opencv_convertor.filters.*;
import lombok.RequiredArgsConstructor;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.imageio.*;
import javax.imageio.metadata.IIOMetadata;
import javax.imageio.metadata.IIOMetadataNode;
import javax.imageio.stream.ImageInputStream;
import javax.imageio.stream.ImageOutputStream;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;
import java.awt.image.WritableRaster;
import java.io.File;
import java.io.FileInputStream;
import java.nio.file.Files;
import java.util.*;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ImageFilterService {

    private final ImageFormatConverter imageFormatConverter;

    public ResponseImage applyFilter(
            MultipartFile file,
            String filterType,
            Map<String, String> params
    ) {

        FilterType type = parseFilterType(filterType);
        SupportedImageFormat imageFormat = detectFormat(file);

        if (imageFormat == SupportedImageFormat.GIF) {
            return processGif(file, type, params);
        }
        File input = null;
        File readable = null;
        File output = null;

        try {
            input = File.createTempFile("input-", "." + imageFormat.getExtension());
            file.transferTo(input);

            readable = imageFormatConverter.convertToReadableForOpenCV(input);
            Mat source = Imgcodecs.imread(readable.getAbsolutePath());

            ImageFilter filter = FilterFactory.create(type);
            FilterParams filterParams = FilterParamsFactory.create(type, params);

            Mat result = filter.apply(source, filterParams);

            if (filter.requiresAlpha()) {
                imageFormat = SupportedImageFormat.PNG;
            }

            output = File.createTempFile("output-", "." + imageFormat.getExtension());
            Imgcodecs.imwrite(output.getAbsolutePath(), result);

            return new ResponseImage(
                    new InputStreamResource(new FileInputStream(output)),
                    output.length(),
                    "filtered." + imageFormat.getExtension(),
                    imageFormat.getMediaType()
            );

        } catch (Exception e) {
            throw new RuntimeException("Failed to apply filter", e);
        } finally {
            safeDelete(input);
            safeDelete(readable);
        }
    }

    private FilterType parseFilterType(String type) {
        try {
            return FilterType.valueOf(type.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Unknown filter: " + type);
        }
    }

    private SupportedImageFormat detectFormat(MultipartFile file) {
        String ext = ImageFormatConverter.getExtension(
                Objects.requireNonNull(file.getOriginalFilename())
        );

        return SupportedImageFormat.fromExtension(ext)
                .orElse(SupportedImageFormat.PNG);
    }

    private void safeDelete(File file) {
        if (file != null) {
            try {
                Files.deleteIfExists(file.toPath());
            } catch (Exception ignored) {
            }
        }
    }

    private ResponseImage processGif(
            MultipartFile file,
            FilterType type,
            Map<String, String> params
    ) {
        File output = null;

        try {
            ImageFilter filter = FilterFactory.create(type);
            FilterParams filterParams = FilterParamsFactory.create(type, params);

            List<GifFrame> processedFrames = new ArrayList<>();

            try (ImageInputStream stream = ImageIO.createImageInputStream(file.getInputStream())) {
                Iterator<ImageReader> it = ImageIO.getImageReadersByFormatName("gif");
                if (!it.hasNext()) {
                    throw new IllegalArgumentException("No GIF reader available");
                }
                ImageReader reader = it.next();
                reader.setInput(stream, false, false);

                int width = reader.getWidth(0);
                int height = reader.getHeight(0);
                int frameCount = reader.getNumImages(true);

                if (frameCount <= 0) {
                    throw new IllegalArgumentException("Empty GIF");
                }
                if (frameCount > 3000) {
                    throw new IllegalArgumentException("GIF too large");
                }

                GifStreamMeta streamMeta = readGifStreamMeta(reader.getStreamMetadata());

                BufferedImage master = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
                Graphics2D gMaster = master.createGraphics();
                gMaster.setComposite(AlphaComposite.SrcOver);

                if (streamMeta.backgroundColor != null) {
                    gMaster.setComposite(AlphaComposite.Src);
                    gMaster.setColor(streamMeta.backgroundColor);
                    gMaster.fillRect(0, 0, width, height);
                    gMaster.setComposite(AlphaComposite.SrcOver);
                }

                GifMeta prevMeta = null;
                BufferedImage prevMasterSnapshot = null;

                for (int i = 0; i < frameCount; i++) {

                    if (prevMeta != null) {
                        if ("restoreToBackgroundColor".equals(prevMeta.disposal)) {
                            Color bg = streamMeta.backgroundColor;
                            if (bg == null) bg = new Color(0, 0, 0, 0);

                            gMaster.setComposite(AlphaComposite.Src);
                            gMaster.setColor(bg);
                            gMaster.fillRect(prevMeta.x, prevMeta.y, prevMeta.w, prevMeta.h);
                            gMaster.setComposite(AlphaComposite.SrcOver);

                        } else if ("restoreToPrevious".equals(prevMeta.disposal)) {
                            if (prevMasterSnapshot != null) {
                                master = deepCopy(prevMasterSnapshot);
                                gMaster.dispose();
                                gMaster = master.createGraphics();
                                gMaster.setComposite(AlphaComposite.SrcOver);
                            }
                        }
                    }

                    prevMasterSnapshot = deepCopy(master);

                    BufferedImage rawFrame = reader.read(i);
                    GifMeta meta = readGifMeta(reader.getImageMetadata(i));

                    if (meta.w <= 0) meta.w = rawFrame.getWidth();
                    if (meta.h <= 0) meta.h = rawFrame.getHeight();

                    gMaster.setComposite(AlphaComposite.SrcOver);
                    gMaster.drawImage(rawFrame, meta.x, meta.y, null);

                    BufferedImage composed = deepCopy(master);

                    Mat src = bufferedImageToMatBGRA(composed);
                    Mat dst = filter.apply(src, filterParams);

                    BufferedImage processed = matToBufferedImageBGRA(dst);
                    processed = toIntArgb(processed);

                    src.release();
                    dst.release();

                    processedFrames.add(new GifFrame(processed, meta.delay));

                    prevMeta = meta;
                }

                gMaster.dispose();
                reader.dispose();
            }

            output = File.createTempFile("filtered-", ".gif");

            writeAnimatedGif(processedFrames, output);

            return new ResponseImage(
                    new InputStreamResource(new FileInputStream(output)),
                    output.length(),
                    "filtered.gif",
                    MediaType.IMAGE_GIF
            );

        } catch (Exception e) {
            throw new RuntimeException("Failed to process GIF", e);
        }
    }

    private GifMeta readGifMeta(IIOMetadata metadata) throws Exception {
        GifMeta result = new GifMeta();
        result.delay = 100;
        result.disposal = "none";
        result.x = 0; result.y = 0;
        result.w = 0; result.h = 0;

        String format = metadata.getNativeMetadataFormatName();
        Node root = metadata.getAsTree(format);

        NodeList children = root.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            Node node = children.item(i);

            if ("GraphicControlExtension".equals(node.getNodeName())) {
                IIOMetadataNode ext = (IIOMetadataNode) node;

                String delayTime = ext.getAttribute("delayTime");
                if (delayTime != null && !delayTime.isBlank()) {
                    int cs = Integer.parseInt(delayTime);
                    result.delay = Math.max(10, cs * 10);
                }

                String disp = ext.getAttribute("disposalMethod");
                if (disp != null && !disp.isBlank()) result.disposal = disp;

                String tFlag = ext.getAttribute("transparentColorFlag");
                result.transparent = "TRUE".equalsIgnoreCase(tFlag);

                String tIndex = ext.getAttribute("transparentColorIndex");
                if (tIndex != null && !tIndex.isBlank()) {
                    result.transparentIndex = Integer.parseInt(tIndex);
                }
            }

            if ("ImageDescriptor".equals(node.getNodeName())) {
                IIOMetadataNode desc = (IIOMetadataNode) node;

                result.x = parseIntSafe(desc.getAttribute("imageLeftPosition"), 0);
                result.y = parseIntSafe(desc.getAttribute("imageTopPosition"), 0);
                result.w = parseIntSafe(desc.getAttribute("imageWidth"), 0);
                result.h = parseIntSafe(desc.getAttribute("imageHeight"), 0);
            }
        }
        return result;
    }

    private GifStreamMeta readGifStreamMeta(IIOMetadata streamMetadata) {
        GifStreamMeta out = new GifStreamMeta();
        if (streamMetadata == null) return out;

        try {
            String format = streamMetadata.getNativeMetadataFormatName();
            Node root = streamMetadata.getAsTree(format);

            Integer bgIndex = null;
            Map<Integer, Color> globalTable = new HashMap<>();

            NodeList children = root.getChildNodes();
            for (int i = 0; i < children.getLength(); i++) {
                Node node = children.item(i);

                if ("LogicalScreenDescriptor".equals(node.getNodeName())) {
                    IIOMetadataNode lsd = (IIOMetadataNode) node;
                    String idx = lsd.getAttribute("backgroundColorIndex");
                    if (idx != null && !idx.isBlank()) bgIndex = Integer.parseInt(idx);
                }

                if ("GlobalColorTable".equals(node.getNodeName())) {
                    NodeList entries = node.getChildNodes();
                    for (int j = 0; j < entries.getLength(); j++) {
                        Node e = entries.item(j);
                        if (!"ColorTableEntry".equals(e.getNodeName())) continue;

                        IIOMetadataNode ent = (IIOMetadataNode) e;
                        int index = parseIntSafe(ent.getAttribute("index"), -1);
                        int r = parseIntSafe(ent.getAttribute("red"), 0);
                        int g = parseIntSafe(ent.getAttribute("green"), 0);
                        int b = parseIntSafe(ent.getAttribute("blue"), 0);

                        if (index >= 0) globalTable.put(index, new Color(r, g, b));
                    }
                }
            }

            if (bgIndex != null) {
                out.backgroundColorIndex = bgIndex;
                out.backgroundColor = globalTable.get(bgIndex);
            }

        } catch (Exception ignore) {
            // fallback: no bg
        }

        return out;
    }

    private void writeAnimatedGif(List<GifFrame> frames, File output) throws Exception {
        if (frames == null || frames.isEmpty()) {
            throw new IllegalArgumentException("No frames to write");
        }

        ImageWriter writer = ImageIO.getImageWritersByFormatName("gif").next();

        try (ImageOutputStream ios = ImageIO.createImageOutputStream(output)) {
            writer.setOutput(ios);

            ImageWriteParam param = writer.getDefaultWriteParam();

            writer.prepareWriteSequence(null);

            for (int i = 0; i < frames.size(); i++) {
                GifFrame frame = frames.get(i);

                BufferedImage img = toIntArgb(frame.image);

                ImageTypeSpecifier type = ImageTypeSpecifier.createFromBufferedImageType(BufferedImage.TYPE_INT_ARGB);
                IIOMetadata meta = writer.getDefaultImageMetadata(type, param);

                meta = setGifFrameMetadata(meta, frame.delay);

                if (i == 0) {
                    meta = addNetscapeLoopExtensionToImageMetadata(meta, 0); // 0 = infinite
                }

                writer.writeToSequence(new IIOImage(img, null, meta), param);
            }

            writer.endWriteSequence();
        } finally {
            writer.dispose();
        }
    }

    private IIOMetadata addNetscapeLoopExtensionToImageMetadata(IIOMetadata metadata, int loopCount) throws Exception {
        if (metadata == null) return null;

        final String GIF_IMAGE_FORMAT = "javax_imageio_gif_image_1.0";

        String nativeFormat = metadata.getNativeMetadataFormatName();
        if (!GIF_IMAGE_FORMAT.equals(nativeFormat)) {
            return metadata;
        }

        IIOMetadataNode root = (IIOMetadataNode) metadata.getAsTree(GIF_IMAGE_FORMAT);

        IIOMetadataNode appExtensions = getOrCreateChild(root, "ApplicationExtensions");

        NodeList kids = appExtensions.getChildNodes();
        for (int i = kids.getLength() - 1; i >= 0; i--) {
            Node n = kids.item(i);
            if (!(n instanceof IIOMetadataNode node)) continue;
            if (!"ApplicationExtension".equals(node.getNodeName())) continue;

            if ("NETSCAPE".equals(node.getAttribute("applicationID"))
                    && "2.0".equals(node.getAttribute("authenticationCode"))) {
                appExtensions.removeChild(node);
            }
        }

        int loops = Math.max(0, loopCount);
        byte[] data = new byte[] {
                0x01,
                (byte) (loops & 0xFF),
                (byte) ((loops >> 8) & 0xFF)
        };

        IIOMetadataNode app = new IIOMetadataNode("ApplicationExtension");
        app.setAttribute("applicationID", "NETSCAPE");
        app.setAttribute("authenticationCode", "2.0");
        app.setUserObject(data);

        appExtensions.appendChild(app);

        metadata.setFromTree(GIF_IMAGE_FORMAT, root);
        return metadata;
    }

    private IIOMetadata setGifFrameMetadata(IIOMetadata metadata, int delayMs) throws Exception {
        final String GIF_IMAGE_FORMAT = "javax_imageio_gif_image_1.0";

        IIOMetadataNode root = (IIOMetadataNode) metadata.getAsTree(GIF_IMAGE_FORMAT);
        NodeList children = root.getChildNodes();

        for (int i = 0; i < children.getLength(); i++) {
            Node n = children.item(i);
            if (!"GraphicControlExtension".equals(n.getNodeName())) continue;

            IIOMetadataNode gce = (IIOMetadataNode) n;

            int delayCs = Math.max(1, delayMs / 10);
            gce.setAttribute("delayTime", String.valueOf(delayCs));
            gce.setAttribute("disposalMethod", "none");
            gce.setAttribute("userInputFlag", "FALSE");
        }

        metadata.setFromTree(GIF_IMAGE_FORMAT, root);
        return metadata;
    }

    private IIOMetadataNode getOrCreateChild(IIOMetadataNode parent, String name) {
        NodeList children = parent.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            Node n = children.item(i);
            if (n instanceof IIOMetadataNode node && name.equals(node.getNodeName())) {
                return node;
            }
        }
        IIOMetadataNode child = new IIOMetadataNode(name);
        parent.appendChild(child);
        return child;
    }


    private Mat bufferedImageToMatBGRA(BufferedImage bi) {
        BufferedImage argb = toIntArgb(bi);

        int w = argb.getWidth();
        int h = argb.getHeight();

        int[] pixels = new int[w * h];
        argb.getRGB(0, 0, w, h, pixels, 0, w);

        byte[] data = new byte[w * h * 4];
        for (int i = 0; i < pixels.length; i++) {
            int p = pixels[i];              // ARGB
            byte a = (byte) ((p >> 24) & 0xFF);
            byte r = (byte) ((p >> 16) & 0xFF);
            byte g = (byte) ((p >> 8) & 0xFF);
            byte b = (byte) (p & 0xFF);

            int idx = i * 4;
            data[idx] = b;
            data[idx + 1] = g;
            data[idx + 2] = r;
            data[idx + 3] = a;
        }

        Mat mat = new Mat(h, w, CvType.CV_8UC4);
        mat.put(0, 0, data);
        return mat;
    }

    private BufferedImage matToBufferedImageBGRA(Mat mat) {
        int w = mat.cols();
        int h = mat.rows();

        Mat bgra = mat;
        if (mat.type() != CvType.CV_8UC4) {
            // try to convert common types
            bgra = new Mat();
            if (mat.type() == CvType.CV_8UC3) {
                Imgproc.cvtColor(mat, bgra, Imgproc.COLOR_BGR2BGRA);
            } else if (mat.type() == CvType.CV_8UC1) {
                Imgproc.cvtColor(mat, bgra, Imgproc.COLOR_GRAY2BGRA);
            } else {
                mat.convertTo(bgra, CvType.CV_8UC4);
            }
        }

        byte[] data = new byte[w * h * 4];
        bgra.get(0, 0, data);

        BufferedImage img = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        int[] out = new int[w * h];

        for (int i = 0; i < out.length; i++) {
            int idx = i * 4;
            int b = data[idx] & 0xFF;
            int g = data[idx + 1] & 0xFF;
            int r = data[idx + 2] & 0xFF;
            int a = data[idx + 3] & 0xFF;

            out[i] = (a << 24) | (r << 16) | (g << 8) | b;
        }

        img.setRGB(0, 0, w, h, out, 0, w);

        if (bgra != mat) bgra.release();
        return img;
    }


    private BufferedImage toIntArgb(BufferedImage src) {
        if (src.getType() == BufferedImage.TYPE_INT_ARGB) return src;

        BufferedImage dst = new BufferedImage(src.getWidth(), src.getHeight(), BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = dst.createGraphics();
        g.setComposite(AlphaComposite.Src);
        g.drawImage(src, 0, 0, null);
        g.dispose();
        return dst;
    }

    private BufferedImage deepCopy(BufferedImage source) {
        ColorModel cm = source.getColorModel();
        boolean isAlphaPremultiplied = cm.isAlphaPremultiplied();
        WritableRaster raster = source.copyData(null);
        return new BufferedImage(cm, raster, isAlphaPremultiplied, null);
    }

    private int parseIntSafe(String s, int def) {
        try {
            if (s == null || s.isBlank()) return def;
            return Integer.parseInt(s);
        } catch (Exception e) {
            return def;
        }
    }

}