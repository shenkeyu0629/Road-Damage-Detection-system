package com.roadinspection.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.roadinspection.common.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("inspection_task")
public class InspectionTask extends BaseEntity {
    private String taskName;
    private String taskCode;
    private Long roadId;
    private Long sectionId;
    private String taskType;
    private LocalDateTime scheduledStartTime;
    private LocalDateTime scheduledEndTime;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private String status;
    private String weather;
    private BigDecimal temperature;
    private String remark;
    
    @TableField(exist = false)
    private String roadName;
    
    @TableField(exist = false)
    private String sectionName;
}
