package com.near.opencv_convertor;

import org.opencv.core.Core;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;

@Component
public class OpenCVInitializer {

    @PostConstruct
    public void init() {
        System.loadLibrary(Core.NATIVE_LIBRARY_NAME);
        System.out.println("OpenCV version: " + Core.VERSION);
    }
}
