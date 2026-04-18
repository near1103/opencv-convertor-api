package com.near.opencv_convertor.projects.dto;

import lombok.Getter;
import lombok.Setter;

import java.util.HashMap;
import java.util.Map;

@Getter
@Setter
public class ProjectOperationDto {
    private Integer order;
    private String category;
    private String tool;
    private Map<String, Object> params = new HashMap<>();
}