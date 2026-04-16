package com.near.opencv_convertor.transformations.services;

import com.near.opencv_convertor.dto.ResponseImage;
import com.near.opencv_convertor.transformations.*;
import lombok.extern.slf4j.Slf4j;
import org.opencv.core.Mat;
import org.opencv.core.MatOfByte;
import org.opencv.imgcodecs.Imgcodecs;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.MediaType;
import org.springframework.http.MediaTypeFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Locale;
import java.util.Map;

@Service
@Slf4j
public class ImageTransformationService {

    public ResponseImage applyTransformation(MultipartFile file, String transformationTypeRaw, Map<String, String> params) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Image file is empty");
        }

        TransformationType transformationType = TransformationType.valueOf(transformationTypeRaw.toUpperCase(Locale.ROOT));

        Mat source = null;
        Mat result = null;

        try {
            source = readImage(file);

            ImageTransformation transformation = TransformationFactory.create(transformationType);
            TransformationParams transformationParams = TransformationParamsFactory.create(transformationType, params);

            result = transformation.apply(source, transformationParams);

            String extension = resolveExtension(file.getOriginalFilename());
            byte[] resultBytes = encodeImage(result, extension);

            String outputFilename = buildOutputFilename(file.getOriginalFilename(), transformationType, extension);
            MediaType mediaType = MediaTypeFactory.getMediaType(outputFilename)
                    .orElse(MediaType.APPLICATION_OCTET_STREAM);

            return new ResponseImage(
                    new InputStreamResource(new ByteArrayInputStream(resultBytes)),
                    resultBytes.length,
                    outputFilename,
                    mediaType
            );
        } catch (IOException e) {
            throw new RuntimeException("Failed to process transformation", e);
        } finally {
            if (source != null) {
                source.release();
            }
            if (result != null) {
                result.release();
            }
        }
    }

    private Mat readImage(MultipartFile file) throws IOException {
        byte[] bytes = file.getBytes();
        MatOfByte mob = new MatOfByte(bytes);
        Mat image = Imgcodecs.imdecode(mob, Imgcodecs.IMREAD_UNCHANGED);
        mob.release();

        if (image.empty()) {
            image.release();
            throw new IllegalArgumentException("Failed to decode image");
        }

        return image;
    }

    private byte[] encodeImage(Mat image, String extension) {
        String normalizedExtension = extension.startsWith(".") ? extension : "." + extension;

        MatOfByte mob = new MatOfByte();
        boolean success = Imgcodecs.imencode(normalizedExtension, image, mob);
        if (!success) {
            mob.release();
            throw new IllegalArgumentException("Failed to encode image with extension: " + normalizedExtension);
        }

        byte[] bytes = mob.toArray();
        mob.release();
        return bytes;
    }

    private String resolveExtension(String originalFilename) {
        if (originalFilename == null || !originalFilename.contains(".")) {
            return "png";
        }

        String extension = originalFilename.substring(originalFilename.lastIndexOf('.') + 1).toLowerCase(Locale.ROOT);

        return switch (extension) {
            case "jpg", "jpeg" -> "jpg";
            case "png" -> "png";
            case "bmp" -> "bmp";
            case "webp" -> "webp";
            case "tif", "tiff" -> "tiff";
            default -> "png";
        };
    }

    private String buildOutputFilename(String originalFilename, TransformationType transformationType, String extension) {
        String baseName = originalFilename == null ? "image" : originalFilename;
        int dotIndex = baseName.lastIndexOf('.');
        if (dotIndex > 0) {
            baseName = baseName.substring(0, dotIndex);
        }

        return baseName + "_" + transformationType.name().toLowerCase(Locale.ROOT) + "." + extension;
    }
}