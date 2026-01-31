package com.near.opencv_convertor.filters.params;

import com.near.opencv_convertor.filters.FilterParams;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class DataMoshParams extends FilterParams {
    private int blockSize = 16;
    private int maxOffset = 30;
    private double chaos = 0.5;
    private double smear = 0.7;
}

