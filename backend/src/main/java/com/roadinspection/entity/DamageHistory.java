package com.roadinspection.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.roadinspection.common.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("damage_history")
public class DamageHistory extends BaseEntity {
    private Long roadId;
    private Long sectionId;
    private String roadName;
    private String sectionName;
    private Integer damageCount;
    private String damageLevel;
    private String damageTypes;
    private LocalDateTime detectionTime;
    private String videoSource;
    private String status;
    private String disposalMethod;
}
