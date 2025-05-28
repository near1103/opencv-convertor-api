package com.near.opencv_convertor.converters;

import org.springframework.stereotype.Service;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

@Service
public class ImageFormatConverter {

    private static final String TEMP_PREFIX_CONVERTED = "converted-";

    public File convert(File inputFile, SupportedImageFormat outputFormat) throws IOException, InterruptedException {
        String inputExt = getExtension(inputFile.getName());

        if ("webp".equalsIgnoreCase(inputExt)) {
            return convertFromWebp(inputFile, outputFormat);
        }

        if ("webp".equalsIgnoreCase(outputFormat.getExtension())) {
            return convertToWebp(inputFile);
        }

        throw new UnsupportedOperationException("Conversion from " + inputExt + " to " + outputFormat.getExtension() + " not supported in this class");
    }

    private File convertToWebp(File inputFile) throws IOException, InterruptedException {
        File outputFile = createTempOutputFile(TEMP_PREFIX_CONVERTED, "webp");
        ProcessBuilder pb = new ProcessBuilder("cwebp", inputFile.getAbsolutePath(), "-o", outputFile.getAbsolutePath());
        Process process = pb.start();
        if (process.waitFor() != 0) {
            throw new IOException("Error calling cwebp");
        }
        return outputFile;
    }

    private File convertFromWebp(File inputFile, SupportedImageFormat targetFormat) throws IOException, InterruptedException {
        File outputFile = createTempOutputFile(TEMP_PREFIX_CONVERTED, targetFormat.getExtension());
        ProcessBuilder pb = new ProcessBuilder("dwebp", inputFile.getAbsolutePath(), "-o", outputFile.getAbsolutePath());
        Process process = pb.start();
        if (process.waitFor() != 0) {
            throw new IOException("Error calling dwebp");
        }
        return outputFile;
    }

    private File createTempOutputFile(String prefix, String extension) throws IOException {
        return File.createTempFile(prefix, "." + extension);
    }

    public static String getExtension(String filename) {
        int i = filename.lastIndexOf('.');
        return i >= 0 ? filename.substring(i + 1).toLowerCase() : "";
    }

    public File convertToReadableForOpenCV(File inputFile) throws IOException, InterruptedException {
        String ext = getExtension(inputFile.getName());
        if ("webp".equalsIgnoreCase(ext)) {
            return convertFromWebp(inputFile, SupportedImageFormat.PNG);
        }

        BufferedImage image = ImageIO.read(inputFile);
        if (image == null) throw new IOException("Error reading image file");

        File outputFile = createTempOutputFile("opencv-", "png");
        ImageIO.write(image, "png", outputFile);
        return outputFile;
    }
}

