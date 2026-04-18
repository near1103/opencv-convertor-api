package com.near.opencv_convertor.manual.params;

import com.near.opencv_convertor.manual.ManualEditParams;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ColorFillParams extends ManualEditParams {
    private int x;
    private int y;
    private String color;
    private int tolerance;
}
