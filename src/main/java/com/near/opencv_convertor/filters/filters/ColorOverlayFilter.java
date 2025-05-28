package com.near.opencv_convertor.filters.filters;
import com.near.opencv_convertor.filters.FilterParams;
import com.near.opencv_convertor.filters.ImageFilter;
import com.near.opencv_convertor.filters.params.ColorOverlayParams;
import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.Scalar;


public class ColorOverlayFilter implements ImageFilter {
    @Override
    public Mat apply(Mat image, FilterParams params) {
        ColorOverlayParams p = (ColorOverlayParams) params;

        Scalar overlayColor = new Scalar(p.getBlue(), p.getGreen(), p.getRed());

        Mat overlay = new Mat(image.size(), image.type(), overlayColor);

        Mat result = new Mat();
        Core.addWeighted(image, 1.0 - p.getAlpha(), overlay, p.getAlpha(), 0.0, result);

        return result;
    }
}
