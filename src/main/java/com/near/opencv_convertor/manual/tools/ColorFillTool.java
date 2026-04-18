package com.near.opencv_convertor.manual.tools;

import com.near.opencv_convertor.manual.ColorParser;
import com.near.opencv_convertor.manual.ImageManualTool;
import com.near.opencv_convertor.manual.ManualEditParams;
import com.near.opencv_convertor.manual.params.ColorFillParams;
import org.opencv.core.*;
import org.opencv.imgproc.Imgproc;

public class ColorFillTool implements ImageManualTool {

    @Override
    public Mat apply(Mat image, ManualEditParams params) {
        ColorFillParams fillParams = (ColorFillParams) params;

        if (image == null || image.empty()) {
            return image;
        }

        boolean hadAlpha = image.channels() == 4;

        Mat working = new Mat();

        if (image.channels() == 4) {
            Imgproc.cvtColor(image, working, Imgproc.COLOR_BGRA2BGR);
        } else if (image.channels() == 3) {
            image.copyTo(working);
        } else if (image.channels() == 1) {
            Imgproc.cvtColor(image, working, Imgproc.COLOR_GRAY2BGR);
        } else {
            throw new IllegalArgumentException("Unsupported image channels count: " + image.channels());
        }

        int x = Math.max(0, Math.min(fillParams.getX(), working.cols() - 1));
        int y = Math.max(0, Math.min(fillParams.getY(), working.rows() - 1));

        Scalar color = ColorParser.parseToBgr(fillParams.getColor());

        double tolerance = Math.max(0, fillParams.getTolerance());
        Scalar loDiff = new Scalar(tolerance, tolerance, tolerance);
        Scalar upDiff = new Scalar(tolerance, tolerance, tolerance);

        Mat mask = Mat.zeros(working.rows() + 2, working.cols() + 2, CvType.CV_8UC1);

        Imgproc.floodFill(
                working,
                mask,
                new Point(x, y),
                color,
                new Rect(),
                loDiff,
                upDiff,
                4
        );

        if (!hadAlpha) {
            return working;
        }

        Mat result = new Mat();
        Imgproc.cvtColor(working, result, Imgproc.COLOR_BGR2BGRA);
        return result;
    }

    @Override
    public boolean requiresAlpha() {
        return true;
    }
}