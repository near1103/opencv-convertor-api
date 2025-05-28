package com.near.opencv_convertor.filters.filters;

import com.near.opencv_convertor.filters.FilterParams;
import com.near.opencv_convertor.filters.ImageFilter;
import com.near.opencv_convertor.filters.params.PixelateParams;
import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.imgproc.Imgproc;

public class PixelateFilter implements ImageFilter {
    @Override
    public Mat apply(Mat image, FilterParams params) {
        PixelateParams p = (PixelateParams) params;
        int blockSize = Math.max(p.getBlockSize(), 1);

        Mat result = image.clone();
        int rows = image.rows();
        int cols = image.cols();

        for (int y = 0; y < rows; y += blockSize) {
            for (int x = 0; x < cols; x += blockSize) {
                int blockWidth = Math.min(blockSize, cols - x);
                int blockHeight = Math.min(blockSize, rows - y);

                Rect rect = new Rect(x, y, blockWidth, blockHeight);
                Mat block = result.submat(rect);

                Scalar avgColor = Core.mean(block);
                Imgproc.rectangle(result, rect, avgColor, Core.FILLED);
            }
        }

        return result;
    }
}
