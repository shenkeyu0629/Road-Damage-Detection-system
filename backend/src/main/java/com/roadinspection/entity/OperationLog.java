package com.roadinspection.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("sys_operation_log")
public class OperationLog {
    private Long id;
    private String username;
    private String operationType;
    private String operationDesc;
    private LocalDateTime operationTime;
}
