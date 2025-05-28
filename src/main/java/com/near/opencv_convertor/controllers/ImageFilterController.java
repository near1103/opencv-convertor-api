package com.near.opencv_convertor.controllers;

import com.near.opencv_convertor.converters.ImageFormatConverter;
import com.near.opencv_convertor.converters.SupportedImageFormat;
import com.near.opencv_convertor.filters.*;
import org.opencv.core.Mat;
import org.opencv.imgcodecs.Imgcodecs;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Map;
import java.util.Objects;

@RestController
@RequestMapping("/api/image")
public class ImageFilterController {

    ImageFormatConverter imageFormatConverter = new ImageFormatConverter();

    @PostMapping(value = "/filter", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<byte[]> applyFilter(@RequestParam("image") MultipartFile file,
                                              @RequestParam("type") String filterType,
                                              @RequestParam Map<String, String> params) throws IOException, InterruptedException {

        String ext = ImageFormatConverter.getExtension(Objects.requireNonNull(file.getOriginalFilename()));
        SupportedImageFormat imageFormat = SupportedImageFormat.fromExtension(ext)
                .orElse(SupportedImageFormat.PNG);

        File uploadedFile = File.createTempFile("input-", "." + imageFormat.getExtension());
        file.transferTo(uploadedFile);

        File converted = imageFormatConverter.convertToReadableForOpenCV(uploadedFile);
        Mat input = Imgcodecs.imread(converted.getAbsolutePath());

        FilterType type;
        try {
            type = FilterType.valueOf(filterType.toUpperCase());
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(("Unknown filter: " + filterType).getBytes());
        }

        ImageFilter filter = FilterFactory.create(type);
        FilterParams filterParams = FilterParamsFactory.create(type, params);
        Mat result = filter.apply(input, filterParams);

        if (filter.requiresAlpha()) {
            imageFormat = SupportedImageFormat.PNG;
        }

        File output = File.createTempFile("output-", "." + imageFormat.getExtension());
        Imgcodecs.imwrite(output.getAbsolutePath(), result);
        byte[] imageBytes = Files.readAllBytes(output.toPath());

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(imageFormat.getMediaType());

        return new ResponseEntity<>(imageBytes, headers, HttpStatus.OK);
    }
}
