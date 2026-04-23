package com.roadinspection.service.impl;

import cn.hutool.core.util.IdUtil;
import cn.hutool.http.HttpUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.roadinspection.dto.DetectionRequestDTO;
import com.roadinspection.dto.DetectionResultDTO;
import com.roadinspection.dto.VideoDetectionRequestDTO;
import com.roadinspection.dto.VideoDetectionResultDTO;
import com.roadinspection.entity.AlarmInfo;
import com.roadinspection.entity.DamageInfo;
import com.roadinspection.entity.InspectionRecord;
import com.roadinspection.exception.BusinessException;
import com.roadinspection.mapper.AlarmInfoMapper;
import com.roadinspection.mapper.DamageInfoMapper;
import com.roadinspection.mapper.InspectionRecordMapper;
import com.roadinspection.service.DetectionService;
import com.roadinspection.service.TrackedDamage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class DetectionServiceImpl implements DetectionService {

    private final InspectionRecordMapper inspectionRecordMapper;
    private final DamageInfoMapper damageInfoMapper;
    private final AlarmInfoMapper alarmInfoMapper;

    @Value("${ai-service.url}")
    private String aiServiceUrl;

    @Override
    public DetectionResultDTO detectImage(DetectionRequestDTO requestDTO) {
        try {
            String url = aiServiceUrl + "/api/v1/detection/detect";
            
            Map<String, Object> params = new HashMap<>();
            params.put("image_path", requestDTO.getImagePath());
            params.put("conf_threshold", requestDTO.getConfThreshold());
            params.put("draw_result", true);
            
            String response = HttpUtil.post(url, JSONUtil.toJsonStr(params));
            JSONObject json = JSONUtil.parseObj(response);
            
            DetectionResultDTO result = new DetectionResultDTO();
            result.setImagePath(json.getStr("image_path"));
            result.setImageWidth(json.getInt("image_width"));
            result.setImageHeight(json.getInt("image_height"));
            
            List<Map<String, Object>> detections = new ArrayList<>();
            List<?> detList = json.getBeanList("detections", Map.class);
            for (Object item : detList) {
                if (item instanceof Map) {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> map = (Map<String, Object>) item;
                    detections.add(map);
                }
            }
            result.setDetections(detections);
            
            @SuppressWarnings("unchecked")
            Map<String, Object> damageSummary = (Map<String, Object>) json.get("damage_summary");
            result.setDamageSummary(damageSummary != null ? damageSummary : new HashMap<>());
            
            result.setOverallLevel(json.getStr("overall_level"));
            result.setAlarmNeeded(json.getBool("alarm_needed"));
            
            @SuppressWarnings("unchecked")
            Map<String, Object> alarmInfo = (Map<String, Object>) json.get("alarm_info");
            result.setAlarmInfo(alarmInfo != null ? alarmInfo : new HashMap<>());
            
            result.setDetectionTime(json.getStr("detection_time"));
            result.setResultImagePath(json.getStr("result_image_path"));
            
            return result;
        } catch (Exception e) {
            log.error("AI服务调用失败", e);
            throw new BusinessException("AI服务调用失败: " + e.getMessage());
        }
    }

    @Override
    public VideoDetectionResultDTO detectVideo(VideoDetectionRequestDTO requestDTO) {
        try {
            String url = aiServiceUrl + "/api/v1/detection/detect-video";
            
            Map<String, Object> params = new HashMap<>();
            params.put("video_path", requestDTO.getVideoPath());
            params.put("conf_threshold", requestDTO.getConfThreshold());
            
            Integer frameInterval = requestDTO.getFrameInterval();
            if (frameInterval != null && frameInterval > 0) {
                params.put("frame_interval", frameInterval);
            } else {
                params.put("frame_interval", 3);
            }
            
            String response = HttpUtil.post(url, JSONUtil.toJsonStr(params));
            JSONObject json = JSONUtil.parseObj(response);
            
            VideoDetectionResultDTO result = new VideoDetectionResultDTO();
            result.setVideoPath(json.getStr("video_path"));
            result.setTotalFrames(json.getInt("total_frames"));
            result.setProcessedFrames(json.getInt("processed_frames"));
            result.setFps(json.getInt("fps"));
            
            Double duration = json.getDouble("duration");
            result.setDurationMs(duration != null ? (long)(duration * 1000) : 0L);
            
            List<TrackedDamage> trackedDamages = new ArrayList<>();
            List<?> allDetections = json.getBeanList("all_detections", Map.class);
            if (allDetections != null) {
                for (Object item : allDetections) {
                    if (item instanceof Map) {
                        @SuppressWarnings("unchecked")
                        Map<String, Object> frameData = (Map<String, Object>) item;
                        List<?> frameDetections = (List<?>) frameData.get("detections");
                        if (frameDetections != null) {
                            for (Object det : frameDetections) {
                                if (det instanceof Map) {
                                    @SuppressWarnings("unchecked")
                                    Map<String, Object> detMap = (Map<String, Object>) det;
                                    
                                    Map<String, Object> bboxMap = (Map<String, Object>) detMap.get("bbox");
                                    double[] bbox = new double[]{0, 0, 0, 0};
                                    if (bboxMap != null) {
                                        bbox = new double[]{
                                            ((Number) bboxMap.get("x1")).doubleValue(),
                                            ((Number) bboxMap.get("y1")).doubleValue(),
                                            ((Number) bboxMap.get("x2")).doubleValue(),
                                            ((Number) bboxMap.get("y2")).doubleValue()
                                        };
                                    }
                                    
                                    TrackedDamage tracked = new TrackedDamage(
                                        (String) detMap.get("class_name"),
                                        bbox,
                                        ((Number) frameData.get("frame")).intValue(),
                                        System.currentTimeMillis()
                                    );
                                    
                                    if (detMap.get("confidence") != null) {
                                        tracked.getConfidences().add(((Number) detMap.get("confidence")).doubleValue());
                                    }
                                    
                                    trackedDamages.add(tracked);
                                }
                            }
                        }
                    }
                }
            }
            result.setTrackedDamages(trackedDamages);
            
            Map<String, Object> summary = json.get("summary", Map.class);
            if (summary != null) {
                Object totalDetections = summary.get("total_detections");
                result.setUniqueDamageCount(totalDetections != null ? ((Number) totalDetections).intValue() : 0);
                result.setOverallLevel((String) summary.get("max_severity"));
                
                Map<String, Object> damageSummary = new HashMap<>();
                damageSummary.put("by_level", summary.get("by_level"));
                result.setDamageSummary(damageSummary);
                
                Object byLevel = summary.get("by_level");
                Integer severeCount = 0;
                if (byLevel instanceof Map) {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> byLevelMap = (Map<String, Object>) byLevel;
                    Object severe = byLevelMap.get("severe");
                    severeCount = severe != null ? ((Number) severe).intValue() : 0;
                }
                result.setAlarmNeeded(severeCount > 0);
            } else {
                result.setUniqueDamageCount(0);
                result.setOverallLevel("none");
                result.setAlarmNeeded(false);
            }
            
            return result;
        } catch (Exception e) {
            log.error("视频检测服务调用失败", e);
            throw new BusinessException("视频检测服务调用失败: " + e.getMessage());
        }
    }

    @Override
    @Transactional
    public List<DamageInfo> saveDetectionResults(Long recordId, List<Map<String, Object>> detections) {
        List<DamageInfo> damageList = new ArrayList<>();
        
        for (Map<String, Object> det : detections) {
            DamageInfo damage = new DamageInfo();
            damage.setRecordId(recordId);
            damage.setDamageType((String) det.get("class_name"));
            damage.setDamageTypeEn((String) det.get("class_name_en"));
            damage.setDamageLevel((String) det.get("level"));
            
            Object confidence = det.get("confidence");
            if (confidence instanceof Number) {
                damage.setConfidence(((Number) confidence).doubleValue());
            }
            
            @SuppressWarnings("unchecked")
            Map<String, Object> bbox = (Map<String, Object>) det.get("bbox");
            if (bbox != null) {
                damage.setBboxX1(((Number) bbox.get("x1")).intValue());
                damage.setBboxY1(((Number) bbox.get("y1")).intValue());
                damage.setBboxX2(((Number) bbox.get("x2")).intValue());
                damage.setBboxY2(((Number) bbox.get("y2")).intValue());
            }
            
            damage.setArea(((Number) det.get("area")).intValue());
            
            Object areaRatio = det.get("area_ratio");
            if (areaRatio instanceof Number) {
                damage.setAreaRatio(BigDecimal.valueOf(((Number) areaRatio).doubleValue()));
            }
            
            damage.setStatus("detected");
            
            damageInfoMapper.insert(damage);
            damageList.add(damage);
        }
        
        return damageList;
    }

    @Override
    @Transactional
    public InspectionRecord createVideoInspectionRecord(VideoDetectionResultDTO result, VideoDetectionRequestDTO request) {
        InspectionRecord record = new InspectionRecord();
        record.setTaskId(request.getTaskId());
        record.setRoadId(request.getRoadId());
        record.setSectionId(request.getSectionId());
        record.setInspectionTime(LocalDateTime.now());
        record.setLongitude(request.getLongitude());
        record.setLatitude(request.getLatitude());
        record.setStakeNumber(request.getStakeNumber());
        record.setDirection(request.getDirection());
        record.setImagePath(result.getVideoPath());
        record.setDamageCount(result.getUniqueDamageCount());
        record.setDamageLevel(result.getOverallLevel());
        record.setAlarmNeeded(result.getAlarmNeeded() ? 1 : 0);
        record.setDetectionResult(JSONUtil.toJsonStr(result));
        
        inspectionRecordMapper.insert(record);
        
        List<Map<String, Object>> damageList = new ArrayList<>();
        for (TrackedDamage tracked : result.getTrackedDamages()) {
            Map<String, Object> damageMap = new HashMap<>();
            damageMap.put("class_name", tracked.getClassName());
            damageMap.put("confidence", tracked.getConfidences().stream()
                .mapToDouble(Double::doubleValue)
                .average()
                .orElse(0.0));
            
            Map<String, Object> bbox = new HashMap<>();
            double[] avgBbox = tracked.toDamageRecord().getBbox();
            bbox.put("x1", avgBbox[0]);
            bbox.put("y1", avgBbox[1]);
            bbox.put("x2", avgBbox[2]);
            bbox.put("y2", avgBbox[3]);
            damageMap.put("bbox", bbox);
            
            damageMap.put("area", 0);
            damageMap.put("area_ratio", 0.0);
            damageMap.put("level", result.getOverallLevel());
            
            damageList.add(damageMap);
        }
        
        List<DamageInfo> damages = saveDetectionResults(record.getId(), damageList);
        record.setDamages(damages);
        
        if (Boolean.TRUE.equals(result.getAlarmNeeded())) {
            createVideoAlarm(record, result);
        }
        
        return record;
    }

    @Override
    @Transactional
    public InspectionRecord createInspectionRecord(DetectionResultDTO result, DetectionRequestDTO request) {
        InspectionRecord record = new InspectionRecord();
        record.setTaskId(request.getTaskId());
        record.setRoadId(request.getRoadId());
        record.setSectionId(request.getSectionId());
        record.setInspectionTime(LocalDateTime.now());
        record.setLongitude(request.getLongitude());
        record.setLatitude(request.getLatitude());
        record.setStakeNumber(request.getStakeNumber());
        record.setDirection(request.getDirection());
        record.setImagePath(result.getImagePath());
        record.setResultImagePath(result.getResultImagePath());
        record.setDamageCount(result.getDetections().size());
        record.setDamageLevel(result.getOverallLevel());
        record.setAlarmNeeded(result.getAlarmNeeded() ? 1 : 0);
        record.setDetectionResult(JSONUtil.toJsonStr(result));
        
        inspectionRecordMapper.insert(record);
        
        List<DamageInfo> damages = saveDetectionResults(record.getId(), result.getDetections());
        record.setDamages(damages);
        
        if (Boolean.TRUE.equals(result.getAlarmNeeded())) {
            createAlarm(record, result);
        }
        
        return record;
    }
    
    private void createAlarm(InspectionRecord record, DetectionResultDTO result) {
        AlarmInfo alarm = new AlarmInfo();
        alarm.setAlarmCode("ALM" + IdUtil.getSnowflakeNextIdStr());
        alarm.setRecordId(record.getId());
        alarm.setRoadId(record.getRoadId());
        alarm.setSectionId(record.getSectionId());
        alarm.setAlarmType("damage_detected");
        alarm.setAlarmLevel(result.getOverallLevel());
        
        Map<String, Object> alarmInfoMap = result.getAlarmInfo();
        if (alarmInfoMap != null) {
            alarm.setAlarmReason(JSONUtil.toJsonStr(alarmInfoMap.get("reasons")));
        }
        
        alarm.setLongitude(record.getLongitude());
        alarm.setLatitude(record.getLatitude());
        alarm.setStakeNumber(record.getStakeNumber());
        alarm.setStatus("pending");
        
        alarmInfoMapper.insert(alarm);
    }
    
    private void createVideoAlarm(InspectionRecord record, VideoDetectionResultDTO result) {
        AlarmInfo alarm = new AlarmInfo();
        alarm.setAlarmCode("ALM" + IdUtil.getSnowflakeNextIdStr());
        alarm.setRecordId(record.getId());
        alarm.setRoadId(record.getRoadId());
        alarm.setSectionId(record.getSectionId());
        alarm.setAlarmType("video_damage_detected");
        alarm.setAlarmLevel(result.getOverallLevel());
        
        alarm.setAlarmReason("视频检测发现" + result.getUniqueDamageCount() + "处病害");
        
        alarm.setLongitude(record.getLongitude());
        alarm.setLatitude(record.getLatitude());
        alarm.setStakeNumber(record.getStakeNumber());
        alarm.setStatus("pending");
        
        alarmInfoMapper.insert(alarm);
    }
}
