package com.near.opencv_convertor.manual.tools;

import com.near.opencv_convertor.manual.ImageManualTool;
import com.near.opencv_convertor.manual.ManualEditParams;
import com.near.opencv_convertor.manual.params.EraserParams;
import com.near.opencv_convertor.manual.params.StrokePoint;
import org.opencv.core.*;
import org.opencv.imgproc.Imgproc;

import java.util.List;

public class EraserTool implements ImageManualTool {

    @Override
    public Mat apply(Mat image, ManualEditParams params) {
        EraserParams eraserParams = (EraserParams) params;

        if (image == null || image.empty()) {
            return image;
        }

        Mat result = ensureBgra(image);

        List<StrokePoint> points = eraserParams.getPoints();
        if (points == null || points.size() < 2) {
            return result;
        }

        double opacity = clamp(eraserParams.getOpacity(), 0.0, 1.0);

        Mat mask = Mat.zeros(result.rows(), result.cols(), CvType.CV_8UC1);

        for (int i = 1; i < points.size(); i++) {
            StrokePoint prev = points.get(i - 1);
            StrokePoint curr = points.get(i);

            Imgproc.line(
                    mask,
                    new Point(prev.getX(), prev.getY()),
                    new Point(curr.getX(), curr.getY()),
                    new Scalar(255),
                    Math.max(1, eraserParams.getSize()),
                    Imgproc.LINE_AA
            );
        }

        for (int y = 0; y < result.rows(); y++) {
            for (int x = 0; x < result.cols(); x++) {
                double[] maskPixel = mask.get(y, x);
                if (maskPixel == null || maskPixel[0] <= 0) {
                    continue;
                }

                double eraseFactor = (maskPixel[0] / 255.0) * opacity;

                double[] src = result.get(y, x);
                if (src == null || src.length < 4) {
                    continue;
                }

                double newAlpha = src[3] * (1.0 - eraseFactor);
                result.put(y, x, src[0], src[1], src[2], newAlpha);
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

    private double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }
}