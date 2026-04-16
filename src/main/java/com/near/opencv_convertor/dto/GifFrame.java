package com.near.opencv_convertor.dto;

import java.awt.image.BufferedImage;

public class GifFrame {
    public BufferedImage image;
    public int delay;

    public GifFrame(BufferedImage image, int delay) {
        this.image = image;
        this.delay = delay;
    }
}