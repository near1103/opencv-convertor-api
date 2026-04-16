package com.near.opencv_convertor.transformations;

import com.near.opencv_convertor.transformations.transformations.CropTransformation;
import com.near.opencv_convertor.transformations.transformations.FlipTransformation;
import com.near.opencv_convertor.transformations.transformations.ResizeTransformation;
import com.near.opencv_convertor.transformations.transformations.RotateTransformation;

public class TransformationFactory {

    private TransformationFactory() {
    }

    public static ImageTransformation create(TransformationType type) {
        return switch (type) {
            case ROTATE -> new RotateTransformation();
            case FLIP -> new FlipTransformation();
            case CROP -> new CropTransformation();
            case RESIZE -> new ResizeTransformation();
        };
    }
}
