from __future__ import annotations

import os
import cv2
import numpy as np
from pathlib import Path
from typing import Any
from datetime import datetime
import json
import torch
from ultralytics import YOLO
from PIL import Image, ImageDraw, ImageFont

from app.core.config import (
    MODEL_PATH, DAMAGE_CLASSES, DAMAGE_LEVELS, ALARM_THRESHOLDS,
    UPLOAD_DIR, RESULT_DIR
)


class DamageDetector:
    _instance = None
    _model = None
    
    def __new__(cls):
        if cls._instance is None:
            cls._instance = super().__new__(cls)
        return cls._instance
    
    def __init__(self):
        if self._model is None:
            self.load_model()
    
    def load_model(self, model_path: str = None):
        model_path = model_path or MODEL_PATH
        if os.path.exists(model_path):
            self._model = YOLO(model_path)
            print(f"Model loaded from {model_path}")
        else:
            self._model = YOLO("yolov8n.pt")
            print(f"Pre-trained model loaded (yolov8n.pt)")
    
    @property
    def model(self):
        return self._model
    
    def detect(self, image_path: str, conf_threshold: float = 0.25) -> Dict[str, Any]:
        if self._model is None:
            raise RuntimeError("Model not loaded")
        
        results = self._model(image_path, conf=conf_threshold, verbose=False)
        
        image = cv2.imread(image_path)
        if image is None:
            raise ValueError(f"Cannot read image: {image_path}")
        
        image_height, image_width = image.shape[:2]
        total_image_area = image_width * image_height
        
        detections = []
        damage_summary = {
            "total_count": 0,
            "total_area": 0,
            "by_class": {},
            "by_level": {"minor": 0, "moderate": 0, "severe": 0, "none": 0},
        }
        
        for result in results:
            boxes = result.boxes
            if boxes is None:
                continue
            
            for i, box in enumerate(boxes):
                x1, y1, x2, y2 = box.xyxy[0].cpu().numpy()
                confidence = float(box.conf[0])
                class_id = int(box.cls[0])
                
                class_info = DAMAGE_CLASSES.get(class_id, {
                    "name": f"未知类型{class_id}",
                    "name_en": f"Unknown{class_id}",
                    "level": "minor"
                })
                
                area = int((x2 - x1) * (y2 - y1))
                area_ratio = area / total_image_area
                
                detection = {
                    "id": i,
                    "class_id": class_id,
                    "class_name": class_info["name"],
                    "class_name_en": class_info["name_en"],
                    "level": class_info["level"],
                    "level_info": DAMAGE_LEVELS.get(class_info["level"], DAMAGE_LEVELS["minor"]),
                    "confidence": round(confidence, 4),
                    "bbox": {
                        "x1": int(x1),
                        "y1": int(y1),
                        "x2": int(x2),
                        "y2": int(y2),
                    },
                    "area": area,
                    "area_ratio": round(area_ratio, 6),
                }
                detections.append(detection)
                
                damage_summary["total_count"] += 1
                damage_summary["total_area"] += area
                
                class_key = str(class_id)
                if class_key not in damage_summary["by_class"]:
                    damage_summary["by_class"][class_key] = {
                        "name": class_info["name"],
                        "count": 0,
                        "total_area": 0,
                    }
                damage_summary["by_class"][class_key]["count"] += 1
                damage_summary["by_class"][class_key]["total_area"] += area
                damage_summary["by_level"][class_info["level"]] += 1
        
        damage_summary["total_area_ratio"] = round(
            damage_summary["total_area"] / total_image_area, 6
        ) if total_image_area > 0 else 0
        
        overall_level, alarm_needed, alarm_info = self._evaluate_damage_level(
            damage_summary
        )
        
        return {
            "image_path": image_path,
            "image_width": image_width,
            "image_height": image_height,
            "detections": detections,
            "damage_summary": damage_summary,
            "overall_level": overall_level,
            "alarm_needed": alarm_needed,
            "alarm_info": alarm_info,
            "detection_time": datetime.now().isoformat(),
        }
    
    def _evaluate_damage_level(self, summary: dict) -> tuple:
        overall_score = 0
        alarm_reasons = []
        
        if summary["by_level"]["severe"] >= ALARM_THRESHOLDS["severe_count"]:
            overall_score = 3
            alarm_reasons.append(f"发现{summary['by_level']['severe']}处严重病害")
        
        if summary["by_level"]["moderate"] >= ALARM_THRESHOLDS["moderate_count"]:
            if overall_score < 2:
                overall_score = 2
            alarm_reasons.append(f"发现{summary['by_level']['moderate']}处中等病害")
        
        if summary["total_area_ratio"] >= ALARM_THRESHOLDS["total_damage_area_ratio"]:
            if overall_score < 2:
                overall_score = 2
            alarm_reasons.append(f"病害总面积占比{summary['total_area_ratio']*100:.2f}%")
        
        if overall_score == 0 and summary["total_count"] > 0:
            overall_score = 1
        elif summary["total_count"] == 0:
            overall_score = 0
        
        level_names = {0: "none", 1: "minor", 2: "moderate", 3: "severe"}
        overall_level = level_names.get(overall_score, "minor")
        
        alarm_needed = len(alarm_reasons) > 0
        alarm_info = {
            "reasons": alarm_reasons,
            "recommendation": DAMAGE_LEVELS[overall_level]["description"] if overall_level != "none" else "路况良好"
        }
        
        return overall_level, alarm_needed, alarm_info
    
    def draw_detections(
        self, 
        image_path: str, 
        detections: list[dict], 
        output_path: str | None = None
    ) -> str:
        image = cv2.imread(image_path)
        if image is None:
            raise ValueError(f"Cannot read image: {image_path}")
        
        level_names = {
            "severe": "高危",
            "moderate": "中等",
            "minor": "轻微",
            "none": "无"
        }
        
        for det in detections:
            bbox = det["bbox"]
            level_info = det["level_info"]
            
            color_bgr = self._hex_to_bgr(level_info["color"])
            color_rgb = self._hex_to_rgb(level_info["color"])
            
            x1, y1, x2, y2 = bbox["x1"], bbox["y1"], bbox["x2"], bbox["y2"]
            cv2.rectangle(image, (x1, y1), (x2, y2), color_bgr, 3)
            
            level_text = level_names.get(det["level"], "轻微")
            label = f"{det['class_name']}[{level_text}] {det['confidence']*100:.0f}%"
            
            image = self._draw_chinese_text(
                image, label, (x1 + 5, y1 - 28),
                font_size=18, color=(255, 255, 255), bg_color=color_rgb
            )
        
        if output_path is None:
            filename = Path(image_path).stem
            output_path = str(RESULT_DIR / f"{filename}_result.jpg")
        
        cv2.imwrite(output_path, image)
        return output_path
    
    def _hex_to_bgr(self, hex_color: str) -> tuple:
        hex_color = hex_color.lstrip('#')
        r, g, b = tuple(int(hex_color[i:i+2], 16) for i in (0, 2, 4))
        return (b, g, r)
    
    def _hex_to_rgb(self, hex_color: str) -> tuple:
        hex_color = hex_color.lstrip('#')
        r, g, b = tuple(int(hex_color[i:i+2], 16) for i in (0, 2, 4))
        return (r, g, b)
    
    def _get_chinese_font(self, size: int = 20):
        font_paths = [
            "C:/Windows/Fonts/msyh.ttc",
            "C:/Windows/Fonts/simhei.ttf",
            "C:/Windows/Fonts/simsun.ttc",
            "/usr/share/fonts/truetype/droid/DroidSansFallbackFull.ttf",
        ]
        for font_path in font_paths:
            if os.path.exists(font_path):
                try:
                    return ImageFont.truetype(font_path, size)
                except:
                    continue
        return ImageFont.load_default()
    
    def _draw_chinese_text(self, img: np.ndarray, text: str, position: tuple, 
                           font_size: int = 20, color: tuple = (255, 255, 255),
                           bg_color: tuple = None) -> np.ndarray:
        img_pil = Image.fromarray(cv2.cvtColor(img, cv2.COLOR_BGR2RGB))
        draw = ImageDraw.Draw(img_pil)
        font = self._get_chinese_font(font_size)
        
        x, y = position
        bbox = draw.textbbox((x, y), text, font=font)
        text_width = bbox[2] - bbox[0]
        text_height = bbox[3] - bbox[1]
        
        if bg_color:
            draw.rectangle([x - 2, y - 2, x + text_width + 4, y + text_height + 4], 
                          fill=bg_color)
        
        draw.text((x, y), text, font=font, fill=color)
        
        return cv2.cvtColor(np.array(img_pil), cv2.COLOR_RGB2BGR)
    
    def detect_video(
        self, 
        video_path: str, 
        output_path: str | None = None,
        conf_threshold: float = 0.25,
        frame_interval: int = 3
    ) -> dict[str, Any]:
        cap = cv2.VideoCapture(video_path)
        if not cap.isOpened():
            raise ValueError(f"Cannot open video: {video_path}")
        
        fps = int(cap.get(cv2.CAP_PROP_FPS))
        total_frames = int(cap.get(cv2.CAP_PROP_FRAME_COUNT))
        width = int(cap.get(cv2.CAP_PROP_FRAME_WIDTH))
        height = int(cap.get(cv2.CAP_PROP_FRAME_HEIGHT))
        
        if output_path is None:
            filename = Path(video_path).stem
            output_path = str(RESULT_DIR / f"{filename}_result.webm")
        
        fourcc = cv2.VideoWriter_fourcc(*'VP80')
        out = cv2.VideoWriter(output_path, fourcc, fps, (width, height))
        
        all_detections = []
        frame_count = 0
        processed_frames = 0
        
        tracked_damages = []
        next_track_id = 0
        
        prev_detections = []
        
        while True:
            ret, frame = cap.read()
            if not ret:
                break
            
            frame_detections = []
            
            if frame_count % frame_interval == 0:
                temp_path = str(UPLOAD_DIR / f"temp_frame_{frame_count}.jpg")
                cv2.imwrite(temp_path, frame)
                
                result = self.detect(temp_path, conf_threshold)
                frame_detections = result["detections"]
                
                for det in frame_detections:
                    matched = False
                    for prev_det in prev_detections:
                        iou = self._calculate_iou(det["bbox"], prev_det["bbox"])
                        if iou > 0.3 and det["class_id"] == prev_det["class_id"]:
                            det["track_id"] = prev_det.get("track_id", -1)
                            matched = True
                            break
                    
                    if not matched:
                        det["track_id"] = next_track_id
                        next_track_id += 1
                        
                        tracked_damages.append({
                            "track_id": det["track_id"],
                            "class_id": det["class_id"],
                            "class_name": det["class_name"],
                            "level": det["level"],
                            "first_frame": frame_count,
                            "first_time": frame_count / fps,
                            "max_confidence": det["confidence"],
                            "detections": [det]
                        })
                    else:
                        for td in tracked_damages:
                            if td["track_id"] == det["track_id"]:
                                td["max_confidence"] = max(td["max_confidence"], det["confidence"])
                                td["detections"].append(det)
                                break
                
                prev_detections = frame_detections.copy() if hasattr(frame_detections, 'copy') else list(frame_detections)
                
                if frame_detections:
                    all_detections.append({
                        "frame": frame_count,
                        "time": frame_count / fps,
                        "detections": frame_detections,
                        "overall_level": result["overall_level"],
                    })
                
                os.remove(temp_path)
                processed_frames += 1
            
            annotated_frame = self._draw_on_frame(frame, frame_detections)
            out.write(annotated_frame)
            
            frame_count += 1
        
        cap.release()
        out.release()
        
        summary = self._summarize_video_detections(all_detections, tracked_damages)
        
        return {
            "video_path": video_path,
            "output_path": output_path,
            "total_frames": total_frames,
            "processed_frames": processed_frames,
            "fps": fps,
            "duration": total_frames / fps,
            "all_detections": all_detections,
            "tracked_damages": tracked_damages,
            "summary": summary,
            "detection_time": datetime.now().isoformat(),
        }
    
    def _calculate_iou(self, bbox1: dict, bbox2: dict) -> float:
        x1_1, y1_1, x2_1, y2_1 = bbox1["x1"], bbox1["y1"], bbox1["x2"], bbox1["y2"]
        x1_2, y1_2, x2_2, y2_2 = bbox2["x1"], bbox2["y1"], bbox2["x2"], bbox2["y2"]
        
        x1_i = max(x1_1, x1_2)
        y1_i = max(y1_1, y1_2)
        x2_i = min(x2_1, x2_2)
        y2_i = min(y2_1, y2_2)
        
        if x2_i < x1_i or y2_i < y1_i:
            return 0.0
        
        intersection = (x2_i - x1_i) * (y2_i - y1_i)
        
        area1 = (x2_1 - x1_1) * (y2_1 - y1_1)
        area2 = (x2_2 - x1_2) * (y2_2 - y1_2)
        
        union = area1 + area2 - intersection
        
        return intersection / union if union > 0 else 0.0
        
        return {
            "video_path": video_path,
            "output_path": output_path,
            "total_frames": total_frames,
            "processed_frames": processed_frames,
            "fps": fps,
            "duration": total_frames / fps,
            "all_detections": all_detections,
            "summary": summary,
            "detection_time": datetime.now().isoformat(),
        }
    
    def _draw_on_frame(self, frame: np.ndarray, detections: list[dict]) -> np.ndarray:
        level_names = {
            "severe": "高危",
            "moderate": "中等",
            "minor": "轻微",
            "none": "无"
        }
        
        for det in detections:
            bbox = det["bbox"]
            level_info = det["level_info"]
            color_bgr = self._hex_to_bgr(level_info["color"])
            color_rgb = self._hex_to_rgb(level_info["color"])
            
            x1, y1, x2, y2 = bbox["x1"], bbox["y1"], bbox["x2"], bbox["y2"]
            cv2.rectangle(frame, (x1, y1), (x2, y2), color_bgr, 3)
            
            level_text = level_names.get(det["level"], "轻微")
            label = f"{det['class_name']}[{level_text}] {det['confidence']*100:.0f}%"
            
            frame = self._draw_chinese_text(
                frame, label, (x1 + 5, y1 - 28),
                font_size=18, color=(255, 255, 255), bg_color=color_rgb
            )
        
        return frame
    
    def _summarize_video_detections(self, all_detections: list[dict], tracked_damages: list[dict] = None) -> dict:
        summary = {
            "total_detections": 0,
            "unique_damages": 0,
            "frames_with_damage": len(all_detections),
            "by_class": {},
            "by_level": {"minor": 0, "moderate": 0, "severe": 0},
            "max_severity": "none",
            "damage_list": [],
        }
        
        for frame_det in all_detections:
            for det in frame_det["detections"]:
                summary["total_detections"] += 1
                
                class_id = det["class_id"]
                if class_id not in summary["by_class"]:
                    summary["by_class"][class_id] = {
                        "name": det["class_name"],
                        "count": 0,
                        "unique_count": 0,
                    }
                summary["by_class"][class_id]["count"] += 1
                summary["by_level"][det["level"]] += 1
        
        if tracked_damages:
            summary["unique_damages"] = len(tracked_damages)
            
            for td in tracked_damages:
                class_id = td["class_id"]
                if class_id in summary["by_class"]:
                    summary["by_class"][class_id]["unique_count"] += 1
                
                summary["damage_list"].append({
                    "track_id": td["track_id"],
                    "class_name": td["class_name"],
                    "level": td["level"],
                    "first_frame": td["first_frame"],
                    "first_time": round(td["first_time"], 2),
                    "max_confidence": round(td["max_confidence"], 4),
                    "detection_count": len(td["detections"]),
                })
        else:
            summary["unique_damages"] = summary["total_detections"]
        
        if summary["by_level"]["severe"] > 0:
            summary["max_severity"] = "severe"
        elif summary["by_level"]["moderate"] > 0:
            summary["max_severity"] = "moderate"
        elif summary["by_level"]["minor"] > 0:
            summary["max_severity"] = "minor"
        
        return summary


detector = DamageDetector()
