package com.near.opencv_convertor.filters.services;

import com.near.opencv_convertor.converters.enums.SupportedImageFormat;
import com.near.opencv_convertor.converters.services.ImageFormatConverter;
import com.near.opencv_convertor.dto.ResponseImage;
import com.near.opencv_convertor.filters.*;
import lombok.RequiredArgsConstructor;
import org.opencv.core.Mat;
import org.opencv.imgcodecs.Imgcodecs;
import org.springframework.core.io.InputStreamResource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.FileInputStream;
import java.nio.file.Files;
import java.util.Map;
import java.util.Objects;

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
}