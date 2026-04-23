package com.roadinspection.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.roadinspection.dto.StatisticsQueryDTO;
import com.roadinspection.entity.DamageInfo;
import com.roadinspection.entity.DamageStatistics;
import com.roadinspection.entity.InspectionRecord;
import com.roadinspection.entity.Road;
import com.roadinspection.mapper.DamageInfoMapper;
import com.roadinspection.mapper.DamageStatisticsMapper;
import com.roadinspection.mapper.InspectionRecordMapper;
import com.roadinspection.mapper.RoadMapper;
import com.roadinspection.service.StatisticsService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Service
@RequiredArgsConstructor
public class StatisticsServiceImpl extends ServiceImpl<DamageStatisticsMapper, DamageStatistics> implements StatisticsService {

    private final InspectionRecordMapper inspectionRecordMapper;
    private final DamageInfoMapper damageInfoMapper;
    private final RoadMapper roadMapper;

    @Override
    public Map<String, Object> getDamageStatistics(StatisticsQueryDTO query) {
        Map<String, Object> result = new HashMap<>();
        
        LambdaQueryWrapper<DamageInfo> wrapper = new LambdaQueryWrapper<>();
        
        if (query.getStartDate() != null) {
            wrapper.ge(DamageInfo::getCreateTime, query.getStartDate().atStartOfDay());
        }
        if (query.getEndDate() != null) {
            wrapper.le(DamageInfo::getCreateTime, query.getEndDate().atTime(LocalTime.MAX));
        }
        if (query.getRoadId() != null) {
            wrapper.eq(DamageInfo::getRoadId, query.getRoadId());
        }
        if (query.getSectionId() != null) {
            wrapper.eq(DamageInfo::getSectionId, query.getSectionId());
        }
        
        List<DamageInfo> damages = damageInfoMapper.selectList(wrapper);
        
        int total = damages.size();
        int minor = 0, moderate = 0, severe = 0;
        Map<String, Integer> byType = new HashMap<>();
        Map<String, Integer> byLevel = new HashMap<>();
        
        for (DamageInfo damage : damages) {
            String level = damage.getDamageLevel();
            if ("轻微".equals(level) || "minor".equals(level)) minor++;
            else if ("中等".equals(level) || "moderate".equals(level)) moderate++;
            else if ("严重".equals(level) || "severe".equals(level)) severe++;
            
            String type = damage.getDamageType();
            if (type != null) {
                byType.put(type, byType.getOrDefault(type, 0) + 1);
            }
        }
        
        byLevel.put("minor", minor);
        byLevel.put("moderate", moderate);
        byLevel.put("severe", severe);
        
        result.put("totalCount", total);
        result.put("byLevel", byLevel);
        result.put("byType", byType);
        
        List<Map<String, Object>> byRoad = getDamageByRoad(query);
        result.put("byRoad", byRoad);
        
        List<Map<String, Object>> trend = getTrendData(query);
        result.put("trend", trend);
        
        return result;
    }
    
    private List<Map<String, Object>> getDamageByRoad(StatisticsQueryDTO query) {
        List<Map<String, Object>> result = new ArrayList<>();
        
        LambdaQueryWrapper<DamageInfo> wrapper = new LambdaQueryWrapper<>();
        if (query.getStartDate() != null) {
            wrapper.ge(DamageInfo::getCreateTime, query.getStartDate().atStartOfDay());
        }
        if (query.getEndDate() != null) {
            wrapper.le(DamageInfo::getCreateTime, query.getEndDate().atTime(LocalTime.MAX));
        }
        
        List<DamageInfo> damages = damageInfoMapper.selectList(wrapper);
        
        Map<Long, Map<String, Integer>> roadStats = new HashMap<>();
        for (DamageInfo damage : damages) {
            Long roadId = damage.getRoadId();
            if (roadId == null) continue;
            
            Map<String, Integer> stats = roadStats.computeIfAbsent(roadId, k -> {
                Map<String, Integer> m = new HashMap<>();
                m.put("total", 0);
                m.put("minor", 0);
                m.put("moderate", 0);
                m.put("severe", 0);
                return m;
            });
            
            stats.put("total", stats.get("total") + 1);
            String level = damage.getDamageLevel();
            if ("轻微".equals(level) || "minor".equals(level)) {
                stats.put("minor", stats.get("minor") + 1);
            } else if ("中等".equals(level) || "moderate".equals(level)) {
                stats.put("moderate", stats.get("moderate") + 1);
            } else if ("严重".equals(level) || "severe".equals(level)) {
                stats.put("severe", stats.get("severe") + 1);
            }
        }
        
        List<Road> roads = roadMapper.selectList(null);
        Map<Long, String> roadNameMap = new HashMap<>();
        for (Road road : roads) {
            roadNameMap.put(road.getId(), road.getRoadName());
        }
        
        for (Map.Entry<Long, Map<String, Integer>> entry : roadStats.entrySet()) {
            Map<String, Object> item = new HashMap<>();
            item.put("roadId", entry.getKey());
            item.put("roadName", roadNameMap.getOrDefault(entry.getKey(), "未知道路"));
            item.put("totalDamages", entry.getValue().get("total"));
            item.put("minorCount", entry.getValue().get("minor"));
            item.put("moderateCount", entry.getValue().get("moderate"));
            item.put("severeCount", entry.getValue().get("severe"));
            
            LambdaQueryWrapper<InspectionRecord> recordWrapper = new LambdaQueryWrapper<>();
            recordWrapper.eq(InspectionRecord::getRoadId, entry.getKey());
            Long inspectionCount = inspectionRecordMapper.selectCount(recordWrapper);
            item.put("inspectionCount", inspectionCount);
            
            int healthIndex = 100;
            int total = entry.getValue().get("total");
            if (total > 0) {
                int severeWeight = entry.getValue().get("severe") * 10;
                int moderateWeight = entry.getValue().get("moderate") * 5;
                int minorWeight = entry.getValue().get("minor") * 2;
                healthIndex = Math.max(0, 100 - (severeWeight + moderateWeight + minorWeight) / Math.max(total, 1));
            }
            item.put("healthIndex", healthIndex);
            
            result.add(item);
        }
        
        result.sort((a, b) -> (Integer) b.get("totalDamages") - (Integer) a.get("totalDamages"));
        
        return result;
    }
    
