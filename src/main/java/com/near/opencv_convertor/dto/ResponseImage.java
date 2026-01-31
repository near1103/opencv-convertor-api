package com.near.opencv_convertor.dto;

import org.springframework.core.io.InputStreamResource;
import org.springframework.http.MediaType;

public record ResponseImage(
        InputStreamResource resource,
        long size,
        String filename,
        MediaType mediaType
) {}
