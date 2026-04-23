package com.roadinspection.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.roadinspection.entity.*;
import com.roadinspection.mapper.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class DetectionSyncService {

    private final InspectionRecordMapper recordMapper;
    private final DamageInfoMapper damageInfoMapper;
    private final AlarmInfoMapper alarmInfoMapper;

    @Transactional
    public Long syncDetectionResults(Long taskId, Long roadId, Long sectionId, 
                                      String imagePath, List<Map<String, Object>> damages) {
        InspectionRecord record = new InspectionRecord();
        record.setTaskId(taskId);
        record.setRoadId(roadId);
        record.setSectionId(sectionId);
        record.setImagePath(imagePath);
        record.setDamageCount(damages.size());
        record.setInspectionTime(LocalDateTime.now());
        record.setStatus("completed");
        recordMapper.insert(record);
        
        for (Map<String, Object> damage : damages) {
            DamageInfo result = new DamageInfo();
            result.setRecordId(record.getId());
            result.setTaskId(taskId);
            result.setRoadId(roadId);
            result.setSectionId(sectionId);
            result.setDamageType((String) damage.get("type"));
            result.setDamageLevel((String) damage.get("level"));
            result.setConfidence((Double) damage.get("confidence"));
            
            Object x = damage.get("x");
            Object y = damage.get("y");
            Object width = damage.get("width");
            Object height = damage.get("height");
            Object area = damage.get("area");
            
            if (x != null) result.setPositionX(((Number) x).intValue());
            if (y != null) result.setPositionY(((Number) y).intValue());
            if (width != null) result.setWidth(((Number) width).intValue());
            if (height != null) result.setHeight(((Number) height).intValue());
            if (area != null) result.setArea(((Number) area).intValue());
            
            result.setImagePath(imagePath);
            result.setStatus("detected");
            result.setCreateTime(LocalDateTime.now());
            damageInfoMapper.insert(result);
            
            if ("严重".equals(result.getDamageLevel()) || "severe".equals(result.getDamageLevel())) {
                createAlarm(result);
            }
        }
        
        log.info("同步检测结果完成: 记录ID={}, 病害数={}", record.getId(), damages.size());
        return record.getId();
    }

    private void createAlarm(DamageInfo damage) {
        AlarmInfo alarm = new AlarmInfo();
        alarm.setRoadId(damage.getRoadId());
        alarm.setSectionId(damage.getSectionId());
        alarm.setDamageId(damage.getId());
        alarm.setAlarmType(damage.getDamageType());
        alarm.setAlarmLevel("严重".equals(damage.getDamageLevel()) ? "high" : "medium");
        alarm.setStatus("pending");
        alarm.setCreateTime(LocalDateTime.now());
        alarmInfoMapper.insert(alarm);
        
        log.info("创建告警: 道路ID={}, 病害类型={}", damage.getRoadId(), damage.getDamageType());
    }

    public Map<String, Object> getDashboardStats() {
        Map<String, Object> stats = new HashMap<>();
        
        stats.put("totalDamages", damageInfoMapper.selectCount(null));
        stats.put("pendingAlarms", alarmInfoMapper.selectCount(
            new LambdaQueryWrapper<AlarmInfo>().eq(AlarmInfo::getStatus, "pending")
        ));
        stats.put("todayRecords", recordMapper.selectCount(
            new LambdaQueryWrapper<InspectionRecord>()
                .ge(InspectionRecord::getInspectionTime, LocalDateTime.now().toLocalDate().atStartOfDay())
        ));
        
        return stats;
    }
}
