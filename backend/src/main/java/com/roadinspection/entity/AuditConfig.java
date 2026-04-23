package com.roadinspection.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.roadinspection.common.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("audit_config")
public class AuditConfig extends BaseEntity {
    private Boolean enabled;
    private String level;
    private Integer confidenceThreshold;
    private String forceReviewTypes;
    private String reviewerAssignment;
    private String specificReviewers;
}
