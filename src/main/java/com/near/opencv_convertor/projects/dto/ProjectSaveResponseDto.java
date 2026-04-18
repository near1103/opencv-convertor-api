package com.near.opencv_convertor.projects.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class ProjectSaveResponseDto {
    private String projectId;
    private String message;
}
