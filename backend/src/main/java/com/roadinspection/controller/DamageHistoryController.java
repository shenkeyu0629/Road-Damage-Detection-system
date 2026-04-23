package com.roadinspection.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.roadinspection.common.Result;
import com.roadinspection.entity.DamageDisposal;
import com.roadinspection.entity.DamageHistory;
import com.roadinspection.mapper.DamageDisposalMapper;
import com.roadinspection.mapper.DamageHistoryMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Tag(name = "历史病害统计")
@RestController
@RequestMapping("/damage-history")
@RequiredArgsConstructor
public class DamageHistoryController {

    private final DamageHistoryMapper damageHistoryMapper;
    private final DamageDisposalMapper damageDisposalMapper;

    @Operation(summary = "分页查询历史病害统计列表")
    @GetMapping("/list")
    public Result<Page<DamageHistory>> list(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size,
            @RequestParam(required = false) Long roadId,
            @RequestParam(required = false) Long sectionId,
            @RequestParam(required = false) String damageLevel,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate) {
        
        Page<DamageHistory> pageParam = new Page<>(page, size);
        LambdaQueryWrapper<DamageHistory> wrapper = new LambdaQueryWrapper<>();
        
        if (roadId != null) {
            wrapper.eq(DamageHistory::getRoadId, roadId);
        }
        if (sectionId != null) {
            wrapper.eq(DamageHistory::getSectionId, sectionId);
        }
        if (damageLevel != null && !damageLevel.isEmpty()) {
            wrapper.eq(DamageHistory::getDamageLevel, damageLevel);
        }
        if (status != null && !status.isEmpty()) {
            wrapper.eq(DamageHistory::getStatus, status);
        }
        if (startDate != null && !startDate.isEmpty()) {
            wrapper.ge(DamageHistory::getDetectionTime, LocalDateTime.parse(startDate + "T00:00:00"));
        }
        if (endDate != null && !endDate.isEmpty()) {
            wrapper.le(DamageHistory::getDetectionTime, LocalDateTime.parse(endDate + "T23:59:59"));
        }
        
        wrapper.eq(DamageHistory::getDeleted, 0);
        wrapper.orderByDesc(DamageHistory::getDetectionTime);
        
        return Result.success(damageHistoryMapper.selectPage(pageParam, wrapper));
    }

    @Operation(summary = "获取历史病害详情")
    @GetMapping("/{id}")
    public Result<DamageHistory> getById(@PathVariable Long id) {
        return Result.success(damageHistoryMapper.selectById(id));
    }

    @Operation(summary = "新增历史病害记录（带去重）")
    @PostMapping
    public Result<Void> save(@RequestBody DamageHistory record) {
        if (record.getDetectionTime() == null) {
            record.setDetectionTime(LocalDateTime.now());
        }
        if (record.getStatus() == null) {
            record.setStatus("未处理");
        }
        
        DamageHistory existingRecord = findTodayRecord(record.getRoadId(), record.getSectionId());
        
        if (existingRecord != null) {
            if ("已处理".equals(existingRecord.getStatus())) {
                return Result.success();
            }
            existingRecord.setDamageCount(record.getDamageCount());
            existingRecord.setDamageLevel(getHigherLevel(existingRecord.getDamageLevel(), record.getDamageLevel()));
            existingRecord.setDamageTypes(mergeDamageTypes(existingRecord.getDamageTypes(), record.getDamageTypes()));
            existingRecord.setDetectionTime(LocalDateTime.now());
            damageHistoryMapper.updateById(existingRecord);
        } else {
            damageHistoryMapper.insert(record);
        }
        return Result.success();
    }
    
    private DamageHistory findTodayRecord(Long roadId, Long sectionId) {
        LocalDate today = LocalDate.now();
        LocalDateTime todayStart = today.atStartOfDay();
        LocalDateTime todayEnd = today.plusDays(1).atStartOfDay();
        
        LambdaQueryWrapper<DamageHistory> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(DamageHistory::getDeleted, 0);
        wrapper.ge(DamageHistory::getDetectionTime, todayStart);
        wrapper.lt(DamageHistory::getDetectionTime, todayEnd);
        
        if (roadId != null) {
            wrapper.eq(DamageHistory::getRoadId, roadId);
        } else {
            wrapper.isNull(DamageHistory::getRoadId);
        }
        
        if (sectionId != null) {
            wrapper.eq(DamageHistory::getSectionId, sectionId);
        } else {
            wrapper.isNull(DamageHistory::getSectionId);
        }
        
        wrapper.last("LIMIT 1");
        
        List<DamageHistory> records = damageHistoryMapper.selectList(wrapper);
        return records.isEmpty() ? null : records.get(0);
    }
    
    private String getHigherLevel(String existingLevel, String newLevel) {
        if ("严重".equals(existingLevel) || "严重".equals(newLevel)) {
            return "严重";
        }
        return "轻微";
    }
    
