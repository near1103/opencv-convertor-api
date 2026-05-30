package com.near.opencv_convertor.gifprocessing;

import org.opencv.core.Mat;

@FunctionalInterface
public interface GifFrameProcessor {
    Mat process(Mat frame) throws Exception;
}