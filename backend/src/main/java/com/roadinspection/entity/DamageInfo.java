package com.roadinspection.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.roadinspection.common.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("damage_info")
public class DamageInfo extends BaseEntity {
    private Long recordId;
    private Long taskId;
    private Long roadId;
    private Long sectionId;
    private String damageType;
    private String damageTypeEn;
    private String damageLevel;
    private Double confidence;
    private Integer positionX;
    private Integer positionY;
    private Integer width;
    private Integer height;
    private Integer bboxX1;
    private Integer bboxY1;
    private Integer bboxX2;
    private Integer bboxY2;
    private Integer area;
    private BigDecimal areaRatio;
    private BigDecimal longitude;
    private BigDecimal latitude;
    private String stakeNumber;
    private String direction;
    private String imagePath;
    private String resultImagePath;
    private String status;
    private String repairSuggestion;
    private String deduplicationHash;
    private LocalDateTime firstDetectedTime;
    private LocalDateTime lastDetectedTime;
    private Integer detectionCount;
}
