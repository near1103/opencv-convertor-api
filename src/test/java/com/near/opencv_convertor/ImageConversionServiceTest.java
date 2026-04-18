package com.near.opencv_convertor;

import com.near.opencv_convertor.converters.enums.SupportedImageFormat;
import com.near.opencv_convertor.converters.services.ImageConversionService;
import com.near.opencv_convertor.converters.services.ImageFormatConverter;
import com.near.opencv_convertor.converters.services.ImageIOService;
import com.near.opencv_convertor.converters.services.ImageProcessor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ImageConversionServiceTest {

    @Mock
    private ImageFormatConverter formatConverter;
    @Mock
    private ImageProcessor imageProcessor;
    @Mock
    private ImageIOService imageIOService;

    private ImageConversionService conversionService;

    @BeforeEach
    void setUp() {
        conversionService = new ImageConversionService(formatConverter, imageProcessor, imageIOService);
    }

    @Test
    void testSuccessfulConversion() throws Exception {
        File inputFile = createTempImageFile("image.png");
        SupportedImageFormat targetFormat = SupportedImageFormat.JPG;
        BufferedImage image = new BufferedImage(100, 100, BufferedImage.TYPE_INT_ARGB);
        File expectedOutput = new File("output.jpg");

        try (MockedStatic<ImageIO> mockedIO = Mockito.mockStatic(ImageIO.class)) {
            mockedIO.when(() -> ImageIO.read(inputFile)).thenReturn(image);
            when(imageProcessor.removeAlphaChannel(image, Color.WHITE)).thenReturn(image);
            when(imageIOService.writeImage(image, "jpg")).thenReturn(expectedOutput);

            File result = conversionService.convert(inputFile, targetFormat);

            assertEquals(expectedOutput, result);
        }
    }

    @Test
    void testUnsupportedInputFormat() {
        File inputFile = new File("image.avif");
        SupportedImageFormat targetFormat = SupportedImageFormat.JPG;

        IOException exception = assertThrows(IOException.class, () -> conversionService.convert(inputFile, targetFormat));

        assertTrue(exception.getMessage().contains("Unsupported input format"));
    }

    @Test
    void testUnsupportedOutputFormat() {
        File inputFile = createTempImageFile("image.jpg");

        SupportedImageFormat unsupportedFormat = mock(SupportedImageFormat.class);
        when(unsupportedFormat.getExtension()).thenReturn("psd");

        IOException exception = assertThrows(IOException.class, () -> conversionService.convert(inputFile, unsupportedFormat));

        assertTrue(exception.getMessage().contains("Unsupported output format"));
    }

    @Test
    void testCorruptedImageFile() {
        File inputFile = createTempImageFile("image.png");
        SupportedImageFormat targetFormat = SupportedImageFormat.JPG;

        try (MockedStatic<ImageIO> mockedIO = Mockito.mockStatic(ImageIO.class)) {
            mockedIO.when(() -> ImageIO.read(inputFile)).thenReturn(null);

            IOException exception = assertThrows(IOException.class, () -> conversionService.convert(inputFile, targetFormat));

            assertEquals("Can't read image file", exception.getMessage());
        }
    }

    private File createTempImageFile(String nameWithExt) {
        try {
            String prefix = nameWithExt.substring(0, nameWithExt.lastIndexOf('.'));
            String suffix = nameWithExt.substring(nameWithExt.lastIndexOf('.'));
            File file = File.createTempFile(prefix, suffix);
            file.deleteOnExit();
            return file;
        } catch (IOException e) {
            throw new RuntimeException("Can't create temp file", e);
        }
    }
}
