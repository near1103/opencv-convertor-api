package com.near.opencv_convertor.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ObamifyTempPresetDto {
    private String preset;
    private String presetId;
    private String label;
    private String description;
}