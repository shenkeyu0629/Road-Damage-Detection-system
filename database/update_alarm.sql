-- 添加告警表新字段
ALTER TABLE alarm_info ADD COLUMN image_path VARCHAR(500) COMMENT '原始图片路径' AFTER handle_result;
ALTER TABLE alarm_info ADD COLUMN result_image_path VARCHAR(500) COMMENT '检测结果图片路径' AFTER image_path;
ALTER TABLE alarm_info ADD COLUMN remark TEXT COMMENT '备注' AFTER result_image_path;
