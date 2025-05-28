package com.near.opencv_convertor.filters.filters;

import com.near.opencv_convertor.filters.FilterParams;
import com.near.opencv_convertor.filters.ImageFilter;
import com.near.opencv_convertor.filters.params.RGBShiftParams;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.imgproc.Imgproc;

public class RGBShiftFilter implements ImageFilter {
    @Override
    public Mat apply(Mat image, FilterParams params) {
        RGBShiftParams p = (RGBShiftParams) params;

        java.util.List<Mat> channels = new java.util.ArrayList<>();
        Core.split(image, channels);

        Mat r = shiftChannel(channels.get(2), p.getRedDx(), p.getRedDy());
        Mat g = shiftChannel(channels.get(1), p.getGreenDx(), p.getGreenDy());
        Mat b = shiftChannel(channels.get(0), p.getBlueDx(), p.getBlueDy());

        java.util.List<Mat> shifted = java.util.List.of(b, g, r);
        Mat output = new Mat();
        Core.merge(shifted, output);
        return output;
    }

    private Mat shiftChannel(Mat channel, int dx, int dy) {
        Mat shifted = new Mat();
        Mat translationMatrix = Mat.eye(2, 3, CvType.CV_32F);
        translationMatrix.put(0, 2, dx);
        translationMatrix.put(1, 2, dy);
        Imgproc.warpAffine(channel, shifted, translationMatrix, channel.size());
        return shifted;
    }
}
