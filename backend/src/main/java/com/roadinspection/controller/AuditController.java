package com.roadinspection.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.roadinspection.common.Result;
import com.roadinspection.entity.AuditConfig;
import com.roadinspection.entity.AuditTask;
import com.roadinspection.mapper.AuditConfigMapper;
import com.roadinspection.mapper.AuditTaskMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Tag(name = "审核流程管理")
@RestController
@RequestMapping("/audit")
@RequiredArgsConstructor
public class AuditController {

    private final AuditConfigMapper auditConfigMapper;
    private final AuditTaskMapper auditTaskMapper;

    @Operation(summary = "获取审核配置")
    @GetMapping("/config")
    public Result<AuditConfig> getConfig() {
        LambdaQueryWrapper<AuditConfig> wrapper = new LambdaQueryWrapper<>();
        wrapper.last("LIMIT 1");
        AuditConfig config = auditConfigMapper.selectOne(wrapper);
        if (config == null) {
            config = new AuditConfig();
            config.setEnabled(true);
            config.setLevel("single");
            config.setConfidenceThreshold(70);
            config.setForceReviewTypes("pothole");
            config.setReviewerAssignment("role");
            auditConfigMapper.insert(config);
        }
        return Result.success(config);
    }

    @Operation(summary = "保存审核配置")
    @PutMapping("/config")
    public Result<Void> saveConfig(@RequestBody AuditConfig config) {
        if (config.getId() == null) {
            auditConfigMapper.insert(config);
        } else {
            auditConfigMapper.updateById(config);
        }
        return Result.success();
    }

    @Operation(summary = "获取待审核任务列表")
    @GetMapping("/pending")
    public Result<List<AuditTask>> getPendingTasks() {
        LambdaQueryWrapper<AuditTask> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(AuditTask::getStatus, "pending");
        wrapper.orderByDesc(AuditTask::getCreateTime);
        return Result.success(auditTaskMapper.selectList(wrapper));
    }

    @Operation(summary = "获取审核任务详情")
    @GetMapping("/task/{id}")
    public Result<AuditTask> getTaskById(@PathVariable Long id) {
        return Result.success(auditTaskMapper.selectById(id));
    }

    @Operation(summary = "审核通过")
    @PutMapping("/task/{id}/approve")
    public Result<Void> approveTask(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        AuditTask task = auditTaskMapper.selectById(id);
        if (task != null) {
            task.setStatus("approved");
            task.setComment((String) body.get("comment"));
            task.setSuggestion((String) body.get("suggestion"));
            task.setApprovedDamages((String) body.get("approvedDamages"));
            auditTaskMapper.updateById(task);
        }
        return Result.success();
    }

    @Operation(summary = "审核驳回")
    @PutMapping("/task/{id}/reject")
    public Result<Void> rejectTask(@PathVariable Long id, @RequestBody Map<String, String> body) {
        AuditTask task = auditTaskMapper.selectById(id);
        if (task != null) {
            task.setStatus("rejected");
            task.setComment(body.get("comment"));
            auditTaskMapper.updateById(task);
        }
        return Result.success();
    }

    @Operation(summary = "获取已审核任务列表")
    @GetMapping("/reviewed")
    public Result<List<AuditTask>> getReviewedTasks() {
        LambdaQueryWrapper<AuditTask> wrapper = new LambdaQueryWrapper<>();
        wrapper.in(AuditTask::getStatus, "approved", "rejected");
        wrapper.orderByDesc(AuditTask::getUpdateTime);
        return Result.success(auditTaskMapper.selectList(wrapper));
    }
}
