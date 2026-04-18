package com.near.opencv_convertor.manual.controllers;

import com.near.opencv_convertor.dto.ResponseImage;
import com.near.opencv_convertor.manual.service.ImageManualEditService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

@RestController
@RequestMapping("/api/image")
@RequiredArgsConstructor
public class ImageManualEditController {

    private final ImageManualEditService imageManualEditService;

    @PostMapping(value = "/manual", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> applyManualEdit(
            @RequestParam("image") MultipartFile file,
            @RequestParam("type") String toolType,
            @RequestParam Map<String, String> params
    ) {
        ResponseImage result = imageManualEditService.applyManualEdit(file, toolType, params);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "inline; filename=\"" + result.filename() + "\"")
                .contentType(result.mediaType())
                .contentLength(result.size())
                .body(result.resource());
    }
}
