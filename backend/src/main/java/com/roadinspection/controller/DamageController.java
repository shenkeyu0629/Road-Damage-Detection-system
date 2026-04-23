package com.roadinspection.controller;

import cn.hutool.core.util.IdUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.roadinspection.common.Result;
import com.roadinspection.dto.DetectionRequestDTO;
import com.roadinspection.dto.DetectionResultDTO;
import com.roadinspection.dto.VideoDetectionRequestDTO;
import com.roadinspection.dto.VideoDetectionResultDTO;
import com.roadinspection.entity.AlarmInfo;
import com.roadinspection.entity.DamageInfo;
import com.roadinspection.entity.InspectionRecord;
import com.roadinspection.entity.Road;
import com.roadinspection.entity.RoadSection;
import com.roadinspection.mapper.AlarmInfoMapper;
import com.roadinspection.mapper.DamageInfoMapper;
import com.roadinspection.mapper.InspectionRecordMapper;
import com.roadinspection.mapper.RoadMapper;
import com.roadinspection.mapper.RoadSectionMapper;
import com.roadinspection.service.DetectionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Tag(name = "病害检测")
@RestController
@RequestMapping("/damage")
@RequiredArgsConstructor
public class DamageController {

    private final DetectionService detectionService;
    private final InspectionRecordMapper inspectionRecordMapper;
    private final DamageInfoMapper damageInfoMapper;
    private final AlarmInfoMapper alarmInfoMapper;
    private final RoadMapper roadMapper;
    private final RoadSectionMapper roadSectionMapper;
    
    private static final int DEDUP_POSITION_THRESHOLD = 50;
    private static final int DEDUP_TIME_THRESHOLD_MINUTES = 30;

    @Operation(summary = "执行病害检测")
    @PostMapping("/detect")
    public Result<DetectionResultDTO> detect(@RequestBody DetectionRequestDTO request) {
        DetectionResultDTO result = detectionService.detectImage(request);
        return Result.success(result);
    }

    @Operation(summary = "检测并保存记录")
    @PostMapping("/detect-and-save")
    public Result<InspectionRecord> detectAndSave(@RequestBody DetectionRequestDTO request) {
        DetectionResultDTO result = detectionService.detectImage(request);
        InspectionRecord record = detectionService.createInspectionRecord(result, request);
        return Result.success(record);
    }

    @Operation(summary = "执行视频病害检测")
    @PostMapping("/detect-video")
    public Result<VideoDetectionResultDTO> detectVideo(@RequestBody VideoDetectionRequestDTO request) {
        VideoDetectionResultDTO result = detectionService.detectVideo(request);
        return Result.success(result);
    }

    @Operation(summary = "视频检测并保存记录")
    @PostMapping("/detect-video-and-save")
    public Result<InspectionRecord> detectVideoAndSave(@RequestBody VideoDetectionRequestDTO request) {
        VideoDetectionResultDTO result = detectionService.detectVideo(request);
        InspectionRecord record = detectionService.createVideoInspectionRecord(result, request);
        return Result.success(record);
    }

    @Operation(summary = "分页查询巡检记录")
    @GetMapping("/record/list")
    public Result<Page<InspectionRecord>> recordList(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size,
            @RequestParam(required = false) Long roadId,
            @RequestParam(required = false) Long taskId,
            @RequestParam(required = false) String damageLevel) {
        
        Page<InspectionRecord> pageParam = new Page<>(page, size);
        LambdaQueryWrapper<InspectionRecord> wrapper = new LambdaQueryWrapper<>();
        
        if (roadId != null) {
            wrapper.eq(InspectionRecord::getRoadId, roadId);
        }
        if (taskId != null) {
            wrapper.eq(InspectionRecord::getTaskId, taskId);
        }
        if (damageLevel != null && !damageLevel.isEmpty()) {
            wrapper.eq(InspectionRecord::getDamageLevel, damageLevel);
        }
        wrapper.eq(InspectionRecord::getDeleted, 0);
        wrapper.orderByDesc(InspectionRecord::getInspectionTime);
        
        return Result.success(inspectionRecordMapper.selectPage(pageParam, wrapper));
    }

