package com.roadinspection.dto;

import lombok.Data;

import java.time.LocalDate;

@Data
public class StatisticsQueryDTO {
    private LocalDate startDate;
    private LocalDate endDate;
    private Long roadId;
    private Long sectionId;
    private String statType;
}
