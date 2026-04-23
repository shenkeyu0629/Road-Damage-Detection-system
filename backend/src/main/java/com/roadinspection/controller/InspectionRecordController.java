package com.roadinspection.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.roadinspection.common.Result;
import com.roadinspection.entity.InspectionRecord;
import com.roadinspection.entity.Road;
import com.roadinspection.entity.RoadSection;
import com.roadinspection.mapper.InspectionRecordMapper;
import com.roadinspection.mapper.RoadMapper;
import com.roadinspection.mapper.RoadSectionMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Tag(name = "巡检记录管理")
@RestController
@RequestMapping("/inspection/record")
@RequiredArgsConstructor
public class InspectionRecordController {

    private final InspectionRecordMapper inspectionRecordMapper;
    private final RoadMapper roadMapper;
    private final RoadSectionMapper roadSectionMapper;

    @Operation(summary = "分页查询巡检记录列表")
    @GetMapping("/list")
    public Result<Page<InspectionRecord>> list(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size,
            @RequestParam(required = false) Long taskId,
            @RequestParam(required = false) Long roadId,
            @RequestParam(required = false) String damageLevel) {
        
        Page<InspectionRecord> pageParam = new Page<>(page, size);
        LambdaQueryWrapper<InspectionRecord> wrapper = new LambdaQueryWrapper<>();
        
        if (taskId != null) {
            wrapper.eq(InspectionRecord::getTaskId, taskId);
        }
        if (roadId != null) {
            wrapper.eq(InspectionRecord::getRoadId, roadId);
        }
        if (damageLevel != null && !damageLevel.isEmpty()) {
            wrapper.eq(InspectionRecord::getDamageLevel, damageLevel);
        }
        wrapper.eq(InspectionRecord::getDeleted, 0);
        wrapper.orderByDesc(InspectionRecord::getInspectionTime);
        
        Page<InspectionRecord> result = inspectionRecordMapper.selectPage(pageParam, wrapper);
        
        List<InspectionRecord> records = result.getRecords();
        if (!records.isEmpty()) {
            Set<Long> roadIds = records.stream()
                    .map(InspectionRecord::getRoadId)
                    .filter(id -> id != null)
                    .collect(Collectors.toSet());
            Set<Long> sectionIds = records.stream()
                    .map(InspectionRecord::getSectionId)
                    .filter(id -> id != null)
                    .collect(Collectors.toSet());
            
            Map<Long, String> roadNameMap = new HashMap<>();
            if (!roadIds.isEmpty()) {
                List<Road> roads = roadMapper.selectBatchIds(roadIds);
                roadNameMap = roads.stream()
                        .collect(Collectors.toMap(Road::getId, Road::getRoadName));
            }
            
            Map<Long, String> sectionNameMap = new HashMap<>();
            if (!sectionIds.isEmpty()) {
                List<RoadSection> sections = roadSectionMapper.selectBatchIds(sectionIds);
                sectionNameMap = sections.stream()
                        .collect(Collectors.toMap(RoadSection::getId, RoadSection::getSectionName));
            }
            
            for (InspectionRecord record : records) {
                if (record.getRoadId() != null) {
                    record.setRoadName(roadNameMap.get(record.getRoadId()));
                }
                if (record.getSectionId() != null) {
                    record.setSectionName(sectionNameMap.get(record.getSectionId()));
                }
            }
        }
        
        return Result.success(result);
    }

    @Operation(summary = "获取巡检记录详情")
    @GetMapping("/{id}")
    public Result<InspectionRecord> getById(@PathVariable Long id) {
        return Result.success(inspectionRecordMapper.selectById(id));
    }

    @Operation(summary = "新增巡检记录")
    @PostMapping
    public Result<Void> save(@RequestBody InspectionRecord record) {
        inspectionRecordMapper.insert(record);
        return Result.success();
    }

    @Operation(summary = "更新巡检记录")
    @PutMapping
    public Result<Void> update(@RequestBody InspectionRecord record) {
        inspectionRecordMapper.updateById(record);
        return Result.success();
    }

