package com.near.opencv_convertor.util;

import org.bytedeco.javacpp.BytePointer;
import org.bytedeco.opencv.global.opencv_imgcodecs;
import org.bytedeco.opencv.opencv_core.Mat;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;

public final class OpenCvImageUtils {

    private OpenCvImageUtils() {
    }

    public static BufferedImage matToBufferedImage(Mat mat) throws IOException {
        if (mat == null || mat.empty()) {
            throw new IOException("Cannot convert empty OpenCV Mat to BufferedImage");
        }

        BytePointer buffer = new BytePointer();

        try {
            boolean encoded = opencv_imgcodecs.imencode(".png", mat, buffer);

            if (!encoded || buffer.limit() <= 0) {
                throw new IOException("Failed to encode OpenCV Mat to PNG");
            }

            byte[] bytes = new byte[(int) buffer.limit()];
            buffer.get(bytes);

            BufferedImage image = ImageIO.read(new ByteArrayInputStream(bytes));

            if (image == null) {
                throw new IOException("ImageIO failed to read encoded PNG from OpenCV Mat");
            }

            return normalizeImageType(image);
        } finally {
            buffer.close();
        }
    }

    private static BufferedImage normalizeImageType(BufferedImage source) {
        if (source.getType() == BufferedImage.TYPE_INT_ARGB) {
            return source;
        }

        BufferedImage converted = new BufferedImage(
                source.getWidth(),
                source.getHeight(),
                BufferedImage.TYPE_INT_ARGB
        );

        Graphics2D graphics = converted.createGraphics();
        try {
            graphics.drawImage(source, 0, 0, null);
        } finally {
            graphics.dispose();
        }

        return converted;
    }
}