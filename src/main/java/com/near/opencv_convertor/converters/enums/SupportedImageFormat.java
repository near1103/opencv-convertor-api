package com.near.opencv_convertor.converters.enums;

import lombok.Getter;
import org.springframework.http.MediaType;

import java.util.Arrays;
import java.util.Optional;

@Getter
public enum SupportedImageFormat {
    JPG("jpg", MediaType.IMAGE_JPEG),
    JPEG("jpeg", MediaType.IMAGE_JPEG),
    PNG("png", MediaType.IMAGE_PNG),
    GIF("gif", MediaType.IMAGE_PNG),
    BMP("bmp", MediaType.valueOf("image/bmp")),
    TIF("tif", MediaType.valueOf("image/tiff")),
    TIFF("tiff", MediaType.valueOf("image/tiff")),
    WEBP("webp", MediaType.valueOf("image/webp")),
    ICO("ico", MediaType.valueOf("image/x-icon")),
    PNM("pnm", MediaType.valueOf("image/x-portable-anymap")),
    TGA("tga", MediaType.valueOf("image/x-tga"));

    private final String extension;
    private final MediaType mediaType;

    SupportedImageFormat(String extension, MediaType mediaType) {
        this.extension = extension;
        this.mediaType = mediaType;
    }

    public boolean supportsAlpha() {
        return switch (this) {
            case PNG, WEBP, ICO, TIFF, TIF -> true;
            default -> false;
        };
    }

    public static Optional<SupportedImageFormat> fromExtension(String ext) {
        return Arrays.stream(values())
                .filter(f -> f.extension.equalsIgnoreCase(ext))
                .findFirst();
    }

    public static boolean isSupported(String ext) {
        return Arrays.stream(values())
                .anyMatch(f -> f.getExtension().equalsIgnoreCase(ext));
    }
}