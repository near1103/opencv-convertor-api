package com.near.opencv_convertor.filters.filters;

import com.near.opencv_convertor.filters.FilterParams;
import com.near.opencv_convertor.filters.ImageFilter;
import com.near.opencv_convertor.filters.params.BlurParams;
import org.opencv.core.Mat;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;

public class BlurFilter implements ImageFilter {
    @Override
    public Mat apply(Mat input, FilterParams params) {
        BlurParams p = (BlurParams) params;
        int kSize = p.getKernelSize();

        if (kSize < 1) kSize = 1;
        if (kSize % 2 == 0) kSize += 1;

        Mat result = new Mat();
        Imgproc.blur(input, result, new Size(kSize, kSize));
        return result;
    }
}
