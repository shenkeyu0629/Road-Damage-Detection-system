package com.roadinspection.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.roadinspection.common.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("audit_task")
public class AuditTask extends BaseEntity {
    private Long recordId;
    private Long roadId;
    private Long sectionId;
    private String roadName;
    private String sectionName;
    private Integer damageCount;
    private String damageTypes;
    private Double minConfidence;
    private String imageUrl;
    private String damages;
    private String status;
    private String uploaderName;
    private LocalDateTime uploadTime;
    private String reviewerName;
    private LocalDateTime reviewTime;
    private String comment;
    private String suggestion;
    private String approvedDamages;
}