    @Operation(summary = "删除巡检记录")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        inspectionRecordMapper.deleteById(id);
        return Result.success();
    }
    
    @Operation(summary = "获取巡检记录统计信息")
    @GetMapping("/statistics")
    public Result<Map<String, Object>> getStatistics(
            @RequestParam(required = false) Long roadId,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate) {
        
        LambdaQueryWrapper<InspectionRecord> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(InspectionRecord::getDeleted, 0);
        
        if (roadId != null) {
            wrapper.eq(InspectionRecord::getRoadId, roadId);
        }
        
        java.util.List<InspectionRecord> allRecords = inspectionRecordMapper.selectList(wrapper);
        
        long totalRecords = allRecords.size();
        long totalDamages = allRecords.stream()
                .mapToLong(r -> r.getDamageCount() != null ? r.getDamageCount() : 0)
                .sum();
        long severeRecords = allRecords.stream()
                .filter(r -> "严重".equals(r.getDamageLevel()))
                .count();
        Set<Long> uniqueRoads = allRecords.stream()
                .map(InspectionRecord::getRoadId)
                .filter(id -> id != null)
                .collect(Collectors.toSet());
        
        Map<String, Object> stats = new HashMap<>();
        stats.put("totalRecords", totalRecords);
        stats.put("totalDamages", totalDamages);
        stats.put("severeRecords", severeRecords);
        stats.put("roadsInspected", uniqueRoads.size());
        
        return Result.success(stats);
    }
    
    @Operation(summary = "获取按道路分类的统计数据")
    @GetMapping("/statistics/by-road")
    public Result<List<Map<String, Object>>> getStatisticsByRoad() {
        LambdaQueryWrapper<InspectionRecord> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(InspectionRecord::getDeleted, 0);
        
        List<InspectionRecord> allRecords = inspectionRecordMapper.selectList(wrapper);
        
        Map<Long, Long> roadCountMap = allRecords.stream()
                .filter(r -> r.getRoadId() != null)
                .collect(Collectors.groupingBy(InspectionRecord::getRoadId, Collectors.counting()));
        
        List<Map<String, Object>> result = new ArrayList<>();
        
        if (!roadCountMap.isEmpty()) {
            List<Road> roads = roadMapper.selectBatchIds(roadCountMap.keySet());
            Map<Long, String> roadNameMap = roads.stream()
                    .collect(Collectors.toMap(Road::getId, Road::getRoadName));
            
            for (Map.Entry<Long, Long> entry : roadCountMap.entrySet()) {
                Map<String, Object> item = new HashMap<>();
                item.put("roadId", entry.getKey());
                item.put("roadName", roadNameMap.getOrDefault(entry.getKey(), "未知道路"));
                item.put("count", entry.getValue());
                result.add(item);
            }
            
            result.sort((a, b) -> Long.compare((Long) b.get("count"), (Long) a.get("count")));
        }
        
        return Result.success(result);
    }
    
    @Operation(summary = "获取按路段分类的统计数据")
    @GetMapping("/statistics/by-section")
    public Result<List<Map<String, Object>>> getStatisticsBySection() {
        LambdaQueryWrapper<InspectionRecord> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(InspectionRecord::getDeleted, 0);
        
        List<InspectionRecord> allRecords = inspectionRecordMapper.selectList(wrapper);
        
        Map<Long, Long> sectionCountMap = allRecords.stream()
                .filter(r -> r.getSectionId() != null)
                .collect(Collectors.groupingBy(InspectionRecord::getSectionId, Collectors.counting()));
        
        List<Map<String, Object>> result = new ArrayList<>();
        
        if (!sectionCountMap.isEmpty()) {
            List<RoadSection> sections = roadSectionMapper.selectBatchIds(sectionCountMap.keySet());
            Map<Long, String> sectionNameMap = sections.stream()
                    .collect(Collectors.toMap(RoadSection::getId, RoadSection::getSectionName));
            Map<Long, Long> sectionRoadMap = sections.stream()
                    .collect(Collectors.toMap(RoadSection::getId, RoadSection::getRoadId));
            
            Set<Long> roadIds = sections.stream()
                    .map(RoadSection::getRoadId)
                    .filter(id -> id != null)
                    .collect(Collectors.toSet());
            
            Map<Long, String> roadNameMap = new HashMap<>();
            if (!roadIds.isEmpty()) {
                List<Road> roads = roadMapper.selectBatchIds(roadIds);
                roadNameMap = roads.stream()
                        .collect(Collectors.toMap(Road::getId, Road::getRoadName));
            }
            
            for (Map.Entry<Long, Long> entry : sectionCountMap.entrySet()) {
                Map<String, Object> item = new HashMap<>();
                Long sectionId = entry.getKey();
                item.put("sectionId", sectionId);
                item.put("sectionName", sectionNameMap.getOrDefault(sectionId, "未知路段"));
                
                Long roadId = sectionRoadMap.get(sectionId);
                item.put("roadId", roadId);
                item.put("roadName", roadNameMap.getOrDefault(roadId, "未知道路"));
                item.put("count", entry.getValue());
                result.add(item);
            }
            
            result.sort((a, b) -> Long.compare((Long) b.get("count"), (Long) a.get("count")));
        }
        
        return Result.success(result);
    }
}
