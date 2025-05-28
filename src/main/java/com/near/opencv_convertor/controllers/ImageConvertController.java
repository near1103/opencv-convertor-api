package com.near.opencv_convertor.controllers;

import com.near.opencv_convertor.converters.ImageConversionService;
import com.near.opencv_convertor.converters.SupportedImageFormat;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

@RestController
@RequestMapping("/api/convert")
@RequiredArgsConstructor
public class ImageConvertController {

    private final ImageConversionService imageConversionService;

    @PostMapping(value = "/to", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<InputStreamResource> convertToFormat(
            @RequestParam("file") MultipartFile file,
            @RequestParam("format") String targetFormat
    ) throws IOException, InterruptedException {

        SupportedImageFormat target = SupportedImageFormat.fromExtension(targetFormat.toLowerCase())
                .orElseThrow(() -> new IllegalArgumentException("Unsupported target format: " + targetFormat));

        File inputFile = File.createTempFile("upload-", getExtensionWithDot(file));
        file.transferTo(inputFile);

        File outputFile = imageConversionService.convert(inputFile, target);

        InputStreamResource resource = new InputStreamResource(new FileInputStream(outputFile));

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"converted." + target.getExtension() + "\"")
                .contentLength(outputFile.length())
                .contentType(target.getMediaType())
                .body(resource);
    }

    private String getExtensionWithDot(MultipartFile file) {
        String name = file.getOriginalFilename();
        return name != null && name.contains(".") ? name.substring(name.lastIndexOf('.')) : ".png";
    }
}
