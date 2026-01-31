package com.near.opencv_convertor.converters.controllers;

import com.near.opencv_convertor.converters.enums.SupportedImageFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

@RestController
public class SupportedFormatController {

    @GetMapping("/api/formats")
    public List<Map<String, String>> getSupportedFormatsExtended() {
        return Arrays.stream(SupportedImageFormat.values())
                .map(format -> Map.of(
                        "value", format.getExtension(),
                        "label", format.getExtension().toUpperCase(),
                        "mimeType", format.getMediaType().toString()
                ))
                .toList();
    }
}