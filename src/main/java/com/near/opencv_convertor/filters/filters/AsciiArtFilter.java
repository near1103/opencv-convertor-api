package com.near.opencv_convertor.filters.filters;

import com.near.opencv_convertor.filters.FilterParams;
import com.near.opencv_convertor.filters.ImageFilter;
import com.near.opencv_convertor.filters.params.AsciiArtParams;
import org.opencv.core.*;
import org.opencv.imgproc.Imgproc;

public class AsciiArtFilter implements ImageFilter {

    @Override
    public Mat apply(Mat image, FilterParams params) {
        AsciiArtParams p = (AsciiArtParams) params;

        Mat gray = new Mat();
        Imgproc.cvtColor(image, gray, Imgproc.COLOR_BGR2GRAY);

        Mat output = Mat.zeros(image.size(), CvType.CV_8UC3);

        int bs = Math.max(4, p.getBlockSize());

        int fontFace = Imgproc.FONT_HERSHEY_SIMPLEX;
        double fontScale = bs / 12.0;
        int thickness = 1;

        Size textSize = Imgproc.getTextSize(
                "W",
                fontFace,
                fontScale,
                thickness,
                null
        );

        int charW = (int) textSize.width + 1;
        int charH = (int) textSize.height + 2;

        char[] gradient = p.getGradient().toCharArray();

        for (int y = 0; y < gray.rows(); y += charH) {
            for (int x = 0; x < gray.cols(); x += charW) {

                int w = Math.min(charW, gray.cols() - x);
                int h = Math.min(charH, gray.rows() - y);

                Rect r = new Rect(x, y, w, h);
                Mat block = gray.submat(r);

                MatOfDouble mean = new MatOfDouble();
                MatOfDouble std = new MatOfDouble();
                Core.meanStdDev(block, mean, std);

                double brightness = mean.toArray()[0];
                double contrast = std.toArray()[0];

                brightness -= contrast * 0.4;

                if (p.isInvert()) {
                    brightness = 255 - brightness;
                }

                brightness = Math.max(0, Math.min(255, brightness));

                int index = (int) Math.round(
                        brightness * (gradient.length - 1) / 255.0
                );

                index = Math.max(0, Math.min(gradient.length - 1, index));

                char c = gradient[index];

                Imgproc.putText(
                        output,
                        String.valueOf(c),
                        new Point(x, y + charH - 2),
                        fontFace,
                        fontScale,
                        new Scalar(255, 255, 255),
                        thickness,
                        Imgproc.LINE_AA
                );
            }
        }

        return output;
    }
}


