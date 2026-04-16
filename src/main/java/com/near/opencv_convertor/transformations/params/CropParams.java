package com.near.opencv_convertor.transformations.params;

import com.near.opencv_convertor.transformations.TransformationParams;

public record CropParams(int x, int y, int width, int height) implements TransformationParams {
}