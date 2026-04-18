package com.near.opencv_convertor.manual;

import org.opencv.core.Mat;

public interface ImageManualTool {
    Mat apply(Mat image, ManualEditParams params);

    default boolean requiresAlpha() {
        return false;
    }
}