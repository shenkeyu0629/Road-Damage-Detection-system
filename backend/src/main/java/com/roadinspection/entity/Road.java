package com.roadinspection.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.roadinspection.common.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("road_info")
public class Road extends BaseEntity {
    private String roadName;
    private String roadCode;
    private String roadLevel;
    private String startPoint;
    private String endPoint;
    private BigDecimal totalLength;
    private String direction;
    private String region;
    private String manageUnit;
    private LocalDate buildDate;
    private Integer status;
    private String description;
}
