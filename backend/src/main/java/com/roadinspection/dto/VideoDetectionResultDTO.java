package com.roadinspection.dto;

import com.roadinspection.service.TrackedDamage;
import lombok.Data;
import java.util.List;
import java.util.Map;

@Data
public class VideoDetectionResultDTO {
    private String videoPath;
    private Integer totalFrames;
    private Integer processedFrames;
    private Integer fps;
    private Long durationMs;
    private List<TrackedDamage> trackedDamages;
    private Integer uniqueDamageCount;
    private String overallLevel;
    private Boolean alarmNeeded;
    private Map<String, Object> damageSummary;
}
