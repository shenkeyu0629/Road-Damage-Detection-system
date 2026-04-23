package com.roadinspection.controller;

import cn.hutool.core.util.IdUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.roadinspection.common.Result;
import com.roadinspection.entity.AlarmInfo;
import com.roadinspection.entity.DisposalRecord;
import com.roadinspection.mapper.AlarmInfoMapper;
import com.roadinspection.mapper.DisposalRecordMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.Map;

@Tag(name = "告警管理")
@RestController
@RequestMapping("/alarm")
@RequiredArgsConstructor
public class AlarmController {

    private final AlarmInfoMapper alarmInfoMapper;
    private final DisposalRecordMapper disposalRecordMapper;

    @Operation(summary = "分页查询告警列表")
    @GetMapping("/list")
    public Result<Page<AlarmInfo>> list(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String alarmLevel,
            @RequestParam(required = false) Long roadId) {
        
        Page<AlarmInfo> pageParam = new Page<>(page, size);
        LambdaQueryWrapper<AlarmInfo> wrapper = new LambdaQueryWrapper<>();
        
        if (status != null && !status.isEmpty()) {
            wrapper.eq(AlarmInfo::getStatus, status);
        }
        if (alarmLevel != null && !alarmLevel.isEmpty()) {
            wrapper.eq(AlarmInfo::getAlarmLevel, alarmLevel);
        }
        if (roadId != null) {
            wrapper.eq(AlarmInfo::getRoadId, roadId);
        }
        wrapper.eq(AlarmInfo::getDeleted, 0);
        wrapper.orderByDesc(AlarmInfo::getCreateTime);
        
        return Result.success(alarmInfoMapper.selectPage(pageParam, wrapper));
    }

    @Operation(summary = "获取告警详情")
    @GetMapping("/{id}")
    public Result<AlarmInfo> getById(@PathVariable Long id) {
        return Result.success(alarmInfoMapper.selectById(id));
    }

    @Operation(summary = "处理告警")
    @PutMapping("/{id}/handle")
    public Result<Void> handleAlarm(
            @PathVariable Long id,
            @RequestBody Map<String, Object> data) {
        
        AlarmInfo alarm = alarmInfoMapper.selectById(id);
        if (alarm == null) {
            return Result.error(404, "告警不存在");
        }
        
        String status = (String) data.getOrDefault("status", "resolved");
        String handleResult = (String) data.get("handleResult");
        String remark = (String) data.get("remark");
        Boolean createDisposal = (Boolean) data.get("createDisposal");
        
        alarm.setStatus(status);
        alarm.setHandleResult(handleResult);
        alarm.setHandleTime(LocalDateTime.now());
        if (remark != null) {
            alarm.setRemark(remark);
        }
        alarmInfoMapper.updateById(alarm);
        
        if (Boolean.TRUE.equals(createDisposal) && handleResult != null) {
            DisposalRecord disposal = new DisposalRecord();
            disposal.setAlarmId(id);
            disposal.setRoadId(alarm.getRoadId());
            disposal.setSectionId(alarm.getSectionId());
            disposal.setDisposalType("病害维修");
            disposal.setDisposalMethod(handleResult);
            disposal.setStatus("pending");
            disposal.setRemark(remark);
            disposalRecordMapper.insert(disposal);
        }
        
        return Result.success();
    }

    @Operation(summary = "从检测结果创建告警")
    @PostMapping("/from-detection")
    public Result<AlarmInfo> createFromDetection(@RequestBody Map<String, Object> data) {
        AlarmInfo alarm = new AlarmInfo();
        alarm.setAlarmCode("ALM" + IdUtil.getSnowflakeNextIdStr());
        alarm.setAlarmType((String) data.getOrDefault("alarm_type", "damage_detected"));
        alarm.setAlarmLevel((String) data.getOrDefault("alarm_level", "minor"));
        alarm.setAlarmReason((String) data.get("alarm_reason"));
        alarm.setStatus("pending");
        
        if (data.containsKey("road_id")) {
            alarm.setRoadId(Long.valueOf(data.get("road_id").toString()));
        }
        if (data.containsKey("section_id")) {
            alarm.setSectionId(Long.valueOf(data.get("section_id").toString()));
        }
        if (data.containsKey("image_path")) {
            alarm.setImagePath((String) data.get("image_path"));
        }
        if (data.containsKey("result_image_path")) {
            alarm.setResultImagePath((String) data.get("result_image_path"));
        }
        
        String remark = (String) data.getOrDefault("remark", "");
        Object totalDamagesObj = data.get("total_damages");
        Integer totalDamages = totalDamagesObj != null ? Integer.valueOf(totalDamagesObj.toString()) : 0;
        Object minorCountObj = data.get("minor_count");
        Integer minorCount = minorCountObj != null ? Integer.valueOf(minorCountObj.toString()) : 0;
        Object moderateCountObj = data.get("moderate_count");
        Integer moderateCount = moderateCountObj != null ? Integer.valueOf(moderateCountObj.toString()) : 0;
        Object severeCountObj = data.get("severe_count");
        Integer severeCount = severeCountObj != null ? Integer.valueOf(severeCountObj.toString()) : 0;
        
        String fullRemark = String.format("检测到%d处病害: 轻微%d处, 中等%d处, 严重%d处。 %s", 
            totalDamages, minorCount, moderateCount, severeCount, remark);
        alarm.setRemark(fullRemark);
        
        alarmInfoMapper.insert(alarm);
        
        return Result.success(alarm);
    }

    @Operation(summary = "创建处置记录")
    @PostMapping("/disposal")
    public Result<Void> createDisposal(@RequestBody DisposalRecord record) {
        record.setStatus("pending");
        disposalRecordMapper.insert(record);
        
        if (record.getAlarmId() != null) {
            AlarmInfo alarm = alarmInfoMapper.selectById(record.getAlarmId());
            if (alarm != null) {
                alarm.setStatus("processing");
                alarmInfoMapper.updateById(alarm);
            }
        }
        
        return Result.success();
    }

    @Operation(summary = "分页查询处置记录")
    @GetMapping("/disposal/list")
    public Result<Page<DisposalRecord>> disposalList(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) Long roadId) {
        
        Page<DisposalRecord> pageParam = new Page<>(page, size);
        LambdaQueryWrapper<DisposalRecord> wrapper = new LambdaQueryWrapper<>();
        
        if (status != null && !status.isEmpty()) {
            wrapper.eq(DisposalRecord::getStatus, status);
        }
        if (roadId != null) {
            wrapper.eq(DisposalRecord::getRoadId, roadId);
        }
        wrapper.eq(DisposalRecord::getDeleted, 0);
        wrapper.orderByDesc(DisposalRecord::getCreateTime);
        
        return Result.success(disposalRecordMapper.selectPage(pageParam, wrapper));
    }

    @Operation(summary = "更新处置记录状态")
    @PutMapping("/disposal/{id}/status")
    public Result<Void> updateDisposalStatus(
            @PathVariable Long id,
            @RequestBody Map<String, Object> data) {
        
        DisposalRecord record = disposalRecordMapper.selectById(id);
        if (record != null) {
            String status = (String) data.get("status");
            if (status != null) {
                record.setStatus(status);
            }
            if ("completed".equals(status)) {
                record.setEndTime(LocalDateTime.now());
            }
            disposalRecordMapper.updateById(record);
        }
        return Result.success();
    }
}
