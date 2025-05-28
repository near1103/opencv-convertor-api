package com.near.opencv_convertor.filters.filters;

import com.near.opencv_convertor.filters.FilterParams;
import com.near.opencv_convertor.filters.ImageFilter;
import com.near.opencv_convertor.filters.params.GaussianBlurParams;
import org.opencv.core.Mat;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;

public class GaussianBlurFilter implements ImageFilter {
    @Override
    public Mat apply(Mat image, FilterParams params) {
        GaussianBlurParams p = (GaussianBlurParams) params;
        int kSize = p.getKernelSize();

        if (kSize % 2 == 0) kSize += 1;
        if (kSize < 1) kSize = 1;

        Mat output = new Mat();
        Imgproc.GaussianBlur(image, output, new Size(kSize, kSize), 0);
        return output;
    }
}
