package com.near.opencv_convertor.manual.tools;

import com.near.opencv_convertor.manual.ImageManualTool;
import com.near.opencv_convertor.manual.ManualEditParams;
import com.near.opencv_convertor.manual.params.BrushParams;
import com.near.opencv_convertor.manual.params.StrokePoint;
import org.opencv.core.*;
import org.opencv.imgproc.Imgproc;

import java.util.List;

public class BrushTool implements ImageManualTool {

    @Override
    public Mat apply(Mat image, ManualEditParams params) {
        BrushParams brushParams = (BrushParams) params;

        if (image == null || image.empty()) {
            return image;
        }

        Mat base = ensureBgra(image);

        List<StrokePoint> points = brushParams.getPoints();
        if (points == null || points.size() < 2) {
            return base;
        }

        double opacity = clamp(brushParams.getOpacity(), 0.0, 1.0);

        int[] rgb = parseHexColor(brushParams.getColor());
        double blue = rgb[2];
        double green = rgb[1];
        double red = rgb[0];

        Mat mask = Mat.zeros(base.rows(), base.cols(), CvType.CV_8UC1);

        for (int i = 1; i < points.size(); i++) {
            StrokePoint prev = points.get(i - 1);
            StrokePoint curr = points.get(i);

            Imgproc.line(
                    mask,
                    new Point(prev.getX(), prev.getY()),
                    new Point(curr.getX(), curr.getY()),
                    new Scalar(255),
                    Math.max(1, brushParams.getSize()),
                    Imgproc.LINE_AA
            );
        }

        Mat result = base.clone();

        for (int y = 0; y < result.rows(); y++) {
            for (int x = 0; x < result.cols(); x++) {
                double[] maskPixel = mask.get(y, x);
                if (maskPixel == null || maskPixel[0] <= 0) {
                    continue;
                }

                double localOpacity = (maskPixel[0] / 255.0) * opacity;
                double[] src = result.get(y, x);
                if (src == null || src.length < 4) {
                    continue;
                }

                double newB = src[0] * (1.0 - localOpacity) + blue * localOpacity;
                double newG = src[1] * (1.0 - localOpacity) + green * localOpacity;
                double newR = src[2] * (1.0 - localOpacity) + red * localOpacity;
                double newA = src[3];

                result.put(y, x, newB, newG, newR, newA);
            }
        }

        return result;
    }

    @Override
    public boolean requiresAlpha() {
        return true;
    }

    private Mat ensureBgra(Mat image) {
        Mat result = new Mat();

        if (image.channels() == 4) {
            image.copyTo(result);
        } else if (image.channels() == 3) {
            Imgproc.cvtColor(image, result, Imgproc.COLOR_BGR2BGRA);
        } else if (image.channels() == 1) {
            Imgproc.cvtColor(image, result, Imgproc.COLOR_GRAY2BGRA);
        } else {
            image.copyTo(result);
        }

        return result;
    }

    private int[] parseHexColor(String color) {
        if (color == null || color.isBlank()) {
            return new int[]{0, 0, 0};
        }

        String normalized = color.startsWith("#") ? color.substring(1) : color;
        if (normalized.length() != 6) {
            return new int[]{0, 0, 0};
        }

        try {
            int r = Integer.parseInt(normalized.substring(0, 2), 16);
            int g = Integer.parseInt(normalized.substring(2, 4), 16);
            int b = Integer.parseInt(normalized.substring(4, 6), 16);
            return new int[]{r, g, b};
        } catch (Exception e) {
            return new int[]{0, 0, 0};
        }
    }

    private double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }
}