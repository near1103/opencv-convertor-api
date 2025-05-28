package com.near.opencv_convertor.filters.filters;

import com.near.opencv_convertor.filters.FilterParams;
import com.near.opencv_convertor.filters.ImageFilter;
import org.opencv.core.Core;
import org.opencv.core.Mat;

public class NegativeFilter implements ImageFilter {
    @Override
    public Mat apply(Mat input, FilterParams params) {
        Mat result = new Mat();
        Core.bitwise_not(input, result);
        return result;
    }
}
