package com.near.opencv_convertor.filters.filters;

import com.near.opencv_convertor.filters.FilterParams;
import com.near.opencv_convertor.filters.ImageFilter;
import com.near.opencv_convertor.filters.params.ContrastParams;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.Scalar;

public class ContrastFilter implements ImageFilter {
    @Override
    public Mat apply(Mat image, FilterParams params) {
        ContrastParams p = (ContrastParams) params;
        double alpha = p.getAlpha();

        Mat floatImage = new Mat();
        image.convertTo(floatImage, CvType.CV_32F);

        Core.subtract(floatImage, new Scalar(128, 128, 128), floatImage);
        Core.multiply(floatImage, new Scalar(alpha, alpha, alpha), floatImage);
        Core.add(floatImage, new Scalar(128, 128, 128), floatImage);

        Mat result = new Mat();
        floatImage.convertTo(result, image.type());
        return result;
    }
}
