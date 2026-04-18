package com.near.opencv_convertor.manual;

import com.near.opencv_convertor.manual.params.StrokePoint;

import java.util.ArrayList;
import java.util.List;

public class CoordinateMapper {

    private CoordinateMapper() {
    }

    public static List<StrokePoint> scalePoints(
            List<StrokePoint> points,
            double scaleX,
            double scaleY
    ) {
        List<StrokePoint> result = new ArrayList<>();

        for (StrokePoint point : points) {
            result.add(new StrokePoint(
                    (int) Math.round(point.getX() * scaleX),
                    (int) Math.round(point.getY() * scaleY)
            ));
        }

        return result;
    }
}
