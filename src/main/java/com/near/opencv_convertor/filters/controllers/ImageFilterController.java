package com.near.opencv_convertor.filters.controllers;

import com.near.opencv_convertor.dto.ResponseImage;
import com.near.opencv_convertor.filters.services.ImageFilterService;
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
public class ImageFilterController {

    private final ImageFilterService imageFilterService;

    @PostMapping(value = "/filter", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> applyFilter(
            @RequestParam("image") MultipartFile file,
            @RequestParam("type") String filterType,
            @RequestParam Map<String, String> params
    ) {

       ResponseImage result = imageFilterService.applyFilter(file, filterType, params);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "inline; filename=\"" + result.filename() + "\"")
                .contentType(result.mediaType())
                .contentLength(result.size())
                .body(result.resource());
    }
}
