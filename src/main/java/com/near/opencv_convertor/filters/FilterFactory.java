package com.near.opencv_convertor.filters;

import com.near.opencv_convertor.filters.filters.*;

public class FilterFactory {

    private FilterFactory() {}

    public static ImageFilter create(FilterType type) {
        return switch (type) {
            case PIXELATE -> new PixelateFilter();
            case BACKGROUND -> new RemoveBackgroundFilter();
            case RGB_SHIFT -> new RGBShiftFilter();
            case EDGE_DETECTION -> new EdgeDetectionFilter();
            case COLOR_OVERLAY -> new ColorOverlayFilter();
            case GRAYSCALE -> new GrayscaleFilter();
            case NEGATIVE -> new NegativeFilter();
            case NOISE -> new NoiseFilter();
            case BRIGHTNESS -> new BrightnessFilter();
            case BLUR -> new BlurFilter();
            case GAUSSIAN_BLUR ->  new GaussianBlurFilter();
            case CONTRAST -> new ContrastFilter();
        };
    }
}
