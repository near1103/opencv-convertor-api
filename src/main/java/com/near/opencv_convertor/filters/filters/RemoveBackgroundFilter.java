package com.near.opencv_convertor.filters.filters;

import com.near.opencv_convertor.filters.FilterParams;
import com.near.opencv_convertor.filters.ImageFilter;
import com.near.opencv_convertor.filters.params.RemoveBackgroundParams;
import org.opencv.core.Mat;
import org.opencv.core.Scalar;
import org.opencv.imgproc.Imgproc;

public class RemoveBackgroundFilter implements ImageFilter {

    @Override
    public Mat apply(Mat image, FilterParams params) {
        RemoveBackgroundParams p = (RemoveBackgroundParams) params;

        Mat result = new Mat();
        if (image.channels() == 3) {
            Imgproc.cvtColor(image, result, Imgproc.COLOR_BGR2BGRA);
        } else {
            image.copyTo(result);
        }

        Scalar bgColor = new Scalar(p.getBlue(), p.getGreen(), p.getRed());
        double threshold = p.getThreshold();

        for (int y = 0; y < result.rows(); y++) {
            for (int x = 0; x < result.cols(); x++) {
                double[] pixel = result.get(y, x);

                double distance = Math.sqrt(
                        Math.pow(pixel[0] - bgColor.val[0], 2) +
                                Math.pow(pixel[1] - bgColor.val[1], 2) +
                                Math.pow(pixel[2] - bgColor.val[2], 2)
                );

                if (distance < threshold) {
                    result.put(y, x, 0, 0, 0, 0);
                }
            }
        }

        return result;
    }
    @Override
    public boolean requiresAlpha() {
        return true;
    }
}


