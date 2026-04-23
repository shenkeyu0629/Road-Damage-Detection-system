package com.roadinspection.dto;

import lombok.Data;
import java.math.BigDecimal;

@Data
public class VideoDetectionRequestDTO {
    private String videoPath;
    private Double confThreshold;
    private Integer frameInterval;
    private Long taskId;
    private Long roadId;
    private Long sectionId;
    private BigDecimal longitude;
    private BigDecimal latitude;
    private String stakeNumber;
    private String direction;
}
