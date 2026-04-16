package com.near.opencv_convertor.transformations.transformations;

import com.near.opencv_convertor.transformations.ImageTransformation;
import com.near.opencv_convertor.transformations.TransformationParams;
import com.near.opencv_convertor.transformations.params.FlipMode;
import com.near.opencv_convertor.transformations.params.FlipParams;
import org.opencv.core.Core;
import org.opencv.core.Mat;

public class FlipTransformation implements ImageTransformation {

    @Override
    public Mat apply(Mat source, TransformationParams params) {
        FlipParams flipParams = (FlipParams) params;

        int flipCode = switch (flipParams.mode()) {
            case HORIZONTAL -> 1;
            case VERTICAL -> 0;
        };

        Mat result = new Mat();
        Core.flip(source, result, flipCode);
        return result;
    }
}