    @Operation(summary = "获取巡检记录详情")
    @GetMapping("/record/{id}")
    public Result<InspectionRecord> getRecordById(@PathVariable Long id) {
        return Result.success(inspectionRecordMapper.selectById(id));
    }

    @Operation(summary = "分页查询病害列表")
    @GetMapping("/list")
    public Result<Page<Map<String, Object>>> list(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size,
            @RequestParam(required = false) Long roadId,
            @RequestParam(required = false) String damageType,
            @RequestParam(required = false) String damageLevel,
            @RequestParam(required = false) String status) {
        
        Page<DamageInfo> pageParam = new Page<>(page, size);
        LambdaQueryWrapper<DamageInfo> wrapper = new LambdaQueryWrapper<>();
        
        if (roadId != null) {
            wrapper.eq(DamageInfo::getRoadId, roadId);
        }
        if (damageType != null && !damageType.isEmpty()) {
            wrapper.eq(DamageInfo::getDamageType, damageType);
        }
        if (damageLevel != null && !damageLevel.isEmpty()) {
            wrapper.eq(DamageInfo::getDamageLevel, damageLevel);
        }
        if (status != null && !status.isEmpty()) {
            wrapper.eq(DamageInfo::getStatus, status);
        }
        wrapper.eq(DamageInfo::getDeleted, 0);
        wrapper.orderByDesc(DamageInfo::getCreateTime);
        
        Page<DamageInfo> damagePage = damageInfoMapper.selectPage(pageParam, wrapper);
        
        Page<Map<String, Object>> resultPage = new Page<>(page, size);
        resultPage.setTotal(damagePage.getTotal());
        resultPage.setPages(damagePage.getPages());
        
        List<Map<String, Object>> records = new ArrayList<>();
        for (DamageInfo damage : damagePage.getRecords()) {
            Map<String, Object> record = new HashMap<>();
            record.put("id", damage.getId());
            record.put("roadId", damage.getRoadId());
            record.put("sectionId", damage.getSectionId());
            record.put("damageType", damage.getDamageType());
            record.put("damageLevel", damage.getDamageLevel());
            record.put("confidence", damage.getConfidence());
            record.put("area", damage.getArea());
            record.put("status", damage.getStatus());
            record.put("createTime", damage.getCreateTime());
            record.put("firstDetectedTime", damage.getFirstDetectedTime());
            record.put("lastDetectedTime", damage.getLastDetectedTime());
            record.put("detectionCount", damage.getDetectionCount());
            
            if (damage.getRoadId() != null) {
                Road road = roadMapper.selectById(damage.getRoadId());
                if (road != null) {
                    record.put("roadName", road.getRoadName());
                }
            }
            
            if (damage.getSectionId() != null) {
                RoadSection section = roadSectionMapper.selectById(damage.getSectionId());
                if (section != null) {
                    record.put("sectionName", section.getSectionName());
                }
            }
            
            records.add(record);
        }
        resultPage.setRecords(records);
        
        return Result.success(resultPage);
    }

    @Operation(summary = "获取病害详情")
    @GetMapping("/{id}")
    public Result<DamageInfo> getById(@PathVariable Long id) {
        return Result.success(damageInfoMapper.selectById(id));
    }

    @Operation(summary = "更新病害状态")
    @PutMapping("/{id}/status")
    public Result<Void> updateStatus(@PathVariable Long id, @RequestParam String status) {
        DamageInfo damage = damageInfoMapper.selectById(id);
        if (damage != null) {
            damage.setStatus(status);
            damageInfoMapper.updateById(damage);
        }
        return Result.success();
    }

