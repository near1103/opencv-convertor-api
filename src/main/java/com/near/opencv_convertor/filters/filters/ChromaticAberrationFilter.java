package com.near.opencv_convertor.filters.filters;

import com.near.opencv_convertor.filters.FilterParams;
import com.near.opencv_convertor.filters.ImageFilter;
import com.near.opencv_convertor.filters.params.ChromaticAberrationParams;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.imgproc.Imgproc;

import java.util.ArrayList;
import java.util.List;

public class ChromaticAberrationFilter implements ImageFilter {

    @Override
    public Mat apply(Mat image, FilterParams params) {
        ChromaticAberrationParams p = (ChromaticAberrationParams) params;

        int width = image.cols();
        int height = image.rows();

        double cx = width / 2.0;
        double cy = height / 2.0;
        double maxRadius = Math.sqrt(cx * cx + cy * cy);

        List<Mat> channels = new ArrayList<>();
        Core.split(image, channels);

        Mat red   = new Mat();
        Mat green = new Mat();
        Mat blue  = new Mat();

        warpRadial(channels.get(2), red,   p.getRadialStrength() * p.getRedStrength(),   cx, cy, maxRadius);
        warpRadial(channels.get(1), green, p.getRadialStrength() * p.getGreenStrength(), cx, cy, maxRadius);
        warpRadial(channels.get(0), blue,  p.getRadialStrength() * p.getBlueStrength(),  cx, cy, maxRadius);

        Mat output = new Mat();
        Core.merge(List.of(blue, green, red), output);
        return output;
    }

    private void warpRadial(Mat src, Mat dst, double strength, double cx, double cy, double maxRadius) {
        int w = src.cols();
        int h = src.rows();

        Mat mapX = new Mat(h, w, CvType.CV_32F);
        Mat mapY = new Mat(h, w, CvType.CV_32F);

        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                double dx = x - cx;
                double dy = y - cy;

                double r = Math.sqrt(dx * dx + dy * dy) / maxRadius;

                double shift = strength * r * r;

                double nx = x + (dx / maxRadius) * shift;
                double ny = y + (dy / maxRadius) * shift;

                mapX.put(y, x, nx);
                mapY.put(y, x, ny);
            }
        }

        Imgproc.remap(src, dst, mapX, mapY, Imgproc.INTER_LINEAR, Core.BORDER_REFLECT);
    }
}
