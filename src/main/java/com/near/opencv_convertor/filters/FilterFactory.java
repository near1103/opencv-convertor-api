package com.near.opencv_convertor.filters;

import com.near.opencv_convertor.filters.filters.AsciiArtFilter;
import com.near.opencv_convertor.filters.filters.BlurFilter;
import com.near.opencv_convertor.filters.filters.BrightnessFilter;
import com.near.opencv_convertor.filters.filters.ChromaticAberrationFilter;
import com.near.opencv_convertor.filters.filters.ColorOverlayFilter;
import com.near.opencv_convertor.filters.filters.ContrastFilter;
import com.near.opencv_convertor.filters.filters.DataMoshFilter;
import com.near.opencv_convertor.filters.filters.EdgeDetectionFilter;
import com.near.opencv_convertor.filters.filters.GaussianBlurFilter;
import com.near.opencv_convertor.filters.filters.GrayscaleFilter;
import com.near.opencv_convertor.filters.filters.NegativeFilter;
import com.near.opencv_convertor.filters.filters.NoiseFilter;
import com.near.opencv_convertor.filters.filters.ObamifyFilter;
import com.near.opencv_convertor.filters.filters.PixelateFilter;
import com.near.opencv_convertor.filters.filters.RGBShiftFilter;
import com.near.opencv_convertor.filters.filters.RemoveBackgroundFilter;
import com.near.opencv_convertor.filters.services.ObamifyTempPresetService;
import org.springframework.stereotype.Component;

@Component
public class FilterFactory {

    private final ObamifyTempPresetService obamifyTempPresetService;

    public FilterFactory(ObamifyTempPresetService obamifyTempPresetService) {
        this.obamifyTempPresetService = obamifyTempPresetService;
    }

    public ImageFilter create(FilterType type) {
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
            case GAUSSIAN_BLUR -> new GaussianBlurFilter();
            case CONTRAST -> new ContrastFilter();
            case CHROMATIC_ABERRATION -> new ChromaticAberrationFilter();
            case DATA_MOSH -> new DataMoshFilter();
            case ASCII_ART -> new AsciiArtFilter();
            case OBAMIFY -> new ObamifyFilter(obamifyTempPresetService);
        };
    }
}