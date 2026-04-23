package com.roadinspection.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.roadinspection.common.Result;
import com.roadinspection.entity.DamageDisposal;
import com.roadinspection.mapper.DamageDisposalMapper;
import com.roadinspection.service.OperationLogService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Tag(name = "病害处置记录")
@RestController
@RequestMapping("/damage-disposal")
@RequiredArgsConstructor
public class DamageDisposalController {

    private final DamageDisposalMapper damageDisposalMapper;
    private final OperationLogService operationLogService;

    @Operation(summary = "分页查询病害处置记录列表")
    @GetMapping("/list")
    public Result<Page<DamageDisposal>> list(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size,
            @RequestParam(required = false) Long roadId,
            @RequestParam(required = false) Long sectionId,
            @RequestParam(required = false) String damageLevel,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate,
            @RequestParam(required = false) String auditStatus,
            @RequestParam(required = false) String roadName) {
        
        Page<DamageDisposal> pageParam = new Page<>(page, size);
        LambdaQueryWrapper<DamageDisposal> wrapper = new LambdaQueryWrapper<>();
        
        if (roadId != null) {
            wrapper.eq(DamageDisposal::getRoadId, roadId);
        }
        if (sectionId != null) {
            wrapper.eq(DamageDisposal::getSectionId, sectionId);
        }
        if (damageLevel != null && !damageLevel.isEmpty()) {
            wrapper.eq(DamageDisposal::getDamageLevel, damageLevel);
        }
        if (startDate != null && !startDate.isEmpty()) {
            wrapper.ge(DamageDisposal::getDisposalTime, java.time.LocalDateTime.parse(startDate + "T00:00:00"));
        }
        if (endDate != null && !endDate.isEmpty()) {
            wrapper.le(DamageDisposal::getDisposalTime, java.time.LocalDateTime.parse(endDate + "T23:59:59"));
        }
        if (auditStatus != null && !auditStatus.isEmpty()) {
            wrapper.eq(DamageDisposal::getAuditStatus, auditStatus);
        }
        if (roadName != null && !roadName.isEmpty()) {
            wrapper.like(DamageDisposal::getRoadName, roadName);
        }
        
        wrapper.eq(DamageDisposal::getDeleted, 0);
        wrapper.orderByDesc(DamageDisposal::getDisposalTime);
        
        return Result.success(damageDisposalMapper.selectPage(pageParam, wrapper));
    }

    @Operation(summary = "获取病害处置详情")
    @GetMapping("/{id}")
    public Result<DamageDisposal> getById(@PathVariable Long id) {
        return Result.success(damageDisposalMapper.selectById(id));
    }

    @Operation(summary = "删除病害处置记录")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        DamageDisposal disposal = damageDisposalMapper.selectById(id);
        damageDisposalMapper.deleteById(id);
        
        String operator = getCurrentUsername();
        operationLogService.logDelete(operator, "病害处置记录：" + (disposal != null ? disposal.getRoadName() : id));
        
        return Result.success();
    }

    @Operation(summary = "获取统计数据")
    @GetMapping("/statistics")
    public Result<Map<String, Object>> getStatistics(
            @RequestParam(required = false) Long roadId,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate) {
        
        LambdaQueryWrapper<DamageDisposal> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(DamageDisposal::getDeleted, 0);
        
        if (roadId != null) {
            wrapper.eq(DamageDisposal::getRoadId, roadId);
        }
        if (startDate != null && !startDate.isEmpty()) {
            wrapper.ge(DamageDisposal::getDisposalTime, java.time.LocalDateTime.parse(startDate + "T00:00:00"));
        }
        if (endDate != null && !endDate.isEmpty()) {
            wrapper.le(DamageDisposal::getDisposalTime, java.time.LocalDateTime.parse(endDate + "T23:59:59"));
        }
        
        List<DamageDisposal> allRecords = damageDisposalMapper.selectList(wrapper);
        
        long totalRecords = allRecords.size();
        long totalDamages = allRecords.stream()
                .mapToLong(r -> r.getDamageCount() != null ? r.getDamageCount() : 0)
                .sum();
        long severeCount = allRecords.stream()
                .filter(r -> "严重".equals(r.getDamageLevel()))
                .count();
        
        Map<String, Object> stats = new HashMap<>();
        stats.put("totalRecords", totalRecords);
        stats.put("totalDamages", totalDamages);
        stats.put("severeCount", severeCount);
        
        return Result.success(stats);
    }
    
    @Operation(summary = "审核病害处置记录")
    @PutMapping("/{id}/audit")
    public Result<Void> audit(@PathVariable Long id, @RequestBody Map<String, Object> data) {
        DamageDisposal disposal = damageDisposalMapper.selectById(id);
        if (disposal == null) {
            return Result.error(404, "记录不存在");
        }
        
        String auditStatus = (String) data.get("auditStatus");
        String auditRemark = (String) data.get("auditRemark");
        
        if (auditStatus == null || (!auditStatus.equals("审核通过") && !auditStatus.equals("审核不通过"))) {
            return Result.error(400, "审核状态无效");
        }
        
        disposal.setAuditStatus(auditStatus);
        disposal.setAuditRemark(auditRemark);
        disposal.setAuditTime(java.time.LocalDateTime.now());
        
        damageDisposalMapper.updateById(disposal);
        
        String operator = getCurrentUsername();
        String target = disposal.getRoadName() + " - " + disposal.getSectionName();
        if (auditStatus.equals("审核通过")) {
            operationLogService.logAuditPass(operator, target);
        } else {
            operationLogService.logAuditReject(operator, target);
        }
        
        return Result.success();
    }
    
    private String getCurrentUsername() {
        return "系统";
    }
}
