-- 操作日志表
CREATE TABLE IF NOT EXISTS operation_log (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    username VARCHAR(50) COMMENT '操作用户',
    operation_type VARCHAR(50) COMMENT '操作类型',
    operation_desc VARCHAR(500) COMMENT '操作描述',
    target_type VARCHAR(50) COMMENT '操作对象类型',
    target_id BIGINT COMMENT '操作对象ID',
    ip_address VARCHAR(50) COMMENT 'IP地址',
    result VARCHAR(20) COMMENT '操作结果',
    request_params TEXT COMMENT '请求参数',
    response_data TEXT COMMENT '响应数据',
    error_msg TEXT COMMENT '错误信息',
    operation_time DATETIME COMMENT '操作时间',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted TINYINT DEFAULT 0 COMMENT '是否删除'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='操作日志表';

-- 宽核配置表
CREATE TABLE IF NOT EXISTS audit_config (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    enabled TINYINT DEFAULT 1 COMMENT '是否启用审核',
    level VARCHAR(20) DEFAULT 'single' COMMENT '审核级别',
    confidence_threshold INT DEFAULT 70 COMMENT '置信度阈值',
    force_review_types VARCHAR(500) COMMENT '强制审核类型',
    reviewer_assignment VARCHAR(20) DEFAULT 'role' COMMENT '审核人分配方式',
    specific_reviewers VARCHAR(500) COMMENT '指定审核人',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted TINYINT DEFAULT 0 COMMENT '是否删除'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='审核配置表';

-- 审核任务表
CREATE TABLE IF NOT EXISTS audit_task (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    record_id BIGINT COMMENT '巡检记录ID',
    road_id BIGINT COMMENT '道路ID',
    section_id BIGINT COMMENT '路段ID',
    road_name VARCHAR(100) COMMENT '道路名称',
    section_name VARCHAR(100) COMMENT '路段名称',
    damage_count INT COMMENT '病害数量',
    damage_types VARCHAR(500) COMMENT '病害类型',
    min_confidence DECIMAL(5,4) COMMENT '最低置信度',
    image_url VARCHAR(500) COMMENT '图片URL',
    damages TEXT COMMENT '病害详情JSON',
    status VARCHAR(20) DEFAULT 'pending' COMMENT '状态',
    uploader_name VARCHAR(50) COMMENT '上传人',
    upload_time DATETIME COMMENT '上传时间',
    reviewer_name VARCHAR(50) COMMENT '审核人',
    review_time DATETIME COMMENT '审核时间',
    comment VARCHAR(500) COMMENT '审核意见',
    suggestion VARCHAR(100) COMMENT '养护建议',
    approved_damages TEXT COMMENT '通过的病害ID',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted TINYINT DEFAULT 0 COMMENT '是否删除'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='审核任务表';

-- 为用户表添加角色字段
ALTER TABLE user ADD COLUMN IF NOT EXISTS roles VARCHAR(200) DEFAULT 'inspector' COMMENT '角色列表';
ALTER TABLE user ADD COLUMN IF NOT EXISTS email VARCHAR(100) COMMENT '邮箱';
ALTER TABLE user ADD COLUMN IF NOT EXISTS phone VARCHAR(20) COMMENT '手机号';

-- 初始化审核配置
INSERT INTO audit_config (enabled, level, confidence_threshold, force_review_types, reviewer_assignment)
SELECT 1, 'single', 70, 'pothole', 'role'
WHERE NOT EXISTS (SELECT 1 FROM audit_config LIMIT 1);
