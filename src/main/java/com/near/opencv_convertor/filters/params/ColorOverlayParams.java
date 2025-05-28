package com.near.opencv_convertor.filters.params;

import com.near.opencv_convertor.filters.FilterParams;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ColorOverlayParams extends FilterParams {
    private int red;
    private int green;
    private int blue;
    private double alpha;
}
