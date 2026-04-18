package com.near.opencv_convertor.manual.service;

import org.opencv.core.Core;
import org.opencv.core.Mat;

public class ManualOverlayService {
    public Mat blend(Mat base, Mat overlay, double alpha) {
        Mat result = new Mat();
        Core.addWeighted(overlay, alpha, base, 1.0 - alpha, 0.0, result);
        return result;
    }
}