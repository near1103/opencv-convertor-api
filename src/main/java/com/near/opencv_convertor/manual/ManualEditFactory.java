package com.near.opencv_convertor.manual;

import com.near.opencv_convertor.manual.enums.ManualEditType;
import com.near.opencv_convertor.manual.tools.BrushTool;
import com.near.opencv_convertor.manual.tools.ColorFillTool;
import com.near.opencv_convertor.manual.tools.EraserTool;

public class ManualEditFactory {
    public static ImageManualTool create(ManualEditType type) {
        return switch (type) {
            case BRUSH -> new BrushTool();
            case ERASER -> new EraserTool();
            case COLOR_FILL -> new ColorFillTool();
        };
    }
}
