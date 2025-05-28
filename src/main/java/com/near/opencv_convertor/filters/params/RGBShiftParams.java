package com.near.opencv_convertor.filters.params;

import com.near.opencv_convertor.filters.FilterParams;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class RGBShiftParams extends FilterParams {
    private int redDx;
    private int redDy;
    private int greenDx;
    private int greenDy;
    private int blueDx;
    private int blueDy;
}
