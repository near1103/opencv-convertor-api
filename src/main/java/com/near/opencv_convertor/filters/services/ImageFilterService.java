package com.near.opencv_convertor.filters.services;

import com.near.opencv_convertor.converters.enums.SupportedImageFormat;
import com.near.opencv_convertor.converters.services.ImageFormatConverter;
import com.near.opencv_convertor.dto.ResponseImage;
import com.near.opencv_convertor.filters.FilterFactory;
import com.near.opencv_convertor.filters.FilterParams;
import com.near.opencv_convertor.filters.FilterParamsFactory;
import com.near.opencv_convertor.filters.FilterType;
import com.near.opencv_convertor.filters.ImageFilter;
import com.near.opencv_convertor.filters.filters.ObamifyFilter;
import com.near.opencv_convertor.gifprocessing.GifProcessingService;
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
    private final GifProcessingService gifProcessingService;
    private final FilterFactory filterFactory;

    public ResponseImage applyFilter(
            MultipartFile file,
            String filterType,
            Map<String, String> params
    ) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Image file is empty");
        }

        FilterType type = parseFilterType(filterType);
        FilterParams filterParams = FilterParamsFactory.create(type, params);

        ImageFilter filter = filterFactory.create(type);

        if (type == FilterType.OBAMIFY) {
            ObamifyFilter obamifyFilter = (ObamifyFilter) filter;
            return obamifyFilter.applyToFile(file, filterParams);
        }

        SupportedImageFormat imageFormat = detectFormat(file);

        if (imageFormat == SupportedImageFormat.GIF) {
            return gifProcessingService.processGif(
                    file,
                    frame -> filter.apply(frame, filterParams),
                    "filtered.gif"
            );
        }

        File input = null;
        File readable = null;
        File output = null;

        Mat source = null;
        Mat result = null;

        try {
            input = File.createTempFile("input-", "." + imageFormat.getExtension());
            file.transferTo(input);

            readable = imageFormatConverter.convertToReadableForOpenCV(input);

            source = Imgcodecs.imread(
                    readable.getAbsolutePath(),
                    Imgcodecs.IMREAD_UNCHANGED
            );

            if (source.empty()) {
                throw new IllegalArgumentException("Failed to decode image");
            }

            result = filter.apply(source, filterParams);

            if (filter.requiresAlpha()) {
                imageFormat = SupportedImageFormat.PNG;
            }

            output = File.createTempFile("output-", "." + imageFormat.getExtension());

            boolean written = Imgcodecs.imwrite(output.getAbsolutePath(), result);

            if (!written || !output.exists() || output.length() == 0) {
                throw new IllegalArgumentException("Failed to encode filtered image");
            }

            return new ResponseImage(
                    new InputStreamResource(new FileInputStream(output)),
                    output.length(),
                    "filtered." + imageFormat.getExtension(),
                    imageFormat.getMediaType()
            );

        } catch (Exception e) {
            throw new RuntimeException("Failed to apply filter", e);
        } finally {
            if (source != null) {
                source.release();
            }

            if (result != null) {
                result.release();
            }

            safeDelete(input);
            safeDelete(readable);
            safeDelete(output);
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