    private String mergeDamageTypes(String existingTypes, String newTypes) {
        if (existingTypes == null || existingTypes.isEmpty()) {
            return newTypes;
        }
        if (newTypes == null || newTypes.isEmpty()) {
            return existingTypes;
        }
        
        Set<String> typeSet = new java.util.HashSet<>();
        for (String type : existingTypes.split("、")) {
            typeSet.add(type.trim());
        }
        for (String type : newTypes.split("、")) {
            typeSet.add(type.trim());
        }
        
        return String.join("、", typeSet);
    }

    @Operation(summary = "更新历史病害记录")
    @PutMapping
    public Result<Void> update(@RequestBody DamageHistory record) {
        damageHistoryMapper.updateById(record);
        return Result.success();
    }

    @Operation(summary = "更新病害状态")
    @PutMapping("/{id}/status")
    public Result<Void> updateStatus(@PathVariable Long id, @RequestParam String status) {
        DamageHistory record = damageHistoryMapper.selectById(id);
        if (record != null) {
            String oldStatus = record.getStatus();
            record.setStatus(status);
            damageHistoryMapper.updateById(record);
            
            if ("已处理".equals(status) && !"已处理".equals(oldStatus)) {
                createDisposalRecord(record);
            }
        }
        return Result.success();
    }

    @Operation(summary = "处置病害（更新状态和处置方法）")
    @PutMapping("/{id}/dispose")
    public Result<Void> dispose(@PathVariable Long id, @RequestBody Map<String, String> params) {
        String disposalMethod = params.get("disposalMethod");
        
        DamageHistory record = damageHistoryMapper.selectById(id);
        if (record != null) {
            String oldStatus = record.getStatus();
            record.setDisposalMethod(disposalMethod);
            record.setStatus("已处理");
            damageHistoryMapper.updateById(record);
            
            if (!"已处理".equals(oldStatus)) {
                createDisposalRecord(record);
            }
        }
        return Result.success();
    }

