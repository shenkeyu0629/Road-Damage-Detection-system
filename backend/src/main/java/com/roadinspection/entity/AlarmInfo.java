package com.roadinspection.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.roadinspection.common.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("alarm_info")
public class AlarmInfo extends BaseEntity {
    private String alarmCode;
    private Long taskId;
    private Long recordId;
    private Long damageId;
    private Long roadId;
    private Long sectionId;
    private String alarmType;
    private String alarmLevel;
    private String alarmReason;
    private BigDecimal longitude;
    private BigDecimal latitude;
    private String stakeNumber;
    private String status;
    private Long handlerId;
    private String handlerName;
    private LocalDateTime handleTime;
    private String handleResult;
    private String imagePath;
    private String resultImagePath;
    private String remark;
}
