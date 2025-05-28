package com.near.opencv_convertor.filters;
import org.opencv.core.*;

public interface ImageFilter {
    Mat apply(Mat image, FilterParams params);

    default boolean requiresAlpha() {
        return false;
    }
}

