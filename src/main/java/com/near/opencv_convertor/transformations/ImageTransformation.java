package com.near.opencv_convertor.transformations;

import org.opencv.core.Mat;

public interface ImageTransformation {
    Mat apply(Mat source, TransformationParams params);
}
