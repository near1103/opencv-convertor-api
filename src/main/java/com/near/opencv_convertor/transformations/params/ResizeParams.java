package com.near.opencv_convertor.transformations.params;

import com.near.opencv_convertor.transformations.TransformationParams;

public record ResizeParams(
        int width,
        int height,
        boolean keepAspect,
        ResizeMethod method
) implements TransformationParams {
}
