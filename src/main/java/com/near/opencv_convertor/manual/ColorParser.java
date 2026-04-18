package com.near.opencv_convertor.manual;

import org.opencv.core.Scalar;

public class ColorParser {

    private ColorParser() {
    }

    public static Scalar parseToBgr(String color) {
        int[] rgb = parseHex(color);
        return new Scalar(rgb[2], rgb[1], rgb[0]);
    }

    public static Scalar parseToBgra(String color, double alpha255) {
        int[] rgb = parseHex(color);
        return new Scalar(rgb[2], rgb[1], rgb[0], alpha255);
    }

    private static int[] parseHex(String color) {
        if (color == null || color.isBlank()) {
            return new int[]{0, 0, 0};
        }

        String normalized = color.startsWith("#") ? color.substring(1) : color;

        if (normalized.length() != 6) {
            return new int[]{0, 0, 0};
        }

        try {
            int r = Integer.parseInt(normalized.substring(0, 2), 16);
            int g = Integer.parseInt(normalized.substring(2, 4), 16);
            int b = Integer.parseInt(normalized.substring(4, 6), 16);
            return new int[]{r, g, b};
        } catch (Exception e) {
            return new int[]{0, 0, 0};
        }
    }
}