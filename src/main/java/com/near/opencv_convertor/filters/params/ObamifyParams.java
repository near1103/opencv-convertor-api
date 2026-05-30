package com.near.opencv_convertor.filters.params;

import com.near.opencv_convertor.filters.FilterParams;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ObamifyParams extends FilterParams {

    private Integer resolution;
    private Integer proximityImportance;

    /**
     * Static preset name: obama
     * Temporary preset format: tmp:{presetId}
     */
    private String preset;

    /**
     * full | object | object_text
     */
    private String presetMode;

    /**
     * none | all | regions | polygons
     */
    private String priority;

    /**
     * Format: x,y,w,h;x,y,w,h
     */
    private String priorityRegions;

    /**
     * Format: x,y;x,y;x,y|x,y;x,y;x,y
     */
    private String priorityPolygons;
}