package com.near.opencv_convertor.transformations.controllers;

import com.near.opencv_convertor.dto.ResponseImage;
import com.near.opencv_convertor.transformations.services.ImageTransformationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

@RestController
@RequestMapping("/api/image")
@RequiredArgsConstructor
public class ImageTransformationController {

    private final ImageTransformationService imageTransformationService;

    @PostMapping(value = "/transform", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> applyTransformation(
            @RequestParam("image") MultipartFile file,
            @RequestParam("type") String transformationType,
            @RequestParam Map<String, String> params
    ) {
        ResponseImage result = imageTransformationService.applyTransformation(file, transformationType, params);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + result.filename() + "\"")
                .contentType(result.mediaType())
                .contentLength(result.size())
                .body(result.resource());
    }
}