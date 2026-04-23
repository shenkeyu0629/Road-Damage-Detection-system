package com.roadinspection.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class DetectionRequestDTO {
    @NotBlank(message = "图片路径不能为空")
    private String imagePath;
    
    private Double confThreshold = 0.25;
    
    private Long roadId;
    
    private Long sectionId;
    
    private Long taskId;
    
    private BigDecimal longitude;
    
    private BigDecimal latitude;
    
    private String stakeNumber;
    
    private String direction;
}
