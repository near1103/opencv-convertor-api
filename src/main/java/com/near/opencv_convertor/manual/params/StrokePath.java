package com.near.opencv_convertor.manual.params;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
public class StrokePath {
    private List<StrokePoint> points = new ArrayList<>();
}