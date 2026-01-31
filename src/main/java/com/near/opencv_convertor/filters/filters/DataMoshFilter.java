package com.near.opencv_convertor.filters.filters;

import com.near.opencv_convertor.filters.FilterParams;
import com.near.opencv_convertor.filters.ImageFilter;
import com.near.opencv_convertor.filters.params.DataMoshParams;
import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.Rect;

import java.util.Random;

public class DataMoshFilter implements ImageFilter {

    private final Random random = new Random();

    @Override
    public Mat apply(Mat image, FilterParams params) {
        DataMoshParams p = (DataMoshParams) params;

        Mat output = image.clone();

        int w = image.cols();
        int h = image.rows();
        int bs = Math.max(4, p.getBlockSize());

        for (int y = 0; y < h; y += bs) {
            for (int x = 0; x < w; x += bs) {

                if (random.nextDouble() > p.getChaos())
                    continue;

                int offsetX = (int) (random.nextGaussian() * p.getMaxOffset());
                int offsetY = (int) (random.nextGaussian() * p.getMaxOffset());

                int extraWidth = random.nextInt(bs / 2);
                int extraHeight = random.nextInt(bs / 2);

                int srcX = clamp(x + offsetX, 0, w - 1);
                int srcY = clamp(y + offsetY, 0, h - 1);

                int srcWidth = Math.min(bs + extraWidth, w - srcX);
                int srcHeight = Math.min(bs + extraHeight, h - srcY);

                int dstWidth = Math.min(bs + extraWidth, w - x);
                int dstHeight = Math.min(bs + extraHeight, h - y);

                int blockWidth = Math.min(srcWidth, dstWidth);
                int blockHeight = Math.min(srcHeight, dstHeight);

                Rect srcRect = new Rect(srcX, srcY, blockWidth, blockHeight);
                Rect dstRect = new Rect(x, y, blockWidth, blockHeight);

                Mat srcBlock = image.submat(srcRect);
                Mat dstBlock = output.submat(dstRect);

                double rnd = random.nextDouble();

                if (rnd < p.getSmear()) {
                    Core.addWeighted(dstBlock, 0.3, srcBlock, 0.7, 0, dstBlock);
                } else if (rnd < p.getSmear() + 0.2) {
                    srcBlock.copyTo(dstBlock);
                    if (dstRect.x + blockWidth < w) {
                        Rect nextDst = new Rect(dstRect.x + blockWidth / 2, dstRect.y, blockWidth / 2, blockHeight);
                        Rect nextSrc = new Rect(srcRect.x, srcRect.y, blockWidth / 2, blockHeight);
                        image.submat(nextSrc).copyTo(output.submat(nextDst));
                    }
                } else {
                    srcBlock.copyTo(dstBlock);
                }
            }
        }

        return output;
    }

    private int clamp(int v, int min, int max) {
        return Math.max(min, Math.min(max, v));
    }
}