    private List<Map<String, Object>> getTrendData(StatisticsQueryDTO query) {
        List<Map<String, Object>> trend = new ArrayList<>();
        
        LocalDate startDate = query.getStartDate() != null ? query.getStartDate() : LocalDate.now().minusDays(30);
        LocalDate endDate = query.getEndDate() != null ? query.getEndDate() : LocalDate.now();
        
        LambdaQueryWrapper<DamageInfo> wrapper = new LambdaQueryWrapper<>();
        wrapper.ge(DamageInfo::getCreateTime, startDate.atStartOfDay());
        wrapper.le(DamageInfo::getCreateTime, endDate.atTime(LocalTime.MAX));
        if (query.getRoadId() != null) {
            wrapper.eq(DamageInfo::getRoadId, query.getRoadId());
        }
        
        List<DamageInfo> damages = damageInfoMapper.selectList(wrapper);
        
        Map<LocalDate, Integer> countByDate = new TreeMap<>();
        for (DamageInfo damage : damages) {
            LocalDate date = damage.getCreateTime().toLocalDate();
            countByDate.put(date, countByDate.getOrDefault(date, 0) + 1);
        }
        
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MM-dd");
        for (Map.Entry<LocalDate, Integer> entry : countByDate.entrySet()) {
            Map<String, Object> item = new HashMap<>();
            item.put("date", entry.getKey().format(formatter));
            item.put("count", entry.getValue());
            trend.add(item);
        }
        
        return trend;
    }

    @Override
    public List<Map<String, Object>> getDamageTrend(StatisticsQueryDTO query) {
        return getTrendData(query);
    }

    @Override
    public Map<String, Object> getDamageDistribution(StatisticsQueryDTO query) {
        Map<String, Object> distribution = new HashMap<>();
        
        LambdaQueryWrapper<DamageInfo> wrapper = new LambdaQueryWrapper<>();
        
        if (query.getStartDate() != null) {
            wrapper.ge(DamageInfo::getCreateTime, query.getStartDate().atStartOfDay());
        }
        if (query.getEndDate() != null) {
            wrapper.le(DamageInfo::getCreateTime, query.getEndDate().atTime(LocalTime.MAX));
        }
        
        List<DamageInfo> damages = damageInfoMapper.selectList(wrapper);
        
        Map<String, Integer> byType = new HashMap<>();
        Map<String, Integer> byLevel = new HashMap<>();
        Map<Long, Integer> byRoad = new HashMap<>();
        
        for (DamageInfo damage : damages) {
            if (damage.getDamageType() != null) {
                byType.merge(damage.getDamageType(), 1, Integer::sum);
            }
            if (damage.getDamageLevel() != null) {
                byLevel.merge(damage.getDamageLevel(), 1, Integer::sum);
            }
            if (damage.getRoadId() != null) {
                byRoad.merge(damage.getRoadId(), 1, Integer::sum);
            }
        }
        
        distribution.put("byType", byType);
        distribution.put("byLevel", byLevel);
        distribution.put("byRoad", byRoad);
        
        return distribution;
    }

    @Override
    public void generateDailyStatistics() {
        LocalDate yesterday = LocalDate.now().minusDays(1);
        
        LocalDateTime startOfDay = yesterday.atStartOfDay();
        LocalDateTime endOfDay = yesterday.atTime(LocalTime.MAX);
        
        List<InspectionRecord> records = inspectionRecordMapper.selectList(
            new LambdaQueryWrapper<InspectionRecord>()
                .between(InspectionRecord::getInspectionTime, startOfDay, endOfDay)
        );
        
        Map<Long, List<InspectionRecord>> byRoad = new HashMap<>();
        for (InspectionRecord record : records) {
            if (record.getRoadId() != null) {
                byRoad.computeIfAbsent(record.getRoadId(), k -> new ArrayList<>()).add(record);
            }
        }
        
        for (Map.Entry<Long, List<InspectionRecord>> entry : byRoad.entrySet()) {
            Long roadId = entry.getKey();
            List<InspectionRecord> roadRecords = entry.getValue();
            
            DamageStatistics stats = new DamageStatistics();
            stats.setStatDate(yesterday);
            stats.setStatType("daily");
            stats.setRoadId(roadId);
            stats.setTotalInspectionCount(roadRecords.size());
            
            int totalDamage = 0, minor = 0, moderate = 0, severe = 0;
            
            for (InspectionRecord record : roadRecords) {
                totalDamage += record.getDamageCount() != null ? record.getDamageCount() : 0;
                String level = record.getDamageLevel();
                if ("轻微".equals(level) || "minor".equals(level)) minor++;
                else if ("中等".equals(level) || "moderate".equals(level)) moderate++;
                else if ("严重".equals(level) || "severe".equals(level)) severe++;
            }
            
            stats.setTotalDamageCount(totalDamage);
            stats.setMinorCount(minor);
            stats.setModerateCount(moderate);
            stats.setSevereCount(severe);
            
            save(stats);
        }
    }
}
