package com.near.opencv_convertor.filters.filters;

import com.near.opencv_convertor.filters.FilterParams;
import com.near.opencv_convertor.filters.ImageFilter;
import com.near.opencv_convertor.filters.params.EdgeDetectionParams;
import org.opencv.core.Mat;
import org.opencv.imgproc.Imgproc;

public class EdgeDetectionFilter implements ImageFilter {

    @Override
    public Mat apply(Mat input, FilterParams params) {
        EdgeDetectionParams p = (EdgeDetectionParams) params;

        Mat gray = new Mat();
        Imgproc.cvtColor(input, gray, Imgproc.COLOR_BGR2GRAY);

        Mat edges = new Mat();
        Imgproc.Canny(gray, edges, p.getThreshold1(), p.getThreshold2());

        Mat result = new Mat();
        Imgproc.cvtColor(edges, result, Imgproc.COLOR_GRAY2BGR);

        return result;
    }
}