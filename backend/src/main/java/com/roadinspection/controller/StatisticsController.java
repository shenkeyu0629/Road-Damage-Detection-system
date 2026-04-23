package com.roadinspection.controller;

import com.roadinspection.common.Result;
import com.roadinspection.dto.StatisticsQueryDTO;
import com.roadinspection.service.StatisticsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Tag(name = "统计分析")
@RestController
@RequestMapping("/statistics")
@RequiredArgsConstructor
public class StatisticsController {

    private final StatisticsService statisticsService;

    @Operation(summary = "获取病害统计数据")
    @GetMapping("/damage")
    public Result<Map<String, Object>> getDamageStatistics(StatisticsQueryDTO query) {
        return Result.success(statisticsService.getDamageStatistics(query));
    }

    @Operation(summary = "获取病害趋势")
    @GetMapping("/trend")
    public Result<List<Map<String, Object>>> getDamageTrend(StatisticsQueryDTO query) {
        return Result.success(statisticsService.getDamageTrend(query));
    }

    @Operation(summary = "获取病害分布")
    @GetMapping("/distribution")
    public Result<Map<String, Object>> getDamageDistribution(StatisticsQueryDTO query) {
        return Result.success(statisticsService.getDamageDistribution(query));
    }

    @Operation(summary = "生成每日统计")
    @PostMapping("/generate-daily")
    public Result<Void> generateDailyStatistics() {
        statisticsService.generateDailyStatistics();
        return Result.success();
    }
}
