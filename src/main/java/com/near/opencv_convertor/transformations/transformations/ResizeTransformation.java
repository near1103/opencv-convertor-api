package com.near.opencv_convertor.transformations.transformations;

import com.near.opencv_convertor.transformations.ImageTransformation;
import com.near.opencv_convertor.transformations.TransformationParams;
import com.near.opencv_convertor.transformations.params.ResizeMethod;
import com.near.opencv_convertor.transformations.params.ResizeParams;
import org.opencv.core.Mat;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;

public class ResizeTransformation implements ImageTransformation {

    @Override
    public Mat apply(Mat source, TransformationParams params) {
        ResizeParams resizeParams = (ResizeParams) params;

        if (resizeParams.width() <= 0 || resizeParams.height() <= 0) {
            throw new IllegalArgumentException("Resize width and height must be greater than 0");
        }

        int targetWidth = resizeParams.width();
        int targetHeight = resizeParams.height();

        if (resizeParams.keepAspect()) {
            double widthRatio = (double) targetWidth / source.cols();
            double heightRatio = (double) targetHeight / source.rows();
            double scale = Math.min(widthRatio, heightRatio);

            targetWidth = Math.max(1, (int) Math.round(source.cols() * scale));
            targetHeight = Math.max(1, (int) Math.round(source.rows() * scale));
        }

        int interpolation = mapInterpolation(resizeParams.method());

        Mat result = new Mat();
        Imgproc.resize(source, result, new Size(targetWidth, targetHeight), 0, 0, interpolation);
        return result;
    }

    private int mapInterpolation(ResizeMethod method) {
        return switch (method) {
            case NEAREST -> Imgproc.INTER_NEAREST;
            case BILINEAR -> Imgproc.INTER_LINEAR;
            case BICUBIC -> Imgproc.INTER_CUBIC;
            case LANCZOS -> Imgproc.INTER_LANCZOS4;
        };
    }
}