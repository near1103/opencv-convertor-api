package com.near.opencv_convertor.transformations;

import com.near.opencv_convertor.transformations.params.CropParams;
import com.near.opencv_convertor.transformations.params.FlipMode;
import com.near.opencv_convertor.transformations.params.FlipParams;
import com.near.opencv_convertor.transformations.params.ResizeMethod;
import com.near.opencv_convertor.transformations.params.ResizeParams;
import com.near.opencv_convertor.transformations.params.RotateParams;

import java.util.Map;

public class TransformationParamsFactory {

    private TransformationParamsFactory() {
    }

    public static TransformationParams create(TransformationType type, Map<String, String> params) {
        return switch (type) {
            case ROTATE -> buildRotateParams(params);
            case FLIP -> buildFlipParams(params);
            case CROP -> buildCropParams(params);
            case RESIZE -> buildResizeParams(params);
        };
    }

    private static RotateParams buildRotateParams(Map<String, String> params) {
        double angle = parseDouble(params, "angle", 0.0);
        return new RotateParams(angle);
    }

    private static FlipParams buildFlipParams(Map<String, String> params) {
        String rawMode = params.getOrDefault("mode", "HORIZONTAL");
        FlipMode mode = FlipMode.valueOf(rawMode.toUpperCase());
        return new FlipParams(mode);
    }

    private static CropParams buildCropParams(Map<String, String> params) {
        int x = parseIntRequired(params, "x");
        int y = parseIntRequired(params, "y");
        int width = parseIntRequired(params, "width");
        int height = parseIntRequired(params, "height");

        return new CropParams(x, y, width, height);
    }

    private static ResizeParams buildResizeParams(Map<String, String> params) {
        int width = parseIntRequired(params, "width");
        int height = parseIntRequired(params, "height");
        boolean keepAspect = parseBoolean(params, "keepAspect", true);

        String rawMethod = params.getOrDefault("method", "BILINEAR");
        ResizeMethod method = ResizeMethod.valueOf(rawMethod.toUpperCase());

        return new ResizeParams(width, height, keepAspect, method);
    }

    private static int parseIntRequired(Map<String, String> params, String key) {
        String value = params.get(key);
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Missing required parameter: " + key);
        }
        return Integer.parseInt(value);
    }

    private static double parseDouble(Map<String, String> params, String key, double defaultValue) {
        String value = params.get(key);
        if (value == null || value.isBlank()) {
            return defaultValue;
        }
        return Double.parseDouble(value);
    }

    private static boolean parseBoolean(Map<String, String> params, String key, boolean defaultValue) {
        String value = params.get(key);
        if (value == null || value.isBlank()) {
            return defaultValue;
        }
        return Boolean.parseBoolean(value);
    }
}