    @Operation(summary = "获取记录下的病害列表")
    @GetMapping("/record/{recordId}/damages")
    public Result<List<DamageInfo>> getDamagesByRecord(@PathVariable Long recordId) {
        LambdaQueryWrapper<DamageInfo> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(DamageInfo::getRecordId, recordId);
        return Result.success(damageInfoMapper.selectList(wrapper));
    }

    @Operation(summary = "保存检测结果")
    @PostMapping("/save-result")
    public Result<InspectionRecord> saveDetectionResult(@RequestBody Map<String, Object> data) {
        Long taskId = data.get("taskId") != null ? Long.valueOf(data.get("taskId").toString()) : null;
        Long roadId = data.get("roadId") != null ? Long.valueOf(data.get("roadId").toString()) : null;
        Long sectionId = data.get("sectionId") != null ? Long.valueOf(data.get("sectionId").toString()) : null;
        String imagePath = (String) data.get("imagePath");
        String resultImagePath = (String) data.get("resultImagePath");
        List<Map<String, Object>> detections = (List<Map<String, Object>>) data.get("detections");
        Map<String, Object> summary = (Map<String, Object>) data.get("summary");
        
        InspectionRecord record = new InspectionRecord();
        record.setTaskId(taskId);
        record.setRoadId(roadId);
        record.setSectionId(sectionId);
        record.setImagePath(imagePath);
        record.setResultImagePath(resultImagePath);
        record.setInspectionTime(LocalDateTime.now());
        
        int totalCount = detections != null ? detections.size() : 0;
        int severeCount = 0;
        int moderateCount = 0;
        int minorCount = 0;
        
        if (detections != null) {
            for (Map<String, Object> det : detections) {
                String level = (String) det.get("level");
                if ("severe".equals(level)) severeCount++;
                else if ("moderate".equals(level)) moderateCount++;
                else minorCount++;
            }
        }
        
        record.setDamageCount(totalCount);
        record.setDamageLevel(severeCount > 0 ? "严重" : (moderateCount > 0 ? "中等" : "轻微"));
        inspectionRecordMapper.insert(record);
        
        if (detections != null) {
            for (int i = 0; i < detections.size(); i++) {
                Map<String, Object> det = detections.get(i);
                DamageInfo damage = new DamageInfo();
                damage.setRecordId(record.getId());
                damage.setTaskId(taskId);
                damage.setRoadId(roadId);
                damage.setSectionId(sectionId);
                damage.setDamageType((String) det.get("class_name"));
                damage.setDamageLevel("severe".equals(det.get("level")) ? "严重" : 
                    ("moderate".equals(det.get("level")) ? "中等" : "轻微"));
                damage.setConfidence(Double.valueOf(det.get("confidence").toString()));
                damage.setImagePath(imagePath);
                damage.setResultImagePath(resultImagePath);
                
                Map<String, Object> bbox = (Map<String, Object>) det.get("bbox");
                if (bbox != null) {
                    damage.setPositionX(Double.valueOf(bbox.get("x1").toString()).intValue());
                    damage.setPositionY(Double.valueOf(bbox.get("y1").toString()).intValue());
                    damage.setWidth(Double.valueOf(bbox.get("x2").toString()).intValue() - damage.getPositionX());
                    damage.setHeight(Double.valueOf(bbox.get("y2").toString()).intValue() - damage.getPositionY());
                }
                
                damage.setStatus("pending");
                damageInfoMapper.insert(damage);
            }
        }
        
        if (severeCount > 0) {
            AlarmInfo alarm = new AlarmInfo();
            alarm.setAlarmCode("ALM" + IdUtil.getSnowflakeNextIdStr());
            alarm.setTaskId(taskId);
            alarm.setRoadId(roadId);
            alarm.setSectionId(sectionId);
            alarm.setAlarmType("damage_detected");
            alarm.setAlarmLevel("severe");
            alarm.setAlarmReason(String.format("检测到%d处严重病害，%d处中等病害，%d处轻微病害", severeCount, moderateCount, minorCount));
            alarm.setImagePath(imagePath);
            alarm.setResultImagePath(resultImagePath);
            alarm.setStatus("pending");
            alarmInfoMapper.insert(alarm);
        }
        
        return Result.success(record);
    }
    
