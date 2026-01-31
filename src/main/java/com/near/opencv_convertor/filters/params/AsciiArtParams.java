package com.near.opencv_convertor.filters.params;

import com.near.opencv_convertor.filters.FilterParams;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AsciiArtParams extends FilterParams {
    private int blockSize = 8;
    private String gradient = " .:-=+*#%@";
    private boolean invert = false;
}