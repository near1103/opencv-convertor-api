package com.near.opencv_convertor.manual.params;

import com.near.opencv_convertor.manual.ManualEditParams;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
public class BrushParams extends ManualEditParams {
    private String color;
    private int size;
    private double opacity;
    private List<StrokePoint> points = new ArrayList<>();
}