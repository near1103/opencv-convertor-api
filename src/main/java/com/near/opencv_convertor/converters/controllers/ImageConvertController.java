package com.near.opencv_convertor.converters.controllers;

import com.near.opencv_convertor.dto.ResponseImage;
import com.near.opencv_convertor.converters.services.ImageConversionService;
import com.near.opencv_convertor.converters.enums.SupportedImageFormat;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/convert")
@RequiredArgsConstructor
public class ImageConvertController {

    private final ImageConversionService imageConversionService;

    @PostMapping(value = "/to", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<InputStreamResource> convertToFormat(
            @RequestPart("file") MultipartFile file,
            @RequestPart("format") String targetFormat
    ) {

        SupportedImageFormat target = SupportedImageFormat
                .fromExtension(targetFormat.toLowerCase())
                .orElseThrow(() ->
                        new IllegalArgumentException("Unsupported target format: " + targetFormat)
                );

        ResponseImage result = imageConversionService.convert(file, target);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"" + result.filename() + "\"")
                .contentLength(result.size())
                .contentType(result.mediaType())
                .body(result.resource());
    }
}
