package com.near.opencv_convertor.manual;

import org.opencv.core.Mat;
import org.opencv.imgproc.Imgproc;

public class AlphaChannelUtils {

    private AlphaChannelUtils() {
    }

    public static Mat ensureBgra(Mat image) {
        Mat result = new Mat();

        if (image.channels() == 4) {
            image.copyTo(result);
            return result;
        }

        if (image.channels() == 3) {
            Imgproc.cvtColor(image, result, Imgproc.COLOR_BGR2BGRA);
            return result;
        }

        if (image.channels() == 1) {
            Imgproc.cvtColor(image, result, Imgproc.COLOR_GRAY2BGRA);
            return result;
        }

        image.copyTo(result);
        return result;
    }
}