-- 历史病害统计表
CREATE TABLE IF NOT EXISTS damage_history (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted INT DEFAULT 0,
    
    road_id BIGINT COMMENT '道路ID',
    section_id BIGINT COMMENT '路段ID',
    road_name VARCHAR(100) COMMENT '道路名称',
    section_name VARCHAR(100) COMMENT '路段名称',
    
    damage_count INT DEFAULT 1 COMMENT '病害数量',
    damage_level VARCHAR(20) COMMENT '病害等级：轻微/中等/严重',
    damage_types VARCHAR(500) COMMENT '病害类型列表',
    
    detection_time DATETIME COMMENT '病害检测时间',
    video_source VARCHAR(200) COMMENT '视频源信息',
    
    status VARCHAR(20) DEFAULT '未处理' COMMENT '状态：未处理/处理中/已处理',
    disposal_method TEXT COMMENT '处置方法'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='历史病害统计';

-- 病害处置记录表
CREATE TABLE IF NOT EXISTS damage_disposal (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted INT DEFAULT 0,
    
    damage_history_id BIGINT COMMENT '关联的历史病害记录ID',
    
    road_id BIGINT COMMENT '道路ID',
    section_id BIGINT COMMENT '路段ID',
    road_name VARCHAR(100) COMMENT '道路名称',
    section_name VARCHAR(100) COMMENT '路段名称',
    
    damage_count INT DEFAULT 1 COMMENT '病害数量',
    damage_level VARCHAR(20) COMMENT '病害等级',
    damage_types VARCHAR(500) COMMENT '病害类型列表',
    
    disposal_method TEXT COMMENT '处置方法',
    disposal_time DATETIME COMMENT '处置时间'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='病害处置记录';
