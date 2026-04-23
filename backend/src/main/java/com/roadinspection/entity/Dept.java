package com.roadinspection.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.roadinspection.common.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_dept")
public class Dept extends BaseEntity {
    private String deptName;
    private Long parentId;
    private String deptPath;
    private Integer sortOrder;
    private String leader;
    private String phone;
    private Integer status;
}
