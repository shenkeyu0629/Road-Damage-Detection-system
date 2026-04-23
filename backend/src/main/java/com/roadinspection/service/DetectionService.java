package com.roadinspection.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.roadinspection.dto.DetectionRequestDTO;
import com.roadinspection.dto.DetectionResultDTO;
import com.roadinspection.dto.VideoDetectionRequestDTO;
import com.roadinspection.dto.VideoDetectionResultDTO;
import com.roadinspection.entity.DamageInfo;
import com.roadinspection.entity.InspectionRecord;

import java.util.List;
import java.util.Map;

public interface DetectionService {
    DetectionResultDTO detectImage(DetectionRequestDTO requestDTO);
    VideoDetectionResultDTO detectVideo(VideoDetectionRequestDTO requestDTO);
    List<DamageInfo> saveDetectionResults(Long recordId, List<Map<String, Object>> detections);
    InspectionRecord createInspectionRecord(DetectionResultDTO result, DetectionRequestDTO request);
    InspectionRecord createVideoInspectionRecord(VideoDetectionResultDTO result, VideoDetectionRequestDTO request);
}
