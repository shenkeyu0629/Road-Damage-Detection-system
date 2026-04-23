package com.roadinspection.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.roadinspection.common.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("road_section")
public class RoadSection extends BaseEntity {
    private Long roadId;
    private String sectionName;
    private String sectionCode;
    private String startStake;
    private String endStake;
    private String startPoint;
    private String endPoint;
    private BigDecimal length;
    private Integer laneCount;
    private String pavementType;
    private String direction;
    private Integer status;
    private String description;
    private String videoFolderPath;
}
