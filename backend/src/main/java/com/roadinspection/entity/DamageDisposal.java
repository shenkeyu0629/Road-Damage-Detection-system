package com.roadinspection.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.roadinspection.common.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("damage_disposal")
public class DamageDisposal extends BaseEntity {
    private Long damageHistoryId;
    private Long roadId;
    private Long sectionId;
    private String roadName;
    private String sectionName;
    private Integer damageCount;
    private String damageLevel;
    private String damageTypes;
    private String disposalMethod;
    private LocalDateTime disposalTime;
    private String auditStatus;
    private LocalDateTime auditTime;
    private String auditor;
    private String auditRemark;
}