    @Operation(summary = "实时监控保存病害（带去重）")
    @PostMapping("/save-realtime")
    public Result<Map<String, Object>> saveRealtimeDetection(@RequestBody Map<String, Object> data) {
        try {
            Long roadId = data.get("roadId") != null ? Long.valueOf(data.get("roadId").toString()) : null;
            Long sectionId = data.get("sectionId") != null ? Long.valueOf(data.get("sectionId").toString()) : null;
            Long taskId = data.get("taskId") != null ? Long.valueOf(data.get("taskId").toString()) : null;
            String videoSource = (String) data.get("videoSource");
            List<Map<String, Object>> detections = (List<Map<String, Object>>) data.get("detections");
            
            int newCount = 0;
            int duplicateCount = 0;
            int severeCount = 0;
            int moderateCount = 0;
            int minorCount = 0;
            
            if (detections != null) {
                for (Map<String, Object> det : detections) {
                    String className = (String) det.get("class_name");
                    String level = (String) det.get("level");
                    Double confidence = Double.valueOf(det.get("confidence").toString());
                    
                    if ("severe".equals(level)) severeCount++;
                    else if ("moderate".equals(level)) moderateCount++;
                    else minorCount++;
                    
                    Map<String, Object> bbox = (Map<String, Object>) det.get("bbox");
                    int x1 = bbox != null ? Double.valueOf(bbox.get("x1").toString()).intValue() : 0;
                    int y1 = bbox != null ? Double.valueOf(bbox.get("y1").toString()).intValue() : 0;
                    int x2 = bbox != null ? Double.valueOf(bbox.get("x2").toString()).intValue() : 0;
                    int y2 = bbox != null ? Double.valueOf(bbox.get("y2").toString()).intValue() : 0;
                    
                    DamageInfo existingDamage = null;
                    if (taskId != null) {
                        existingDamage = findTaskDamage(taskId, className, x1, y1);
                    } else {
                        existingDamage = findDailyDamage(roadId, sectionId, className);
                    }
                    
                    if (existingDamage != null) {
                        existingDamage.setLastDetectedTime(LocalDateTime.now());
                        existingDamage.setDetectionCount(existingDamage.getDetectionCount() != null ? existingDamage.getDetectionCount() + 1 : 1);
                        if (confidence > existingDamage.getConfidence()) {
                            existingDamage.setConfidence(confidence);
                        }
                        damageInfoMapper.updateById(existingDamage);
                        duplicateCount++;
                    } else {
                        String dedupHash = taskId != null 
                            ? generateTaskDedupHash(taskId, className, x1, y1)
                            : generateDailyDedupHash(roadId, sectionId, className);
                        
                        DamageInfo damage = new DamageInfo();
                        damage.setTaskId(taskId);
                        damage.setRoadId(roadId);
                        damage.setSectionId(sectionId);
                        damage.setDamageType(className);
                        damage.setDamageLevel("severe".equals(level) ? "严重" : 
                            ("moderate".equals(level) ? "中等" : "轻微"));
                        damage.setConfidence(confidence);
                        damage.setBboxX1(x1);
                        damage.setBboxY1(y1);
                        damage.setBboxX2(x2);
                        damage.setBboxY2(y2);
                        damage.setArea((x2 - x1) * (y2 - y1));
                        damage.setDeduplicationHash(dedupHash);
                        damage.setFirstDetectedTime(LocalDateTime.now());
                        damage.setLastDetectedTime(LocalDateTime.now());
                        damage.setDetectionCount(1);
                        damage.setStatus("pending");
                        damageInfoMapper.insert(damage);
                        newCount++;
                        
                        if ("severe".equals(level)) {
                            createAlarm(roadId, sectionId, className, confidence, videoSource);
                        }
                    }
                }
            }
            
            if (taskId != null) {
                updateInspectionRecordStats(taskId, detections != null ? detections.size() : 0, severeCount, moderateCount, minorCount);
            }
            
            return Result.success(Map.of(
                "newCount", newCount,
                "duplicateCount", duplicateCount,
                "totalProcessed", detections != null ? detections.size() : 0
            ));
        } catch (Exception e) {
            log.error("保存实时检测结果失败: ", e);
            return Result.success(Map.of(
                "newCount", 0,
                "duplicateCount", 0,
                "totalProcessed", 0,
                "error", e.getMessage()
            ));
        }
    }
    
