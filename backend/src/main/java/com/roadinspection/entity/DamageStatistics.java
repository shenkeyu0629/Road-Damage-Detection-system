package com.roadinspection.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.roadinspection.common.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDate;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("damage_statistics")
public class DamageStatistics extends BaseEntity {
    private LocalDate statDate;
    private String statType;
    private Long roadId;
    private Long sectionId;
    private Integer totalInspectionCount;
    private Integer totalDamageCount;
    private Integer minorCount;
    private Integer moderateCount;
    private Integer severeCount;
    private Integer longitudinalCrackCount;
    private Integer transverseCrackCount;
    private Integer alligatorCrackCount;
    private Integer potholeCount;
    private Integer ruttingCount;
    private Integer patchCount;
    private Integer settlementCount;
    private Integer ravelingCount;
    private Integer alarmCount;
    private Integer repairedCount;
}
