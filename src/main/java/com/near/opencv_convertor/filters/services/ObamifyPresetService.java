package com.near.opencv_convertor.filters.services;

import com.near.opencv_convertor.dto.ObamifyPresetDto;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class ObamifyPresetService {

    private static final String PRESET_TARGET_PATTERN = "classpath*:/obamify/*/target256.png";
    private static final String PRESET_WEIGHTS_PATTERN = "classpath*:/obamify/*/weights256.png";

    private final PathMatchingResourcePatternResolver resolver =
            new PathMatchingResourcePatternResolver();

    public List<ObamifyPresetDto> getPresets() {
        try {
            Map<String, Boolean> targetPresets = findPresetNames(PRESET_TARGET_PATTERN);
            Map<String, Boolean> weightPresets = findPresetNames(PRESET_WEIGHTS_PATTERN);

            List<ObamifyPresetDto> result = new ArrayList<>();

            for (String preset : targetPresets.keySet()) {
                if (!weightPresets.containsKey(preset)) {
                    continue;
                }

                result.add(new ObamifyPresetDto(
                        preset,
                        buildLabel(preset),
                        buildDescription(preset)
                ));
            }

            result.sort(Comparator.comparing(ObamifyPresetDto::getLabel));
            return result;
        } catch (IOException e) {
            throw new RuntimeException("Failed to read obamify presets", e);
        }
    }

    public boolean presetExists(String preset) {
        return getPresets()
                .stream()
                .anyMatch(item -> item.getValue().equals(preset));
    }

    private Map<String, Boolean> findPresetNames(String pattern) throws IOException {
        Resource[] resources = resolver.getResources(pattern);

        Map<String, Boolean> presets = new LinkedHashMap<>();

        for (Resource resource : resources) {
            String url = URLDecoder.decode(
                    resource.getURL().toString(),
                    StandardCharsets.UTF_8
            );

            String preset = extractPresetName(url);

            if (preset != null && !preset.isBlank()) {
                presets.put(preset, true);
            }
        }

        return presets;
    }

    private String extractPresetName(String url) {
        String normalized = url.replace("\\", "/");

        String marker = "/obamify/";
        int markerIndex = normalized.indexOf(marker);

        if (markerIndex < 0) {
            return null;
        }

        String afterMarker = normalized.substring(markerIndex + marker.length());
        String[] parts = afterMarker.split("/");

        if (parts.length < 2) {
            return null;
        }

        return parts[0];
    }

    private String buildLabel(String value) {
        String[] parts = value.replace("-", "_").split("_");

        StringBuilder result = new StringBuilder();

        for (String part : parts) {
            if (part.isBlank()) {
                continue;
            }

            if (result.length() > 0) {
                result.append(" ");
            }

            result.append(part.substring(0, 1).toUpperCase());
            result.append(part.substring(1).toLowerCase());
        }

        return result.length() == 0 ? value : result.toString();
    }

    private String buildDescription(String value) {
        if ("obama".equalsIgnoreCase(value)) {
            return "Default Obama-style target preset";
        }

        return "Custom target preset";
    }
}