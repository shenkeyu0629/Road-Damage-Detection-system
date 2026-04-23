package com.roadinspection.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.roadinspection.common.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("disposal_record")
public class DisposalRecord extends BaseEntity {
    private Long alarmId;
    private Long damageId;
    private Long roadId;
    private Long sectionId;
    private String disposalType;
    private String disposalMethod;
    private String disposalDesc;
    private String beforeImages;
    private String afterImages;
    private BigDecimal cost;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private Long executorId;
    private String executorName;
    private String status;
    private String verifyResult;
    private String remark;
}
