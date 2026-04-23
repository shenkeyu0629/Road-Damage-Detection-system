-- 为damage_info表添加缺失字段
ALTER TABLE damage_info ADD COLUMN position_x INT COMMENT '位置X';
ALTER TABLE damage_info ADD COLUMN position_y INT COMMENT '位置Y';
ALTER TABLE damage_info ADD COLUMN width INT COMMENT '宽度';
ALTER TABLE damage_info ADD COLUMN height INT COMMENT '高度';
ALTER TABLE damage_info ADD COLUMN image_path VARCHAR(500) COMMENT '图片路径';
ALTER TABLE damage_info ADD COLUMN result_image_path VARCHAR(500) COMMENT '结果图片路径';
ALTER TABLE damage_info ADD COLUMN deduplication_hash VARCHAR(100) COMMENT '去重哈希';
ALTER TABLE damage_info ADD COLUMN first_detected_time DATETIME COMMENT '首次检测时间';
ALTER TABLE damage_info ADD COLUMN last_detected_time DATETIME COMMENT '最后检测时间';
ALTER TABLE damage_info ADD COLUMN detection_count INT DEFAULT 1 COMMENT '检测次数';

-- 为inspection_record表添加缺失字段
ALTER TABLE inspection_record ADD COLUMN status VARCHAR(20) DEFAULT 'pending' COMMENT '状态';

-- 添加索引
ALTER TABLE damage_info ADD INDEX idx_dedup_hash (deduplication_hash);
ALTER TABLE damage_info ADD INDEX idx_first_detected_time (first_detected_time);
