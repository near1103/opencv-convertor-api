package com.near.opencv_convertor.manual;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.near.opencv_convertor.manual.enums.ManualEditType;
import com.near.opencv_convertor.manual.params.BrushParams;
import com.near.opencv_convertor.manual.params.ColorFillParams;
import com.near.opencv_convertor.manual.params.EraserParams;
import com.near.opencv_convertor.manual.params.StrokePoint;

import java.util.Collections;
import java.util.List;
import java.util.Map;

public class ManualEditParamsFactory {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private ManualEditParamsFactory() {
    }

    public static ManualEditParams create(ManualEditType type, Map<String, String> params) {
        return switch (type) {
            case BRUSH -> createBrushParams(params);
            case ERASER -> createEraserParams(params);
            case COLOR_FILL -> createColorFillParams(params);
        };
    }

    private static BrushParams createBrushParams(Map<String, String> params) {
        BrushParams brushParams = new BrushParams();
        brushParams.setColor(params.getOrDefault("color", "#000000"));
        brushParams.setSize(parseInt(params.get("size"), 5));
        brushParams.setOpacity(parseDouble(params.get("opacity"), 1.0));
        brushParams.setPoints(parsePoints(params.get("points")));
        return brushParams;
    }

    private static EraserParams createEraserParams(Map<String, String> params) {
        EraserParams eraserParams = new EraserParams();
        eraserParams.setSize(parseInt(params.get("size"), 10));
        eraserParams.setOpacity(parseDouble(params.get("opacity"), 1.0));
        eraserParams.setPoints(parsePoints(params.get("points")));
        return eraserParams;
    }

    private static ColorFillParams createColorFillParams(Map<String, String> params) {
        ColorFillParams colorFillParams = new ColorFillParams();
        colorFillParams.setX(parseInt(params.get("x"), 0));
        colorFillParams.setY(parseInt(params.get("y"), 0));
        colorFillParams.setColor(params.getOrDefault("color", "#000000"));
        colorFillParams.setTolerance(parseInt(params.get("tolerance"), 20));
        return colorFillParams;
    }

    private static List<StrokePoint> parsePoints(String rawPoints) {
        if (rawPoints == null || rawPoints.isBlank()) {
            return Collections.emptyList();
        }

        try {
            return OBJECT_MAPPER.readValue(rawPoints, new TypeReference<List<StrokePoint>>() {});
        } catch (Exception e) {
            return Collections.emptyList();
        }
    }

    private static int parseInt(String value, int defaultValue) {
        try {
            return value == null ? defaultValue : Integer.parseInt(value);
        } catch (Exception e) {
            return defaultValue;
        }
    }

    private static double parseDouble(String value, double defaultValue) {
        try {
            return value == null ? defaultValue : Double.parseDouble(value);
        } catch (Exception e) {
            return defaultValue;
        }
    }
}