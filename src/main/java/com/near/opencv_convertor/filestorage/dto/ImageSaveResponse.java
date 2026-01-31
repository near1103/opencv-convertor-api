package com.near.opencv_convertor.filestorage.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class ImageSaveResponse {
    private String message;
    private String id;
    private String format;
}