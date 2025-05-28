package com.near.opencv_convertor.filters.params;

import com.near.opencv_convertor.filters.FilterParams;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class EdgeDetectionParams extends FilterParams {
    private double threshold1;
    private double threshold2;
}