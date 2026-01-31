package com.near.opencv_convertor.converters.services;

import com.near.opencv_convertor.dto.ResponseImage;
import com.near.opencv_convertor.converters.enums.SupportedImageFormat;
import org.springframework.core.io.InputStreamResource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileInputStream;
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

        System.out.println("Input file name: " + inputFile.getName());
        System.out.println("Input extension: " + inputExt);
        System.out.println("Output format: " + outputFormat.getExtension());

        if (!SupportedImageFormat.isSupported(inputExt)) {
            throw new IOException("Unsupported input format: " + inputExt);
        }

        if (!SupportedImageFormat.isSupported(targetExt)) {
            throw new IOException("Unsupported output format: " + targetExt);
        }

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

        if (!outputFormat.supportsAlpha()) {
            image = imageProcessor.removeAlphaChannel(image, Color.WHITE);
        }

        return imageIOService.writeImage(image, targetExt);
    }

    public ResponseImage convert(MultipartFile multipartFile, SupportedImageFormat target)
    {
        File input = null;
        File output = null;

        try {
            String ext = ImageFormatConverter.getExtension(
                    multipartFile.getOriginalFilename()
            );

            input = File.createTempFile("upload-", "." + ext);
            multipartFile.transferTo(input);

            output = convert(input, target);

            InputStreamResource resource =
                    new InputStreamResource(new FileInputStream(output));

            return new ResponseImage(
                    resource,
                    output.length(),
                    "converted." + target.getExtension(),
                    target.getMediaType()
            );

        } catch (Exception e) {
            throw new RuntimeException("Image conversion failed", e);
        } finally {
            if (input != null) input.delete();
        }
    }

}

