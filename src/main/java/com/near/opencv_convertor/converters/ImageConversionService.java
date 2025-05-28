package com.near.opencv_convertor.converters;

import org.springframework.stereotype.Service;
import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

@Service
public class ImageConversionService {

    private final ImageFormatConverter formatConverter;
    private final ImageProcessor imageProcessor;
    private final ImageIOService imageIOService;

    public ImageConversionService(ImageFormatConverter formatConverter, ImageProcessor imageProcessor, ImageIOService imageIOService) {
        this.formatConverter = formatConverter;
        this.imageProcessor = imageProcessor;
        this.imageIOService = imageIOService;
    }

    public File convert(File inputFile, SupportedImageFormat outputFormat) throws IOException, InterruptedException {
        String inputExt = ImageFormatConverter.getExtension(inputFile.getName());
        String targetExt = outputFormat.getExtension().toLowerCase();

        if ("webp".equalsIgnoreCase(inputExt) || "webp".equalsIgnoreCase(targetExt)) {
            return formatConverter.convert(inputFile, outputFormat);
        }

        BufferedImage image = ImageIO.read(inputFile);
        if (image == null) throw new IOException("Can't read image file");

        if ("ico".equalsIgnoreCase(targetExt)) {
            image = imageProcessor.cropToSquareTopLeft(image);
            image = imageProcessor.resizeIfNeeded(image, 256, 256);
            image = imageProcessor.convertToABGR(image);
        }

        return imageIOService.writeImage(image, targetExt);
    }

}

