package com.roadinspection.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.roadinspection.common.Result;
import com.roadinspection.entity.OperationLog;
import com.roadinspection.mapper.OperationLogMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@Tag(name = "操作日志管理")
@RestController
@RequestMapping("/log")
@RequiredArgsConstructor
public class LogController {

    private final OperationLogMapper operationLogMapper;

    @Operation(summary = "分页查询操作日志")
    @GetMapping("/list")
    public Result<Page<OperationLog>> list(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "20") Integer size,
            @RequestParam(required = false) String username,
            @RequestParam(required = false) String operationType,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate) {
        
        Page<OperationLog> pageParam = new Page<>(page, size);
        LambdaQueryWrapper<OperationLog> wrapper = new LambdaQueryWrapper<>();
        
        if (username != null && !username.isEmpty()) {
            wrapper.like(OperationLog::getUsername, username);
        }
        if (operationType != null && !operationType.isEmpty()) {
            wrapper.eq(OperationLog::getOperationType, operationType);
        }
        if (startDate != null && !startDate.isEmpty()) {
            wrapper.ge(OperationLog::getOperationTime, LocalDate.parse(startDate).atStartOfDay());
        }
        if (endDate != null && !endDate.isEmpty()) {
            wrapper.le(OperationLog::getOperationTime, LocalDate.parse(endDate).atTime(23, 59, 59));
        }
        wrapper.orderByDesc(OperationLog::getOperationTime);
        
        return Result.success(operationLogMapper.selectPage(pageParam, wrapper));
    }

    @Operation(summary = "获取日志详情")
    @GetMapping("/{id}")
    public Result<OperationLog> getById(@PathVariable Long id) {
        return Result.success(operationLogMapper.selectById(id));
    }

    @Operation(summary = "批量删除日志")
    @DeleteMapping("/batch")
    public Result<Void> batchDelete(@RequestBody List<Long> ids) {
        operationLogMapper.deleteBatchIds(ids);
        return Result.success();
    }

    @Operation(summary = "清理指定时间之前的日志")
    @DeleteMapping("/clean")
    public Result<Long> cleanBefore(
            @RequestParam(required = false) String beforeDate,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate) {
        
        LambdaQueryWrapper<OperationLog> wrapper = new LambdaQueryWrapper<>();
        
        if (beforeDate != null && !beforeDate.isEmpty()) {
            wrapper.lt(OperationLog::getOperationTime, LocalDate.parse(beforeDate).atTime(23, 59, 59));
        } else if (startDate != null && !startDate.isEmpty() && endDate != null && !endDate.isEmpty()) {
            wrapper.ge(OperationLog::getOperationTime, LocalDate.parse(startDate).atStartOfDay());
            wrapper.le(OperationLog::getOperationTime, LocalDate.parse(endDate).atTime(23, 59, 59));
        }
        
        Long count = operationLogMapper.selectCount(wrapper);
        operationLogMapper.delete(wrapper);
        
        return Result.success(count);
    }
}
