package com.near.opencv_convertor.filters.filters;

import com.near.opencv_convertor.filters.FilterParams;
import com.near.opencv_convertor.filters.ImageFilter;
import com.near.opencv_convertor.filters.params.NoiseParams;
import org.opencv.core.Mat;
import org.opencv.core.Core;

public class NoiseFilter implements ImageFilter {
    @Override
    public Mat apply(Mat input, FilterParams params) {
        NoiseParams noiseParams = (NoiseParams) params;

        Mat noise = new Mat(input.size(), input.type());
        Core.randn(noise, noiseParams.getMean(), noiseParams.getStddev());

        Mat result = new Mat();
        Core.add(input, noise, result);
        return result;
    }
}