    private DamageInfo findTaskDamage(Long taskId, String damageType, int x1, int y1) {
        int positionThreshold = 50;
        
        LambdaQueryWrapper<DamageInfo> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(DamageInfo::getTaskId, taskId);
        wrapper.eq(DamageInfo::getDamageType, damageType);
        wrapper.eq(DamageInfo::getDeleted, 0);
        
        List<DamageInfo> results = damageInfoMapper.selectList(wrapper);
        
        for (DamageInfo damage : results) {
            if (damage.getBboxX1() != null && damage.getBboxY1() != null) {
                int dx = Math.abs(damage.getBboxX1() - x1);
                int dy = Math.abs(damage.getBboxY1() - y1);
                if (dx < positionThreshold && dy < positionThreshold) {
                    return damage;
                }
            }
        }
        
        return null;
    }
    
    private String generateTaskDedupHash(Long taskId, String damageType, int x1, int y1) {
        int gridX = x1 / 50;
        int gridY = y1 / 50;
        return String.format("task_%d_%s_%d_%d", taskId, damageType, gridX, gridY);
    }
    
    private void updateInspectionRecordStats(Long taskId, int totalCount, int severeCount, int moderateCount, int minorCount) {
        LambdaQueryWrapper<InspectionRecord> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(InspectionRecord::getTaskId, taskId);
        wrapper.orderByDesc(InspectionRecord::getCreateTime);
        wrapper.last("LIMIT 1");
        
        InspectionRecord record = inspectionRecordMapper.selectOne(wrapper);
        if (record != null) {
            LambdaQueryWrapper<DamageInfo> damageWrapper = new LambdaQueryWrapper<>();
            damageWrapper.eq(DamageInfo::getTaskId, taskId);
            Long totalDamages = damageInfoMapper.selectCount(damageWrapper);
            
            record.setDamageCount(totalDamages.intValue());
            if (severeCount > 0) {
                record.setDamageLevel("严重");
            } else if (moderateCount > 0) {
                record.setDamageLevel("中等");
            } else if (minorCount > 0) {
                record.setDamageLevel("轻微");
            }
            record.setStatus("completed");
            inspectionRecordMapper.updateById(record);
        }
    }
    
