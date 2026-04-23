from __future__ import annotations

from pydantic import BaseModel, Field
from typing import Any
from datetime import datetime
from enum import Enum


class DamageLevel(str, Enum):
    minor = "minor"
    moderate = "moderate"
    severe = "severe"
    none = "none"


class BoundingBox(BaseModel):
    x1: int
    y1: int
    x2: int
    y2: int


class LevelInfo(BaseModel):
    name: str
    score: int
    color: str
    description: str


class Detection(BaseModel):
    id: int
    class_id: int
    class_name: str
    class_name_en: str
    level: str
    level_info: LevelInfo
    confidence: float
    bbox: BoundingBox
    area: int
    area_ratio: float


class DamageSummary(BaseModel):
    total_count: int
    total_area: int
    total_area_ratio: float
    by_class: dict[str, Any]
    by_level: dict[str, int]


class AlarmInfo(BaseModel):
    reasons: list[str]
    recommendation: str


class DetectionResult(BaseModel):
    image_path: str
    image_width: int
    image_height: int
    detections: list[Detection]
    damage_summary: DamageSummary
    overall_level: str
    alarm_needed: bool
    alarm_info: AlarmInfo
    detection_time: str
    result_image_path: str | None = None
    uploaded_file_path: str | None = None


class VideoDetectionResult(BaseModel):
    video_path: str
    output_path: str
    total_frames: int
    processed_frames: int
    fps: int
    duration: float
    all_detections: list[dict[str, Any]]
    summary: dict[str, Any]
    detection_time: str


class TrainRequest(BaseModel):
    data_yaml: str
    model_name: str = "yolov8n.pt"
    epochs: int = 100
    imgsz: int = 640
    batch: int = 16
    device: str = "0"


class TrainResponse(BaseModel):
    success: bool
    project: str
    name: str
    best_weights: str | None
    results: str


class ValidateRequest(BaseModel):
    model_path: str
    data_yaml: str


class ValidateResponse(BaseModel):
    success: bool
    model_path: str
    metrics: dict[str, float | None]


class HealthResponse(BaseModel):
    status: str
    model_loaded: bool
    timestamp: str


class DetectionRequest(BaseModel):
    image_path: str
    conf_threshold: float = Field(default=0.25, ge=0.0, le=1.0)
    draw_result: bool = True


class VideoDetectionRequest(BaseModel):
    video_path: str
    conf_threshold: float = Field(default=0.25, ge=0.0, le=1.0)
    frame_interval: int = Field(default=5, ge=1)
