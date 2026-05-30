package com.near.opencv_convertor.manual.service;

import com.near.opencv_convertor.converters.enums.SupportedImageFormat;
import com.near.opencv_convertor.converters.services.ImageFormatConverter;
import com.near.opencv_convertor.dto.ResponseImage;
import com.near.opencv_convertor.gifprocessing.GifProcessingService;
import com.near.opencv_convertor.manual.ImageManualTool;
import com.near.opencv_convertor.manual.ManualEditFactory;
import com.near.opencv_convertor.manual.ManualEditParams;
import com.near.opencv_convertor.manual.ManualEditParamsFactory;
import com.near.opencv_convertor.manual.enums.ManualEditType;
import lombok.RequiredArgsConstructor;
import org.opencv.core.Mat;
import org.opencv.imgcodecs.Imgcodecs;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.nio.file.Files;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class ImageManualEditService {

    private final ImageFormatConverter imageFormatConverter;
    private final GifProcessingService gifProcessingService;

    public ResponseImage applyManualEdit(
            MultipartFile file,
            String toolType,
            Map<String, String> params
    ) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Image file is empty");
        }

        File uploadedFile = null;
        File converted = null;
        File output = null;

        Mat input = null;
        Mat result = null;

        try {
            String originalFilename = Objects.requireNonNull(file.getOriginalFilename());

            String ext = ImageFormatConverter.getExtension(originalFilename);

            SupportedImageFormat imageFormat = SupportedImageFormat.fromExtension(ext)
                    .orElse(SupportedImageFormat.PNG);

            String normalizedToolType = toolType;
            if (normalizedToolType != null && normalizedToolType.contains(",")) {
                normalizedToolType = normalizedToolType.split(",")[0].trim();
            }

            ManualEditType type = ManualEditType.valueOf(normalizedToolType.toUpperCase(Locale.ROOT));
            ImageManualTool tool = ManualEditFactory.create(type);
            ManualEditParams manualEditParams = ManualEditParamsFactory.create(type, params);

            if (imageFormat == SupportedImageFormat.GIF) {
                String outputFilename = buildOutputFilename(originalFilename, type, "gif");

                return gifProcessingService.processGif(
                        file,
                        frame -> tool.apply(frame, manualEditParams),
                        outputFilename
                );
            }

            uploadedFile = File.createTempFile("manual-input-", "." + imageFormat.getExtension());
            file.transferTo(uploadedFile);

            converted = imageFormatConverter.convertToReadableForOpenCV(uploadedFile);
            input = Imgcodecs.imread(converted.getAbsolutePath(), Imgcodecs.IMREAD_UNCHANGED);

            if (input.empty()) {
                throw new IllegalArgumentException("Failed to decode image");
            }

            result = tool.apply(input, manualEditParams);

            if (tool.requiresAlpha()) {
                imageFormat = SupportedImageFormat.PNG;
            }

            output = File.createTempFile("manual-output-", "." + imageFormat.getExtension());
            Imgcodecs.imwrite(output.getAbsolutePath(), result);

            byte[] bytes = Files.readAllBytes(output.toPath());
            MediaType mediaType = imageFormat.getMediaType();

            String outputFilename = buildOutputFilename(
                    originalFilename,
                    type,
                    imageFormat.getExtension()
            );

            return new ResponseImage(
                    new InputStreamResource(new ByteArrayInputStream(bytes)),
                    bytes.length,
                    outputFilename,
                    mediaType
            );
        } catch (Exception e) {
            throw new RuntimeException("Failed to apply manual edit: " + e.getMessage(), e);
        } finally {
            if (input != null) {
                input.release();
            }
            if (result != null) {
                result.release();
            }

            deleteTempFile(uploadedFile);
            deleteTempFile(converted);
            deleteTempFile(output);
        }
    }

    private String buildOutputFilename(String originalFilename, ManualEditType type, String extension) {
        String baseName = originalFilename == null ? "image" : originalFilename;

        int dotIndex = baseName.lastIndexOf('.');
        if (dotIndex > 0) {
            baseName = baseName.substring(0, dotIndex);
        }

        return baseName + "_" + type.name().toLowerCase(Locale.ROOT) + "." + extension;
    }

    private void deleteTempFile(File file) {
        if (file != null && file.exists()) {
            try {
                Files.deleteIfExists(file.toPath());
            } catch (Exception ignored) {
                // temp cleanup should not break image processing response
            }
        }
    }
}
