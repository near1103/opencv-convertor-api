package com.near.opencv_convertor.configs;

import org.opencv.core.Core;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Configuration;
import jakarta.annotation.PostConstruct;

@Configuration
public class OpenCVConfig {

    private static final Logger log = LoggerFactory.getLogger(OpenCVConfig.class);

    @PostConstruct
    public void init() {
        System.loadLibrary(Core.NATIVE_LIBRARY_NAME);
        log.info("OpenCV initialized, version={}", Core.VERSION);
    }
}
