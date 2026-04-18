package com.near.opencv_convertor.manual.service;

import com.near.opencv_convertor.converters.enums.SupportedImageFormat;
import com.near.opencv_convertor.converters.services.ImageFormatConverter;
import com.near.opencv_convertor.dto.ResponseImage;
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
import java.util.Map;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class ImageManualEditService {

    private final ImageFormatConverter imageFormatConverter;

    public ResponseImage applyManualEdit(
            MultipartFile file,
            String toolType,
            Map<String, String> params
    ) {
        try {
            String ext = ImageFormatConverter.getExtension(
                    Objects.requireNonNull(file.getOriginalFilename())
            );

            SupportedImageFormat imageFormat = SupportedImageFormat.fromExtension(ext)
                    .orElse(SupportedImageFormat.PNG);

            File uploadedFile = File.createTempFile("manual-input-", "." + imageFormat.getExtension());
            file.transferTo(uploadedFile);

            File converted = imageFormatConverter.convertToReadableForOpenCV(uploadedFile);
            Mat input = Imgcodecs.imread(converted.getAbsolutePath(), Imgcodecs.IMREAD_UNCHANGED);

            String normalizedToolType = toolType;
            if (normalizedToolType != null && normalizedToolType.contains(",")) {
                normalizedToolType = normalizedToolType.split(",")[0].trim();
            }

            ManualEditType type = ManualEditType.valueOf(normalizedToolType.toUpperCase());
            ImageManualTool tool = ManualEditFactory.create(type);
            ManualEditParams manualEditParams = ManualEditParamsFactory.create(type, params);

            Mat result = tool.apply(input, manualEditParams);

            if (tool.requiresAlpha()) {
                imageFormat = SupportedImageFormat.PNG;
            }

            File output = File.createTempFile("manual-output-", "." + imageFormat.getExtension());
            Imgcodecs.imwrite(output.getAbsolutePath(), result);

            byte[] bytes = Files.readAllBytes(output.toPath());
            MediaType mediaType = imageFormat.getMediaType();

            return new ResponseImage(
                    new InputStreamResource(new ByteArrayInputStream(bytes)),
                    bytes.length,
                    output.getName(),
                    mediaType
            );
        } catch (Exception e) {
            throw new RuntimeException("Failed to apply manual edit: " + e.getMessage(), e);
        }
    }
}
