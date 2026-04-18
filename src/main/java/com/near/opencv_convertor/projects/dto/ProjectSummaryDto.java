package com.near.opencv_convertor.projects.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ProjectSummaryDto {
    private String projectId;
    private String name;
    private String sourcePath;
    private String resultPath;
    private String sourceFormatId;
    private String resultFormatId;
    private Long createdAt;
    private Long updatedAt;
    private Integer operationsCount;
    private String previewTool;
}