-- 道路路面病害智能检测系统数据库初始化脚本
-- 创建数据库
CREATE DATABASE IF NOT EXISTS road_inspection DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

USE road_inspection;

-- 用户表
CREATE TABLE IF NOT EXISTS sys_user (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '用户ID',
    username VARCHAR(50) NOT NULL UNIQUE COMMENT '用户名',
    password VARCHAR(255) NOT NULL COMMENT '密码(加密)',
    real_name VARCHAR(50) COMMENT '真实姓名',
    email VARCHAR(100) COMMENT '邮箱',
    phone VARCHAR(20) COMMENT '手机号',
    avatar VARCHAR(255) COMMENT '头像URL',
    status TINYINT DEFAULT 1 COMMENT '状态: 0-禁用, 1-启用',
    dept_id BIGINT COMMENT '部门ID',
    role_id BIGINT COMMENT '角色ID',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    last_login_time DATETIME COMMENT '最后登录时间',
    deleted TINYINT DEFAULT 0 COMMENT '删除标记: 0-未删除, 1-已删除',
    INDEX idx_username (username),
    INDEX idx_dept_id (dept_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户表';

-- 角色表
CREATE TABLE IF NOT EXISTS sys_role (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '角色ID',
    role_name VARCHAR(50) NOT NULL COMMENT '角色名称',
    role_code VARCHAR(50) NOT NULL UNIQUE COMMENT '角色编码',
    description VARCHAR(255) COMMENT '角色描述',
    status TINYINT DEFAULT 1 COMMENT '状态: 0-禁用, 1-启用',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted TINYINT DEFAULT 0 COMMENT '删除标记',
    INDEX idx_role_code (role_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='角色表';

-- 权限表
CREATE TABLE IF NOT EXISTS sys_permission (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '权限ID',
    permission_name VARCHAR(50) NOT NULL COMMENT '权限名称',
    permission_code VARCHAR(100) NOT NULL UNIQUE COMMENT '权限编码',
    permission_type TINYINT COMMENT '权限类型: 1-菜单, 2-按钮, 3-接口',
    parent_id BIGINT DEFAULT 0 COMMENT '父权限ID',
    path VARCHAR(255) COMMENT '路由路径',
    component VARCHAR(255) COMMENT '组件路径',
    icon VARCHAR(50) COMMENT '图标',
    sort_order INT DEFAULT 0 COMMENT '排序',
    status TINYINT DEFAULT 1 COMMENT '状态',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted TINYINT DEFAULT 0 COMMENT '删除标记',
    INDEX idx_parent_id (parent_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='权限表';

-- 角色权限关联表
CREATE TABLE IF NOT EXISTS sys_role_permission (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    role_id BIGINT NOT NULL COMMENT '角色ID',
    permission_id BIGINT NOT NULL COMMENT '权限ID',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_role_permission (role_id, permission_id),
    INDEX idx_role_id (role_id),
    INDEX idx_permission_id (permission_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='角色权限关联表';

-- 部门表
CREATE TABLE IF NOT EXISTS sys_dept (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '部门ID',
    dept_name VARCHAR(50) NOT NULL COMMENT '部门名称',
    parent_id BIGINT DEFAULT 0 COMMENT '父部门ID',
    dept_path VARCHAR(255) COMMENT '部门路径',
    sort_order INT DEFAULT 0 COMMENT '排序',
    leader VARCHAR(50) COMMENT '负责人',
    phone VARCHAR(20) COMMENT '联系电话',
    status TINYINT DEFAULT 1 COMMENT '状态',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted TINYINT DEFAULT 0 COMMENT '删除标记',
    INDEX idx_parent_id (parent_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='部门表';

-- 操作日志表
CREATE TABLE IF NOT EXISTS sys_operation_log (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '日志ID',
    user_id BIGINT COMMENT '操作用户ID',
    username VARCHAR(50) COMMENT '操作用户名',
    operation_type VARCHAR(50) COMMENT '操作类型',
    operation_desc VARCHAR(255) COMMENT '操作描述',
    request_method VARCHAR(10) COMMENT '请求方法',
    request_url VARCHAR(500) COMMENT '请求URL',
    request_params TEXT COMMENT '请求参数',
    response_result TEXT COMMENT '响应结果',
    ip VARCHAR(50) COMMENT '操作IP',
    location VARCHAR(100) COMMENT '操作地点',
    status TINYINT COMMENT '操作状态: 0-失败, 1-成功',
    error_msg TEXT COMMENT '错误信息',
    execution_time INT COMMENT '执行时间(ms)',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '操作时间',
    INDEX idx_user_id (user_id),
    INDEX idx_create_time (create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='操作日志表';

-- 道路信息表
CREATE TABLE IF NOT EXISTS road_info (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '道路ID',
    road_name VARCHAR(100) NOT NULL COMMENT '道路名称',
    road_code VARCHAR(50) UNIQUE COMMENT '道路编码',
    road_level VARCHAR(20) COMMENT '道路等级: highway-高速, national-国道, provincial-省道, county-县道, city-城市道路',
    start_point VARCHAR(255) COMMENT '起点',
    end_point VARCHAR(255) COMMENT '终点',
    total_length DECIMAL(10,2) COMMENT '总长度(km)',
    direction VARCHAR(20) COMMENT '方向',
    region VARCHAR(100) COMMENT '所属区域',
    manage_unit VARCHAR(100) COMMENT '管理单位',
    build_date DATE COMMENT '建设日期',
    status TINYINT DEFAULT 1 COMMENT '状态: 0-停用, 1-启用',
    description TEXT COMMENT '描述',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted TINYINT DEFAULT 0 COMMENT '删除标记',
    INDEX idx_road_code (road_code),
    INDEX idx_region (region)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='道路信息表';

-- 路段信息表
CREATE TABLE IF NOT EXISTS road_section (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '路段ID',
    road_id BIGINT NOT NULL COMMENT '道路ID',
    section_name VARCHAR(100) NOT NULL COMMENT '路段名称',
    section_code VARCHAR(50) COMMENT '路段编码',
    start_stake VARCHAR(50) COMMENT '起始桩号',
    end_stake VARCHAR(50) COMMENT '终止桩号',
    start_point VARCHAR(255) COMMENT '起点',
    end_point VARCHAR(255) COMMENT '终点',
    length DECIMAL(10,2) COMMENT '长度(km)',
    lane_count INT COMMENT '车道数',
    pavement_type VARCHAR(20) COMMENT '路面类型: asphalt-沥青, concrete-水泥, other-其他',
    direction VARCHAR(20) COMMENT '方向',
    status TINYINT DEFAULT 1 COMMENT '状态',
    description TEXT COMMENT '描述',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted TINYINT DEFAULT 0 COMMENT '删除标记',
    INDEX idx_road_id (road_id),
    INDEX idx_section_code (section_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='路段信息表';

-- 巡检任务表
CREATE TABLE IF NOT EXISTS inspection_task (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '任务ID',
    task_name VARCHAR(100) NOT NULL COMMENT '任务名称',
    task_code VARCHAR(50) UNIQUE COMMENT '任务编码',
    road_id BIGINT COMMENT '道路ID',
    section_id BIGINT COMMENT '路段ID',
    task_type VARCHAR(20) COMMENT '任务类型: routine-常规巡检, special-专项巡检, emergency-应急巡检',
    inspector_id BIGINT COMMENT '巡检员ID',
    inspector_name VARCHAR(50) COMMENT '巡检员姓名',
    plan_date DATE COMMENT '计划日期',
    start_time DATETIME COMMENT '开始时间',
    end_time DATETIME COMMENT '结束时间',
    status VARCHAR(20) DEFAULT 'pending' COMMENT '状态: pending-待执行, in_progress-执行中, completed-已完成, cancelled-已取消',
    weather VARCHAR(50) COMMENT '天气',
    temperature DECIMAL(5,2) COMMENT '温度',
    remark TEXT COMMENT '备注',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted TINYINT DEFAULT 0 COMMENT '删除标记',
    INDEX idx_road_id (road_id),
    INDEX idx_section_id (section_id),
    INDEX idx_inspector_id (inspector_id),
    INDEX idx_status (status),
    INDEX idx_plan_date (plan_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='巡检任务表';

-- 巡检记录表
CREATE TABLE IF NOT EXISTS inspection_record (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '记录ID',
    task_id BIGINT COMMENT '任务ID',
    road_id BIGINT COMMENT '道路ID',
    section_id BIGINT COMMENT '路段ID',
    inspector_id BIGINT COMMENT '巡检员ID',
    inspector_name VARCHAR(50) COMMENT '巡检员姓名',
    inspection_time DATETIME COMMENT '巡检时间',
    longitude DECIMAL(10,6) COMMENT '经度',
    latitude DECIMAL(10,6) COMMENT '纬度',
    stake_number VARCHAR(50) COMMENT '桩号',
    direction VARCHAR(20) COMMENT '方向',
    image_path VARCHAR(500) COMMENT '原始图片路径',
    result_image_path VARCHAR(500) COMMENT '检测结果图片路径',
    video_path VARCHAR(500) COMMENT '视频路径',
    result_video_path VARCHAR(500) COMMENT '检测结果视频路径',
    damage_count INT DEFAULT 0 COMMENT '病害数量',
    damage_level VARCHAR(20) COMMENT '总体病害等级',
    alarm_needed TINYINT DEFAULT 0 COMMENT '是否需要告警',
    detection_result TEXT COMMENT '检测结果JSON',
    remark TEXT COMMENT '备注',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted TINYINT DEFAULT 0 COMMENT '删除标记',
    INDEX idx_task_id (task_id),
    INDEX idx_road_id (road_id),
    INDEX idx_section_id (section_id),
    INDEX idx_inspector_id (inspector_id),
    INDEX idx_inspection_time (inspection_time),
    INDEX idx_damage_level (damage_level)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='巡检记录表';

-- 病害信息表
CREATE TABLE IF NOT EXISTS damage_info (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '病害ID',
    record_id BIGINT NOT NULL COMMENT '巡检记录ID',
    task_id BIGINT COMMENT '任务ID',
    road_id BIGINT COMMENT '道路ID',
    section_id BIGINT COMMENT '路段ID',
    damage_type VARCHAR(50) COMMENT '病害类型',
    damage_type_en VARCHAR(50) COMMENT '病害类型英文',
    damage_level VARCHAR(20) COMMENT '病害等级: minor-轻微, moderate-中等, severe-严重',
    confidence DECIMAL(5,4) COMMENT '置信度',
    bbox_x1 INT COMMENT '边界框x1',
    bbox_y1 INT COMMENT '边界框y1',
    bbox_x2 INT COMMENT '边界框x2',
    bbox_y2 INT COMMENT '边界框y2',
    area INT COMMENT '面积(像素)',
    area_ratio DECIMAL(8,6) COMMENT '面积占比',
    longitude DECIMAL(10,6) COMMENT '经度',
    latitude DECIMAL(10,6) COMMENT '纬度',
    stake_number VARCHAR(50) COMMENT '桩号',
    direction VARCHAR(20) COMMENT '方向',
    status VARCHAR(20) DEFAULT 'detected' COMMENT '状态: detected-已检测, confirmed-已确认, processing-处理中, repaired-已修复',
    repair_suggestion TEXT COMMENT '维修建议',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted TINYINT DEFAULT 0 COMMENT '删除标记',
    INDEX idx_record_id (record_id),
    INDEX idx_task_id (task_id),
    INDEX idx_road_id (road_id),
    INDEX idx_section_id (section_id),
    INDEX idx_damage_type (damage_type),
    INDEX idx_damage_level (damage_level),
    INDEX idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='病害信息表';

-- 告警信息表
CREATE TABLE IF NOT EXISTS alarm_info (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '告警ID',
    alarm_code VARCHAR(50) UNIQUE COMMENT '告警编码',
    record_id BIGINT COMMENT '巡检记录ID',
    damage_id BIGINT COMMENT '病害ID',
    road_id BIGINT COMMENT '道路ID',
    section_id BIGINT COMMENT '路段ID',
    alarm_type VARCHAR(50) COMMENT '告警类型',
    alarm_level VARCHAR(20) COMMENT '告警级别: minor-轻微, moderate-中等, severe-严重',
    alarm_reason TEXT COMMENT '告警原因',
    longitude DECIMAL(10,6) COMMENT '经度',
    latitude DECIMAL(10,6) COMMENT '纬度',
    stake_number VARCHAR(50) COMMENT '桩号',
    status VARCHAR(20) DEFAULT 'pending' COMMENT '状态: pending-待处理, processing-处理中, resolved-已解决, ignored-已忽略',
    handler_id BIGINT COMMENT '处理人ID',
    handler_name VARCHAR(50) COMMENT '处理人姓名',
    handle_time DATETIME COMMENT '处理时间',
    handle_result TEXT COMMENT '处理结果',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted TINYINT DEFAULT 0 COMMENT '删除标记',
    INDEX idx_record_id (record_id),
    INDEX idx_damage_id (damage_id),
    INDEX idx_road_id (road_id),
    INDEX idx_alarm_level (alarm_level),
    INDEX idx_status (status),
    INDEX idx_create_time (create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='告警信息表';

-- 处置记录表
CREATE TABLE IF NOT EXISTS disposal_record (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '处置ID',
    alarm_id BIGINT COMMENT '告警ID',
    damage_id BIGINT COMMENT '病害ID',
    road_id BIGINT COMMENT '道路ID',
    section_id BIGINT COMMENT '路段ID',
    disposal_type VARCHAR(50) COMMENT '处置类型: repair-维修, monitor-监控, ignore-忽略',
    disposal_method VARCHAR(100) COMMENT '处置方式',
    disposal_desc TEXT COMMENT '处置描述',
    before_images TEXT COMMENT '处置前图片(JSON数组)',
    after_images TEXT COMMENT '处置后图片(JSON数组)',
    cost DECIMAL(10,2) COMMENT '费用(元)',
    start_time DATETIME COMMENT '开始时间',
    end_time DATETIME COMMENT '结束时间',
    executor_id BIGINT COMMENT '执行人ID',
    executor_name VARCHAR(50) COMMENT '执行人姓名',
    supervisor_id BIGINT COMMENT '监督人ID',
    supervisor_name VARCHAR(50) COMMENT '监督人姓名',
    status VARCHAR(20) DEFAULT 'pending' COMMENT '状态: pending-待处理, in_progress-进行中, completed-已完成, verified-已验收',
    verify_time DATETIME COMMENT '验收时间',
    verify_result TEXT COMMENT '验收结果',
    remark TEXT COMMENT '备注',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted TINYINT DEFAULT 0 COMMENT '删除标记',
    INDEX idx_alarm_id (alarm_id),
    INDEX idx_damage_id (damage_id),
    INDEX idx_road_id (road_id),
    INDEX idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='处置记录表';

-- 病害统计表
CREATE TABLE IF NOT EXISTS damage_statistics (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '统计ID',
    stat_date DATE COMMENT '统计日期',
    stat_type VARCHAR(20) COMMENT '统计类型: daily-日, weekly-周, monthly-月, yearly-年',
    road_id BIGINT COMMENT '道路ID',
    section_id BIGINT COMMENT '路段ID',
    total_inspection_count INT DEFAULT 0 COMMENT '巡检总次数',
    total_damage_count INT DEFAULT 0 COMMENT '病害总数',
    minor_count INT DEFAULT 0 COMMENT '轻微病害数',
    moderate_count INT DEFAULT 0 COMMENT '中等病害数',
    severe_count INT DEFAULT 0 COMMENT '严重病害数',
    longitudinal_crack_count INT DEFAULT 0 COMMENT '纵向裂缝数',
    transverse_crack_count INT DEFAULT 0 COMMENT '横向裂缝数',
    alligator_crack_count INT DEFAULT 0 COMMENT '龟裂数',
    pothole_count INT DEFAULT 0 COMMENT '坑槽数',
    rutting_count INT DEFAULT 0 COMMENT '车辙数',
    patch_count INT DEFAULT 0 COMMENT '修补数',
    settlement_count INT DEFAULT 0 COMMENT '沉陷数',
    raveling_count INT DEFAULT 0 COMMENT '剥落数',
    alarm_count INT DEFAULT 0 COMMENT '告警数',
    repaired_count INT DEFAULT 0 COMMENT '已修复数',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    UNIQUE KEY uk_stat (stat_date, stat_type, road_id, section_id),
    INDEX idx_stat_date (stat_date),
    INDEX idx_road_id (road_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='病害统计表';

-- 系统配置表
CREATE TABLE IF NOT EXISTS sys_config (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '配置ID',
    config_key VARCHAR(100) NOT NULL UNIQUE COMMENT '配置键',
    config_value TEXT COMMENT '配置值',
    config_type VARCHAR(20) COMMENT '配置类型',
    description VARCHAR(255) COMMENT '配置描述',
    status TINYINT DEFAULT 1 COMMENT '状态',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX idx_config_key (config_key)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='系统配置表';

-- 初始化角色数据
INSERT INTO sys_role (role_name, role_code, description, status) VALUES
('超级管理员', 'SUPER_ADMIN', '系统超级管理员，拥有所有权限', 1),
('系统管理员', 'ADMIN', '系统管理员，拥有大部分管理权限', 1),
('巡检管理员', 'INSPECTION_ADMIN', '巡检管理员，管理巡检任务和记录', 1),
('巡检员', 'INSPECTOR', '巡检员，执行巡检任务', 1),
('数据分析员', 'ANALYST', '数据分析员，查看统计数据和报表', 1);

-- 初始化部门数据
INSERT INTO sys_dept (dept_name, parent_id, dept_path, sort_order, status) VALUES
('道路养护管理中心', 0, '/1', 1, 1),
('巡检部', 1, '/1/2', 1, 1),
('维修部', 1, '/1/3', 2, 1),
('数据分析部', 1, '/1/4', 3, 1);

-- 初始化管理员用户 (密码: admin123，使用BCrypt加密)
INSERT INTO sys_user (username, password, real_name, email, phone, status, dept_id, role_id) VALUES
('admin', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iAt6Z5EH', '系统管理员', 'admin@example.com', '13800138000', 1, 1, 1);

-- 初始化权限数据
INSERT INTO sys_permission (permission_name, permission_code, permission_type, parent_id, path, component, icon, sort_order, status) VALUES
('系统管理', 'system', 1, 0, '/system', 'Layout', 'setting', 1, 1),
('用户管理', 'system:user', 1, 1, '/system/user', 'system/user/index', 'user', 1, 1),
('角色管理', 'system:role', 1, 1, '/system/role', 'system/role/index', 'peoples', 2, 1),
('权限管理', 'system:permission', 1, 1, '/system/permission', 'system/permission/index', 'lock', 3, 1),
('部门管理', 'system:dept', 1, 1, '/system/dept', 'system/dept/index', 'tree', 4, 1),
('操作日志', 'system:log', 1, 1, '/system/log', 'system/log/index', 'log', 5, 1),
('道路管理', 'road', 1, 0, '/road', 'Layout', 'road', 2, 1),
('道路信息', 'road:info', 1, 7, '/road/info', 'road/info/index', 'guide', 1, 1),
('路段管理', 'road:section', 1, 7, '/road/section', 'road/section/index', 'tree-table', 2, 1),
('巡检管理', 'inspection', 1, 0, '/inspection', 'Layout', 'inspection', 3, 1),
('巡检任务', 'inspection:task', 1, 10, '/inspection/task', 'inspection/task/index', 'list', 1, 1),
('巡检记录', 'inspection:record', 1, 10, '/inspection/record', 'inspection/record/index', 'documentation', 2, 1),
('病害管理', 'damage', 1, 0, '/damage', 'Layout', 'damage', 4, 1),
('病害检测', 'damage:detect', 1, 14, '/damage/detect', 'damage/detect/index', 'search', 1, 1),
('病害列表', 'damage:list', 1, 14, '/damage/list', 'damage/list/index', 'table', 2, 1),
('告警管理', 'damage:alarm', 1, 14, '/damage/alarm', 'damage/alarm/index', 'message', 3, 1),
('处置记录', 'damage:disposal', 1, 14, '/damage/disposal', 'damage/disposal/index', 'edit', 4, 1),
('统计分析', 'statistics', 1, 0, '/statistics', 'Layout', 'chart', 5, 1),
('病害统计', 'statistics:damage', 1, 19, '/statistics/damage', 'statistics/damage/index', 'chart', 1, 1),
('趋势分析', 'statistics:trend', 1, 19, '/statistics/trend', 'statistics/trend/index', 'trend-chart', 2, 1);

-- 初始化系统配置
INSERT INTO sys_config (config_key, config_value, config_type, description, status) VALUES
('ai_service_url', 'http://localhost:8001', 'string', 'AI服务地址', 1),
('upload_path', '/uploads', 'string', '文件上传路径', 1),
('max_upload_size', '50', 'number', '最大上传文件大小(MB)', 1),
('alarm_severe_threshold', '1', 'number', '严重病害告警阈值', 1),
('alarm_moderate_threshold', '3', 'number', '中等病害告警阈值', 1),
('alarm_area_ratio_threshold', '0.1', 'number', '病害面积占比告警阈值', 1);
