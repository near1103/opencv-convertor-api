package com.near.opencv_convertor.transformations.transformations;

import com.near.opencv_convertor.transformations.ImageTransformation;
import com.near.opencv_convertor.transformations.TransformationParams;
import com.near.opencv_convertor.transformations.params.CropParams;
import org.opencv.core.Mat;
import org.opencv.core.Rect;

public class CropTransformation implements ImageTransformation {

    @Override
    public Mat apply(Mat source, TransformationParams params) {
        CropParams cropParams = (CropParams) params;

        if (cropParams.width() <= 0 || cropParams.height() <= 0) {
            throw new IllegalArgumentException("Crop width and height must be greater than 0");
        }

        int x = Math.max(0, cropParams.x());
        int y = Math.max(0, cropParams.y());

        if (x >= source.cols() || y >= source.rows()) {
            throw new IllegalArgumentException("Crop origin is outside image bounds");
        }

        int maxWidth = source.cols() - x;
        int maxHeight = source.rows() - y;

        int width = Math.min(cropParams.width(), maxWidth);
        int height = Math.min(cropParams.height(), maxHeight);

        Rect rect = new Rect(x, y, width, height);
        return new Mat(source, rect).clone();
    }
}