package com.near.opencv_convertor.filters;

import com.near.opencv_convertor.filters.params.*;

import java.util.Map;

public class FilterParamsFactory {

    private FilterParamsFactory() {}

    public static FilterParams create(FilterType type, Map<String, String> params) {
        return switch (type) {
            case RGB_SHIFT -> {
                RGBShiftParams rgb = new RGBShiftParams();
                rgb.setRedDx(Integer.parseInt(params.getOrDefault("redDx", "0")));
                rgb.setRedDy(Integer.parseInt(params.getOrDefault("redDy", "0")));
                rgb.setGreenDx(Integer.parseInt(params.getOrDefault("greenDx", "0")));
                rgb.setGreenDy(Integer.parseInt(params.getOrDefault("greenDy", "0")));
                rgb.setBlueDx(Integer.parseInt(params.getOrDefault("blueDx", "0")));
                rgb.setBlueDy(Integer.parseInt(params.getOrDefault("blueDy", "0")));
                yield rgb;
            }
            case PIXELATE -> {
                PixelateParams pixelate = new PixelateParams();
                pixelate.setBlockSize(Integer.parseInt(params.getOrDefault("blockSize", "10")));
                yield pixelate;
            }
            case BACKGROUND -> {
                RemoveBackgroundParams bg = new RemoveBackgroundParams();
                bg.setRed(Integer.parseInt(params.getOrDefault("red", "255")));
                bg.setGreen(Integer.parseInt(params.getOrDefault("green", "255")));
                bg.setBlue(Integer.parseInt(params.getOrDefault("blue", "255")));
                bg.setThreshold(Double.parseDouble(params.getOrDefault("threshold", "60.0")));
                yield bg;
            }
            case EDGE_DETECTION -> {
                EdgeDetectionParams ed = new EdgeDetectionParams();
                ed.setThreshold1(Double.parseDouble(params.getOrDefault("threshold1", "0.0")));
                ed.setThreshold2(Double.parseDouble(params.getOrDefault("threshold2", "0.0")));
                yield ed;
            }
            case COLOR_OVERLAY -> {
                ColorOverlayParams cop = new ColorOverlayParams();
                cop.setRed(Integer.parseInt(params.getOrDefault("red", "0")));
                cop.setGreen(Integer.parseInt(params.getOrDefault("green", "0")));
                cop.setBlue(Integer.parseInt(params.getOrDefault("blue", "0")));
                cop.setAlpha(Double.parseDouble(params.getOrDefault("alpha", "0.5")));
                yield cop;
            }
            case NOISE -> {
                NoiseParams np = new NoiseParams();
                np.setMean(Double.parseDouble(params.getOrDefault("mean", "0")));
                np.setStddev(Double.parseDouble(params.getOrDefault("stddev", "10")));
                yield np;
            }
            case BRIGHTNESS -> {
                BrightnessParams bp = new BrightnessParams();
                bp.setBrightness(Integer.parseInt(params.getOrDefault("brightness", "0")));
                yield bp;
            }
            case BLUR -> {
                BlurParams bp = new BlurParams();
                bp.setKernelSize(Integer.parseInt(params.getOrDefault("kernelSize", "3")));
                yield bp;
            }
            case GAUSSIAN_BLUR -> {
                GaussianBlurParams gbParams = new GaussianBlurParams();
                gbParams.setKernelSize(Integer.parseInt(params.getOrDefault("kernelSize", "5")));
                yield gbParams;
            }
            case CONTRAST -> {
                ContrastParams cp = new ContrastParams();
                cp.setAlpha(Double.parseDouble(params.getOrDefault("alpha", "1.0")));
                yield cp;
            }
            case CHROMATIC_ABERRATION -> {
                ChromaticAberrationParams cap = new ChromaticAberrationParams();
                cap.setBlueStrength(Double.parseDouble(params.getOrDefault("blueStrength", "1.0")));
                cap.setRedStrength(Double.parseDouble(params.getOrDefault("redStrength", "1.0")));
                cap.setGreenStrength(Double.parseDouble(params.getOrDefault("greenStrength", "1.0")));
                cap.setRadialStrength(Double.parseDouble(params.getOrDefault("radialStrength", "5.0")));
                yield cap;
            }
            case DATA_MOSH -> {
                DataMoshParams dmp = new DataMoshParams();
                dmp.setBlockSize(Integer.parseInt(params.getOrDefault("blockSize", "10")));
                dmp.setMaxOffset(Integer.parseInt(params.getOrDefault("maxOffset", "10")));
                dmp.setChaos(Double.parseDouble(params.getOrDefault("chaos", "0.3")));
                dmp.setSmear(Double.parseDouble(params.getOrDefault("smear", "0.7")));
                yield dmp;
            }
            case ASCII_ART -> {
                AsciiArtParams aap = new AsciiArtParams();
                aap.setBlockSize(Integer.parseInt(params.getOrDefault("blockSize", "10")));
                aap.setGradient(params.getOrDefault("gradient", " .:-=+*#%@"));
                aap.setInvert(Boolean.parseBoolean(params.getOrDefault("invert", "false")));
                yield aap;
            }
            case GRAYSCALE, NEGATIVE -> new EmptyParams();

        };
    }
}