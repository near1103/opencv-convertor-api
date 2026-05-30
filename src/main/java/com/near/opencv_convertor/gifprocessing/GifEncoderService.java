package com.near.opencv_convertor.gifprocessing;

import org.springframework.stereotype.Service;
import org.w3c.dom.Node;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageTypeSpecifier;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.metadata.IIOMetadata;
import javax.imageio.metadata.IIOMetadataNode;
import javax.imageio.stream.ImageOutputStream;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Iterator;
import java.util.List;

@Service
public class GifEncoderService {

    public byte[] encodeGif(List<BufferedImage> frames, int delayMs) throws IOException {
        if (frames == null || frames.isEmpty()) {
            throw new IllegalArgumentException("GIF frames list is empty");
        }

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

        try (ImageOutputStream imageOutputStream = ImageIO.createImageOutputStream(outputStream)) {
            Iterator<ImageWriter> writers = ImageIO.getImageWritersByFormatName("gif");

            if (!writers.hasNext()) {
                throw new IOException("No GIF ImageWriter found");
            }

            ImageWriter writer = writers.next();

            try {
                writer.setOutput(imageOutputStream);
                writer.prepareWriteSequence(null);

                for (BufferedImage frame : frames) {
                    if (frame == null) {
                        throw new IOException("GIF frame is null");
                    }

                    BufferedImage normalizedFrame = normalizeFrame(frame);
                    IIOMetadata metadata = createGifMetadata(writer, normalizedFrame, delayMs);

                    writer.writeToSequence(
                            new IIOImage(normalizedFrame, null, metadata),
                            null
                    );
                }

                writer.endWriteSequence();
            } finally {
                writer.dispose();
            }
        }

        return outputStream.toByteArray();
    }

    private IIOMetadata createGifMetadata(
            ImageWriter writer,
            BufferedImage image,
            int delayMs
    ) throws IOException {
        if (image == null) {
            throw new IOException("Cannot create GIF metadata for null image");
        }

        ImageWriteParam params = writer.getDefaultWriteParam();

        IIOMetadata metadata = writer.getDefaultImageMetadata(
                ImageTypeSpecifier.createFromBufferedImageType(image.getType()),
                params
        );

        String metaFormatName = metadata.getNativeMetadataFormatName();
        Node root = metadata.getAsTree(metaFormatName);

        IIOMetadataNode graphicsControlExtensionNode = getOrCreateNode(
                root,
                "GraphicControlExtension"
        );

        graphicsControlExtensionNode.setAttribute("disposalMethod", "none");
        graphicsControlExtensionNode.setAttribute("userInputFlag", "FALSE");
        graphicsControlExtensionNode.setAttribute("transparentColorFlag", "FALSE");
        graphicsControlExtensionNode.setAttribute("delayTime", String.valueOf(Math.max(1, delayMs / 10)));
        graphicsControlExtensionNode.setAttribute("transparentColorIndex", "0");

        IIOMetadataNode appExtensionsNode = getOrCreateNode(
                root,
                "ApplicationExtensions"
        );

        IIOMetadataNode appExtensionNode = new IIOMetadataNode("ApplicationExtension");
        appExtensionNode.setAttribute("applicationID", "NETSCAPE");
        appExtensionNode.setAttribute("authenticationCode", "2.0");

        // 0 = infinite loop
        appExtensionNode.setUserObject(new byte[]{0x1, 0x0, 0x0});
        appExtensionsNode.appendChild(appExtensionNode);

        metadata.setFromTree(metaFormatName, root);

        return metadata;
    }

    private IIOMetadataNode getOrCreateNode(Node rootNode, String nodeName) {
        for (int i = 0; i < rootNode.getChildNodes().getLength(); i++) {
            Node childNode = rootNode.getChildNodes().item(i);

            if (nodeName.equalsIgnoreCase(childNode.getNodeName())) {
                return (IIOMetadataNode) childNode;
            }
        }

        IIOMetadataNode node = new IIOMetadataNode(nodeName);
        rootNode.appendChild(node);

        return node;
    }

    private BufferedImage normalizeFrame(BufferedImage source) {
        if (source.getType() == BufferedImage.TYPE_INT_RGB) {
            return source;
        }

        BufferedImage converted = new BufferedImage(
                source.getWidth(),
                source.getHeight(),
                BufferedImage.TYPE_INT_RGB
        );

        var graphics = converted.createGraphics();
        try {
            graphics.drawImage(source, 0, 0, null);
        } finally {
            graphics.dispose();
        }

        return converted;
    }
}