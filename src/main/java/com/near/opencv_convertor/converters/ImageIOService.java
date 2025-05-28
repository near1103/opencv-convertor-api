package com.near.opencv_convertor.converters;

import org.springframework.stereotype.Service;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageOutputStream;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.Iterator;

@Service
public class ImageIOService {

    private static final String TEMP_PREFIX_CONVERTED = "converted-";

    public File writeImage(BufferedImage image, String formatName) throws IOException {
        String ext = formatName.toLowerCase();
        File outputFile = File.createTempFile(TEMP_PREFIX_CONVERTED, "." + ext);

        Iterator<ImageWriter> writers = ImageIO.getImageWritersByFormatName(ext);
        if (!writers.hasNext()) {
            throw new IOException("No ImageWriter found for format: " + ext);
        }

        ImageWriter writer = writers.next();
        try (ImageOutputStream ios = ImageIO.createImageOutputStream(outputFile)) {
            writer.setOutput(ios);
            ImageWriteParam param = writer.getDefaultWriteParam();

            if (param.canWriteCompressed()) {
                param.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
            }

            writer.write(null, new IIOImage(image, null, null), param);
        } finally {
            writer.dispose();
        }

        return outputFile;
    }
}

