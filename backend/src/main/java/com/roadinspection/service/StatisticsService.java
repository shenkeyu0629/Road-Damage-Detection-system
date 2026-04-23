package com.roadinspection.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.roadinspection.dto.StatisticsQueryDTO;
import com.roadinspection.entity.DamageStatistics;

import java.util.List;
import java.util.Map;

public interface StatisticsService extends IService<DamageStatistics> {
    Map<String, Object> getDamageStatistics(StatisticsQueryDTO query);
    List<Map<String, Object>> getDamageTrend(StatisticsQueryDTO query);
    Map<String, Object> getDamageDistribution(StatisticsQueryDTO query);
    void generateDailyStatistics();
}
