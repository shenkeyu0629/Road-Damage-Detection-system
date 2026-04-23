package com.roadinspection.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.roadinspection.common.Result;
import com.roadinspection.entity.InspectionRecord;
import com.roadinspection.entity.InspectionTask;
import com.roadinspection.entity.Road;
import com.roadinspection.entity.RoadSection;
import com.roadinspection.mapper.InspectionRecordMapper;
import com.roadinspection.mapper.InspectionTaskMapper;
import com.roadinspection.mapper.RoadMapper;
import com.roadinspection.mapper.RoadSectionMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Tag(name = "自动巡检管理")
@RestController
@RequestMapping("/inspection/task")
@RequiredArgsConstructor
public class InspectionTaskController {

    private final InspectionTaskMapper taskMapper;
    private final RoadMapper roadMapper;
    private final RoadSectionMapper roadSectionMapper;
    private final InspectionRecordMapper inspectionRecordMapper;

    @Operation(summary = "分页查询巡检任务")
    @GetMapping("/list")
    public Result<Page<InspectionTask>> list(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) Long roadId,
            @RequestParam(required = false) Long sectionId,
            @RequestParam(required = false) String taskName) {
        
        Page<InspectionTask> pageParam = new Page<>(page, size);
        LambdaQueryWrapper<InspectionTask> wrapper = new LambdaQueryWrapper<>();
        
        if (status != null && !status.isEmpty()) {
            wrapper.eq(InspectionTask::getStatus, status);
        }
        if (roadId != null) {
            wrapper.eq(InspectionTask::getRoadId, roadId);
        }
        if (sectionId != null) {
            wrapper.eq(InspectionTask::getSectionId, sectionId);
        }
        if (taskName != null && !taskName.isEmpty()) {
            wrapper.like(InspectionTask::getTaskName, taskName);
        }
        wrapper.orderByDesc(InspectionTask::getCreateTime);
        
        Page<InspectionTask> result = taskMapper.selectPage(pageParam, wrapper);
        
        for (InspectionTask task : result.getRecords()) {
            fillTaskInfo(task);
        }
        
        return Result.success(result);
    }

    @Operation(summary = "获取任务详情")
    @GetMapping("/{id}")
    public Result<InspectionTask> getById(@PathVariable Long id) {
        InspectionTask task = taskMapper.selectById(id);
        if (task != null) {
            fillTaskInfo(task);
        }
        return Result.success(task);
    }

    @Operation(summary = "创建巡检任务")
    @PostMapping
    public Result<InspectionTask> create(@RequestBody InspectionTask task) {
        task.setStatus("pending");
        task.setCreateTime(LocalDateTime.now());
        taskMapper.insert(task);
        
        createInspectionRecord(task);
        
        fillTaskInfo(task);
        return Result.success(task);
    }
    
    private void createInspectionRecord(InspectionTask task) {
        InspectionRecord record = new InspectionRecord();
        record.setTaskId(task.getId());
        record.setRoadId(task.getRoadId());
        record.setSectionId(task.getSectionId());
        record.setInspectionTime(LocalDateTime.now());
        record.setDamageCount(0);
        record.setDamageLevel("无");
        record.setStatus("pending");
        inspectionRecordMapper.insert(record);
    }

    @Operation(summary = "立即开始巡检")
    @PostMapping("/immediate")
    public Result<InspectionTask> createImmediate(@RequestBody InspectionTask task) {
        task.setStatus("running");
        task.setCreateTime(LocalDateTime.now());
        task.setStartTime(LocalDateTime.now());
        task.setTaskType("immediate");
        taskMapper.insert(task);
        
        createInspectionRecord(task);
        
        fillTaskInfo(task);
        return Result.success(task);
    }

    @Operation(summary = "更新巡检任务")
    @PutMapping("/{id}")
    public Result<Void> update(@PathVariable Long id, @RequestBody InspectionTask task) {
        task.setId(id);
        taskMapper.updateById(task);
        return Result.success();
    }

    @Operation(summary = "删除巡检任务")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        taskMapper.deleteById(id);
        return Result.success();
    }

    @Operation(summary = "执行巡检任务")
    @PostMapping("/{id}/execute")
    public Result<Map<String, Object>> executeTask(@PathVariable Long id) {
        InspectionTask task = taskMapper.selectById(id);
        if (task == null) {
            return Result.error(404, "任务不存在");
        }
        
        task.setStatus("running");
        task.setStartTime(LocalDateTime.now());
        taskMapper.updateById(task);
        
        Map<String, Object> result = new HashMap<>();
        result.put("taskId", id);
        result.put("status", "started");
        result.put("message", "巡检任务已开始执行");
        
        return Result.success(result);
    }

    @Operation(summary = "取消巡检任务")
    @PostMapping("/{id}/cancel")
    public Result<Void> cancelTask(@PathVariable Long id) {
        InspectionTask task = taskMapper.selectById(id);
        if (task != null && "running".equals(task.getStatus())) {
            task.setStatus("cancelled");
            task.setEndTime(LocalDateTime.now());
            taskMapper.updateById(task);
        }
        return Result.success();
    }

    @Operation(summary = "获取任务进度")
    @GetMapping("/{id}/progress")
    public Result<Map<String, Object>> getProgress(@PathVariable Long id) {
        InspectionTask task = taskMapper.selectById(id);
        if (task == null) {
            return Result.error(404, "任务不存在");
        }
        
        Map<String, Object> progress = new HashMap<>();
        progress.put("taskId", id);
        progress.put("status", task.getStatus());
        progress.put("startTime", task.getStartTime());
        progress.put("endTime", task.getEndTime());
        
        return Result.success(progress);
    }

    @Operation(summary = "获取正在执行的任务")
    @GetMapping("/running")
    public Result<List<InspectionTask>> getRunningTasks() {
        LambdaQueryWrapper<InspectionTask> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(InspectionTask::getStatus, "running");
        List<InspectionTask> tasks = taskMapper.selectList(wrapper);
        for (InspectionTask task : tasks) {
            fillTaskInfo(task);
        }
        return Result.success(tasks);
    }

    private void fillTaskInfo(InspectionTask task) {
        if (task.getRoadId() != null) {
            Road road = roadMapper.selectById(task.getRoadId());
            if (road != null) {
                task.setRoadName(road.getRoadName());
            }
        }
        if (task.getSectionId() != null) {
            RoadSection section = roadSectionMapper.selectById(task.getSectionId());
            if (section != null) {
                task.setSectionName(section.getSectionName());
            }
        }
    }

    @Scheduled(fixedRate = 60000)
    public void checkScheduledTasks() {
        LocalDateTime now = LocalDateTime.now();
        
        LambdaQueryWrapper<InspectionTask> pendingWrapper = new LambdaQueryWrapper<>();
        pendingWrapper.eq(InspectionTask::getStatus, "pending");
        pendingWrapper.isNotNull(InspectionTask::getScheduledStartTime);
        pendingWrapper.le(InspectionTask::getScheduledStartTime, now);
        
        List<InspectionTask> tasksToStart = taskMapper.selectList(pendingWrapper);
        for (InspectionTask task : tasksToStart) {
            task.setStatus("running");
            task.setStartTime(now);
            taskMapper.updateById(task);
            System.out.println("任务 " + task.getTaskName() + " 已自动开始执行");
        }
        
        LambdaQueryWrapper<InspectionTask> runningWrapper = new LambdaQueryWrapper<>();
        runningWrapper.eq(InspectionTask::getStatus, "running");
        runningWrapper.isNotNull(InspectionTask::getScheduledEndTime);
        runningWrapper.le(InspectionTask::getScheduledEndTime, now);
        
        List<InspectionTask> tasksToEnd = taskMapper.selectList(runningWrapper);
        for (InspectionTask task : tasksToEnd) {
            task.setStatus("completed");
            task.setEndTime(now);
            taskMapper.updateById(task);
            System.out.println("任务 " + task.getTaskName() + " 已自动结束");
        }
    }
    
    @Operation(summary = "获取统计数据")
    @GetMapping("/statistics")
    public Result<Map<String, Object>> getStatistics(
            @RequestParam(required = false) Long roadId) {
        LambdaQueryWrapper<InspectionTask> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(InspectionTask::getDeleted, 0);
        
        if (roadId != null) {
            wrapper.eq(InspectionTask::getRoadId, roadId);
        }
        
        List<InspectionTask> allTasks = taskMapper.selectList(wrapper);
        
        long totalRecords = allTasks.size();
        Set<Long> uniqueRoads = allTasks.stream()
                .map(InspectionTask::getRoadId)
                .filter(id -> id != null)
                .collect(Collectors.toSet());
        
        Map<String, Object> stats = new HashMap<>();
        stats.put("totalRecords", totalRecords);
        stats.put("roadsInspected", uniqueRoads.size());
        
        return Result.success(stats);
    }
    
    @Operation(summary = "获取按道路分类的统计数据")
    @GetMapping("/statistics/by-road")
    public Result<List<Map<String, Object>>> getStatisticsByRoad() {
        LambdaQueryWrapper<InspectionTask> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(InspectionTask::getDeleted, 0);
        
        List<InspectionTask> allTasks = taskMapper.selectList(wrapper);
        
        Map<Long, Long> roadCountMap = allTasks.stream()
                .filter(t -> t.getRoadId() != null)
                .collect(Collectors.groupingBy(InspectionTask::getRoadId, Collectors.counting()));
        
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
        LambdaQueryWrapper<InspectionTask> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(InspectionTask::getDeleted, 0);
        
        List<InspectionTask> allTasks = taskMapper.selectList(wrapper);
        
        Map<Long, Long> sectionCountMap = allTasks.stream()
                .filter(t -> t.getSectionId() != null)
                .collect(Collectors.groupingBy(InspectionTask::getSectionId, Collectors.counting()));
        
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
