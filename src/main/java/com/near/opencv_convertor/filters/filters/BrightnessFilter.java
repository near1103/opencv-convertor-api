package com.near.opencv_convertor.filters.filters;

import com.near.opencv_convertor.filters.FilterParams;
import com.near.opencv_convertor.filters.ImageFilter;
import com.near.opencv_convertor.filters.params.BrightnessParams;
import org.opencv.core.Mat;

public class BrightnessFilter implements ImageFilter {
    @Override
    public Mat apply(Mat input, FilterParams params) {
        BrightnessParams bp = (BrightnessParams) params;
        int brightness = bp.getBrightness();

        Mat result = new Mat();
        input.convertTo(result, -1, 1, brightness);
        return result;
    }
}
