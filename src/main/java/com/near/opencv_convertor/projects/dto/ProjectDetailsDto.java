package com.near.opencv_convertor.projects.dto;

import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
public class ProjectDetailsDto {
    private ProjectSummaryDto project;
    private List<ProjectOperationDto> operations = new ArrayList<>();
}