    @Operation(summary = "删除历史病害记录")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        damageHistoryMapper.deleteById(id);
        return Result.success();
    }

    @Operation(summary = "获取统计数据")
    @GetMapping("/statistics")
    public Result<Map<String, Object>> getStatistics(
            @RequestParam(required = false) Long roadId,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate) {
        
        LambdaQueryWrapper<DamageHistory> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(DamageHistory::getDeleted, 0);
        
        if (roadId != null) {
            wrapper.eq(DamageHistory::getRoadId, roadId);
        }
        if (startDate != null && !startDate.isEmpty()) {
            wrapper.ge(DamageHistory::getDetectionTime, LocalDateTime.parse(startDate + "T00:00:00"));
        }
        if (endDate != null && !endDate.isEmpty()) {
            wrapper.le(DamageHistory::getDetectionTime, LocalDateTime.parse(endDate + "T23:59:59"));
        }
        
        List<DamageHistory> allRecords = damageHistoryMapper.selectList(wrapper);
        
        long totalRecords = allRecords.size();
        long totalDamages = allRecords.stream()
                .mapToLong(r -> r.getDamageCount() != null ? r.getDamageCount() : 0)
                .sum();
        long severeCount = allRecords.stream()
                .filter(r -> "严重".equals(r.getDamageLevel()))
                .count();
        long untreatedCount = allRecords.stream()
                .filter(r -> "未处理".equals(r.getStatus()))
                .count();
        long processingCount = allRecords.stream()
                .filter(r -> "处理中".equals(r.getStatus()))
                .count();
        long processedCount = allRecords.stream()
                .filter(r -> "已处理".equals(r.getStatus()))
                .count();
        
        Map<String, Object> stats = new HashMap<>();
        stats.put("totalRecords", totalRecords);
        stats.put("totalDamages", totalDamages);
        stats.put("severeCount", severeCount);
        stats.put("untreatedCount", untreatedCount);
        stats.put("processingCount", processingCount);
        stats.put("processedCount", processedCount);
        
        return Result.success(stats);
    }

    private void createDisposalRecord(DamageHistory history) {
        DamageDisposal disposal = new DamageDisposal();
        disposal.setDamageHistoryId(history.getId());
        disposal.setRoadId(history.getRoadId());
        disposal.setSectionId(history.getSectionId());
        disposal.setRoadName(history.getRoadName());
        disposal.setSectionName(history.getSectionName());
        disposal.setDamageCount(history.getDamageCount());
        disposal.setDamageLevel(history.getDamageLevel());
        disposal.setDamageTypes(history.getDamageTypes());
        disposal.setDisposalMethod(history.getDisposalMethod());
        disposal.setDisposalTime(LocalDateTime.now());
        damageDisposalMapper.insert(disposal);
    }
    
    @Operation(summary = "获取病害分析数据 - 按道路分类")
    @GetMapping("/analysis/by-road")
    public Result<List<Map<String, Object>>> getAnalysisByRoad() {
        LambdaQueryWrapper<DamageHistory> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(DamageHistory::getDeleted, 0);
        
        List<DamageHistory> allRecords = damageHistoryMapper.selectList(wrapper);
        
        Map<Long, List<DamageHistory>> roadGroupMap = allRecords.stream()
                .filter(r -> r.getRoadId() != null)
                .collect(Collectors.groupingBy(DamageHistory::getRoadId));
        
        List<Map<String, Object>> result = new ArrayList<>();
        
        for (Map.Entry<Long, List<DamageHistory>> entry : roadGroupMap.entrySet()) {
            Long roadId = entry.getKey();
            List<DamageHistory> records = entry.getValue();
            
            Map<String, Object> item = new HashMap<>();
            item.put("roadId", roadId);
            item.put("roadName", records.get(0).getRoadName() != null ? 
                records.get(0).getRoadName() : "未知道路");
            item.put("recordCount", records.size());
            item.put("totalDamages", records.stream()
                .mapToLong(r -> r.getDamageCount() != null ? r.getDamageCount() : 0)
                .sum());
            item.put("severeCount", records.stream()
                .filter(r -> "严重".equals(r.getDamageLevel()))
                .count());
            item.put("minorCount", records.stream()
                .filter(r -> "轻微".equals(r.getDamageLevel()))
                .count());
            
            result.add(item);
        }
        
        result.sort((a, b) -> Long.compare((Long) b.get("totalDamages"), (Long) a.get("totalDamages")));
        
        return Result.success(result);
    }
    
    @Operation(summary = "获取病害分析数据 - 按路段分类")
    @GetMapping("/analysis/by-section")
    public Result<List<Map<String, Object>>> getAnalysisBySection() {
        LambdaQueryWrapper<DamageHistory> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(DamageHistory::getDeleted, 0);
        
        List<DamageHistory> allRecords = damageHistoryMapper.selectList(wrapper);
        
        Map<String, List<DamageHistory>> sectionGroupMap = allRecords.stream()
                .filter(r -> r.getRoadId() != null)
                .collect(Collectors.groupingBy(r -> 
                    r.getRoadId() + "_" + (r.getSectionId() != null ? r.getSectionId() : "null")));
        
        List<Map<String, Object>> result = new ArrayList<>();
        
        for (Map.Entry<String, List<DamageHistory>> entry : sectionGroupMap.entrySet()) {
            List<DamageHistory> records = entry.getValue();
            DamageHistory first = records.get(0);
            
            Map<String, Object> item = new HashMap<>();
            item.put("roadId", first.getRoadId());
            item.put("roadName", first.getRoadName() != null ? first.getRoadName() : "未知道路");
            item.put("sectionId", first.getSectionId());
            item.put("sectionName", first.getSectionName() != null ? first.getSectionName() : "全部路段");
            item.put("recordCount", records.size());
            item.put("totalDamages", records.stream()
                .mapToLong(r -> r.getDamageCount() != null ? r.getDamageCount() : 0)
                .sum());
            item.put("severeCount", records.stream()
                .filter(r -> "严重".equals(r.getDamageLevel()))
                .count());
            item.put("minorCount", records.stream()
                .filter(r -> "轻微".equals(r.getDamageLevel()))
                .count());
            
            Set<String> damageTypes = new java.util.HashSet<>();
            for (DamageHistory r : records) {
                if (r.getDamageTypes() != null) {
                    for (String type : r.getDamageTypes().split("、")) {
                        damageTypes.add(type.trim());
                    }
                }
            }
            item.put("damageTypes", String.join("、", damageTypes));
            
            result.add(item);
        }
        
        result.sort((a, b) -> Long.compare((Long) b.get("totalDamages"), (Long) a.get("totalDamages")));
        
        return Result.success(result);
    }
    
    @Operation(summary = "获取病害高频路段TOP10")
    @GetMapping("/analysis/top-sections")
    public Result<List<Map<String, Object>>> getTopSections() {
        LambdaQueryWrapper<DamageHistory> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(DamageHistory::getDeleted, 0);
        
        List<DamageHistory> allRecords = damageHistoryMapper.selectList(wrapper);
        
        Map<String, List<DamageHistory>> sectionGroupMap = allRecords.stream()
                .filter(r -> r.getRoadId() != null)
                .collect(Collectors.groupingBy(r -> 
                    r.getRoadId() + "_" + (r.getSectionId() != null ? r.getSectionId() : "null")));
        
        List<Map<String, Object>> result = new ArrayList<>();
        
        for (Map.Entry<String, List<DamageHistory>> entry : sectionGroupMap.entrySet()) {
            List<DamageHistory> records = entry.getValue();
            DamageHistory first = records.get(0);
            
            Map<String, Object> item = new HashMap<>();
            item.put("roadId", first.getRoadId());
            item.put("roadName", first.getRoadName() != null ? first.getRoadName() : "未知道路");
            item.put("sectionId", first.getSectionId());
            item.put("sectionName", first.getSectionName() != null ? first.getSectionName() : "全部路段");
            item.put("recordCount", records.size());
            item.put("totalDamages", records.stream()
                .mapToLong(r -> r.getDamageCount() != null ? r.getDamageCount() : 0)
                .sum());
            item.put("severeCount", records.stream()
                .filter(r -> "严重".equals(r.getDamageLevel()))
                .count());
            
            result.add(item);
        }
        
        result.sort((a, b) -> Long.compare((Long) b.get("totalDamages"), (Long) a.get("totalDamages")));
        
        if (result.size() > 10) {
            result = result.subList(0, 10);
        }
        
        return Result.success(result);
    }
}
