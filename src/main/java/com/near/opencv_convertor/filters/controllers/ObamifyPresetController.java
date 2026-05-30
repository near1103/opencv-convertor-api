package com.near.opencv_convertor.filters.controllers;

import com.near.opencv_convertor.dto.ObamifyPresetDto;
import com.near.opencv_convertor.dto.ObamifyTempPresetDto;
import com.near.opencv_convertor.filters.services.ObamifyPresetService;
import com.near.opencv_convertor.filters.services.ObamifyTempPresetService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/obamify")
public class ObamifyPresetController {

    private final ObamifyPresetService obamifyPresetService;
    private final ObamifyTempPresetService obamifyTempPresetService;

    public ObamifyPresetController(
            ObamifyPresetService obamifyPresetService,
            ObamifyTempPresetService obamifyTempPresetService
    ) {
        this.obamifyPresetService = obamifyPresetService;
        this.obamifyTempPresetService = obamifyTempPresetService;
    }

    @GetMapping("/presets")
    public List<ObamifyPresetDto> getPresets() {
        return obamifyPresetService.getPresets();
    }

    @PostMapping("/temp-preset")
    public ObamifyTempPresetDto createTempPreset(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "mode", required = false, defaultValue = "object_text") String mode,
            @RequestParam(value = "priority", required = false, defaultValue = "none") String priority,
            @RequestParam(value = "priorityRegions", required = false, defaultValue = "") String priorityRegions,
            @RequestParam(value = "priorityPolygons", required = false, defaultValue = "") String priorityPolygons,
            @RequestParam(value = "priorityMask", required = false) MultipartFile priorityMask
    ) {
        return obamifyTempPresetService.createTempPreset(
                file,
                mode,
                priority,
                priorityRegions,
                priorityPolygons,
                priorityMask
        );
    }
}