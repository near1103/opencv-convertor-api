package com.near.opencv_convertor.converters;

import org.springframework.stereotype.Service;

import java.awt.*;
import java.awt.image.BufferedImage;

@Service
public class ImageProcessor {

    public BufferedImage cropToSquareTopLeft(BufferedImage src) {
        int size = Math.min(src.getWidth(), src.getHeight());
        return src.getSubimage(0, 0, size, size);
    }

    public BufferedImage resizeIfNeeded(BufferedImage originalImage, int maxWidth, int maxHeight) {
        int width = originalImage.getWidth();
        int height = originalImage.getHeight();

        if (width <= maxWidth && height <= maxHeight) {
            return originalImage;
        }

        double scale = Math.min((double) maxWidth / width, (double) maxHeight / height);
        int newWidth = (int) (width * scale);
        int newHeight = (int) (height * scale);

        Image tmp = originalImage.getScaledInstance(newWidth, newHeight, Image.SCALE_SMOOTH);
        BufferedImage resized = new BufferedImage(newWidth, newHeight, BufferedImage.TYPE_INT_ARGB);

        Graphics2D g2d = resized.createGraphics();
        g2d.drawImage(tmp, 0, 0, null);
        g2d.dispose();

        return resized;
    }

    public BufferedImage convertToABGR(BufferedImage src) {
        if (src.getType() == BufferedImage.TYPE_4BYTE_ABGR) {
            return src;
        }
        int width = src.getWidth();
        int height = src.getHeight();

        BufferedImage abgrImage = new BufferedImage(width, height, BufferedImage.TYPE_4BYTE_ABGR);

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int argb = src.getRGB(x, y);

                int alpha = (argb >> 24) & 0xff;
                int red   = (argb >> 16) & 0xff;
                int green = (argb >> 8) & 0xff;
                int blue  = (argb) & 0xff;

                int abgr = (alpha << 24) | (blue << 16) | (green << 8) | red;
                abgrImage.setRGB(x, y, abgr);
            }
        }

        return abgrImage;
    }
}
