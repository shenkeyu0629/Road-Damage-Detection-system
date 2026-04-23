package com.roadinspection.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.roadinspection.common.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("inspection_record")
public class InspectionRecord extends BaseEntity {
    private Long taskId;
    private Long roadId;
    private Long sectionId;
    private String imagePath;
    private String resultImagePath;
    private Integer damageCount;
    private String damageLevel;
    private String detectionResult;
    private BigDecimal longitude;
    private BigDecimal latitude;
    private String stakeNumber;
    private String direction;
    private Integer alarmNeeded;
    private LocalDateTime inspectionTime;
    private String status;
    private String result;
    
    @TableField(exist = false)
    private List<DamageInfo> damages;
    
    @TableField(exist = false)
    private String roadName;
    
    @TableField(exist = false)
    private String sectionName;
}
