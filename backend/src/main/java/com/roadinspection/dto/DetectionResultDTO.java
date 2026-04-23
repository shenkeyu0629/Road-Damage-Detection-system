package com.roadinspection.dto;

import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
public class DetectionResultDTO {
    private String imagePath;
    private Integer imageWidth;
    private Integer imageHeight;
    private List<Map<String, Object>> detections;
    private Map<String, Object> damageSummary;
    private String overallLevel;
    private Boolean alarmNeeded;
    private Map<String, Object> alarmInfo;
    private String detectionTime;
    private String resultImagePath;
}
