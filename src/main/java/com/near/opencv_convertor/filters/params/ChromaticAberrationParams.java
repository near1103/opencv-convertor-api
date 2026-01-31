package com.near.opencv_convertor.filters.params;

import com.near.opencv_convertor.filters.FilterParams;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ChromaticAberrationParams extends FilterParams {
    private double redStrength = 1.0;
    private double blueStrength = 1.0;
    private double greenStrength = 1.0;
    private double radialStrength = 5.0;
}
