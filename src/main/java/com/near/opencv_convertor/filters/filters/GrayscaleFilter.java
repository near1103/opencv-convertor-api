package com.near.opencv_convertor.filters.filters;

import com.near.opencv_convertor.filters.FilterParams;
import com.near.opencv_convertor.filters.ImageFilter;
import org.opencv.core.Mat;
import org.opencv.imgproc.Imgproc;

public class GrayscaleFilter implements ImageFilter {
    @Override
    public Mat apply(Mat image, FilterParams params) {
        Mat gray = new Mat();
        Imgproc.cvtColor(image, gray, Imgproc.COLOR_BGR2GRAY);
        Imgproc.cvtColor(gray, gray, Imgproc.COLOR_GRAY2BGR);
        return gray;
    }
}