    @Operation(summary = "获取去重后的病害统计")
    @GetMapping("/statistics/deduplicated")
    public Result<Map<String, Object>> getDeduplicatedStatistics(
            @RequestParam(required = false) Long roadId,
            @RequestParam(required = false) String damageType,
            @RequestParam(required = false) String damageLevel,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate) {
        
        LambdaQueryWrapper<DamageInfo> wrapper = new LambdaQueryWrapper<>();
        
        if (roadId != null) {
            wrapper.eq(DamageInfo::getRoadId, roadId);
        }
        if (damageType != null && !damageType.isEmpty()) {
            wrapper.eq(DamageInfo::getDamageType, damageType);
        }
        if (damageLevel != null && !damageLevel.isEmpty()) {
            wrapper.eq(DamageInfo::getDamageLevel, damageLevel);
        }
        if (startDate != null && !startDate.isEmpty()) {
            wrapper.ge(DamageInfo::getFirstDetectedTime, LocalDateTime.parse(startDate + "T00:00:00"));
        }
        if (endDate != null && !endDate.isEmpty()) {
            wrapper.le(DamageInfo::getFirstDetectedTime, LocalDateTime.parse(endDate + "T23:59:59"));
        }
        wrapper.eq(DamageInfo::getDeleted, 0);
        wrapper.orderByDesc(DamageInfo::getLastDetectedTime);
        
        List<DamageInfo> damages = damageInfoMapper.selectList(wrapper);
        
        int totalCount = damages.size();
        int severeCount = 0;
        int moderateCount = 0;
        int minorCount = 0;
        int potholeCount = 0;
        int crackCount = 0;
        double avgConfidence = 0;
        int totalDetectionCount = 0;
        
        for (DamageInfo d : damages) {
            if ("严重".equals(d.getDamageLevel())) severeCount++;
            else if ("中等".equals(d.getDamageLevel())) moderateCount++;
            else minorCount++;
            
            if ("坑洞".equals(d.getDamageType())) potholeCount++;
            else if ("裂缝".equals(d.getDamageType())) crackCount++;
            
            avgConfidence += d.getConfidence();
            totalDetectionCount += d.getDetectionCount() != null ? d.getDetectionCount() : 1;
        }
        
        if (totalCount > 0) {
            avgConfidence /= totalCount;
        }
        
        return Result.success(Map.of(
            "totalUniqueDamages", totalCount,
            "totalDetections", totalDetectionCount,
            "byLevel", Map.of("严重", severeCount, "中等", moderateCount, "轻微", minorCount),
            "byType", Map.of("坑洞", potholeCount, "裂缝", crackCount),
            "averageConfidence", Math.round(avgConfidence * 1000) / 10.0,
            "damages", damages
        ));
    }
    
    private String generateDailyDedupHash(Long roadId, Long sectionId, String damageType) {
        try {
            String today = LocalDateTime.now().toLocalDate().toString();
            String data = String.format("%d_%d_%s_%s", 
                roadId != null ? roadId : 0, 
                sectionId != null ? sectionId : 0, 
                damageType, today);
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] hash = md.digest(data.getBytes(StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (Exception e) {
            return String.valueOf(System.currentTimeMillis());
        }
    }
    
    private DamageInfo findDailyDamage(Long roadId, Long sectionId, String damageType) {
        LocalDateTime todayStart = LocalDateTime.now().toLocalDate().atStartOfDay();
        LocalDateTime todayEnd = todayStart.plusDays(1);
        
        LambdaQueryWrapper<DamageInfo> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(DamageInfo::getRoadId, roadId);
        wrapper.eq(DamageInfo::getDamageType, damageType);
        wrapper.ge(DamageInfo::getFirstDetectedTime, todayStart);
        wrapper.lt(DamageInfo::getFirstDetectedTime, todayEnd);
        wrapper.eq(DamageInfo::getDeleted, 0);
        
        if (sectionId != null) {
            wrapper.eq(DamageInfo::getSectionId, sectionId);
        }
        
        List<DamageInfo> results = damageInfoMapper.selectList(wrapper);
        return results.isEmpty() ? null : results.get(0);
    }
    
    private void createAlarm(Long roadId, Long sectionId, String damageType, Double confidence, String videoSource) {
        AlarmInfo alarm = new AlarmInfo();
        alarm.setAlarmCode("ALM" + IdUtil.getSnowflakeNextIdStr());
        alarm.setRoadId(roadId);
        alarm.setSectionId(sectionId);
        alarm.setAlarmType("realtime_damage");
        alarm.setAlarmLevel("severe");
        alarm.setAlarmReason(String.format("实时监控检测到%s病害，置信度%.1f%%", damageType, confidence * 100));
        alarm.setStatus("pending");
        alarmInfoMapper.insert(alarm);
    }
}
