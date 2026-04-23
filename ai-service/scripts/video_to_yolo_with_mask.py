"""
视频数据集转YOLO格式数据集转换脚本
支持裂缝和坑洞两种病害类型检测
使用mask视频提取精确标注
"""

import os
import cv2
import random
import gc
import numpy as np
from pathlib import Path
from datetime import datetime

BASE_DIR = Path(r"C:\Users\55120\Desktop\Road Damage Detection")
VIDEO_DIR = BASE_DIR / "pothole_video" / "pothole_video"
OUTPUT_DIR = BASE_DIR / "ai-service" / "datasets" / "pothole_dataset"

CLASS_NAMES = ["pothole", "crack"]
CLASS_IDS = {"pothole": 0, "crack": 1}

FRAME_INTERVAL = 2
TRAIN_RATIO = 0.8
MIN_AREA = 50

train_count = 0
val_count = 0


def create_directories():
    for split in ["train", "val"]:
        (OUTPUT_DIR / "images" / split).mkdir(parents=True, exist_ok=True)
        (OUTPUT_DIR / "labels" / split).mkdir(parents=True, exist_ok=True)
    print(f"Created directory structure at {OUTPUT_DIR}")


def get_video_pairs():
    pairs = []
    for split in ["train", "test"]:
        rgb_dir = VIDEO_DIR / split / "rgb"
        mask_dir = VIDEO_DIR / split / "mask"
        
        if rgb_dir.exists() and mask_dir.exists():
            for rgb_file in sorted(rgb_dir.glob("*.mp4")):
                mask_file = mask_dir / rgb_file.name
                if mask_file.exists():
                    pairs.append({
                        "rgb": rgb_file,
                        "mask": mask_file,
                        "split": split
                    })
    
    print(f"Found {len(pairs)} video pairs")
    return pairs


def analyze_damage_type(mask_frame):
    """分析mask图像判断病害类型"""
    if mask_frame is None:
        return "pothole"
    
    if len(mask_frame.shape) == 3:
        gray = cv2.cvtColor(mask_frame, cv2.COLOR_BGR2GRAY)
    else:
        gray = mask_frame
    
    _, binary = cv2.threshold(gray, 1, 255, cv2.THRESH_BINARY)
    
    contours, _ = cv2.findContours(binary, cv2.RETR_EXTERNAL, cv2.CHAIN_APPROX_SIMPLE)
    
    if not contours:
        return "pothole"
    
    max_contour = max(contours, key=cv2.contourArea)
    x, y, w, h = cv2.boundingRect(max_contour)
    
    aspect_ratio = w / h if h > 0 else 1
    area = w * h
    
    perimeter = cv2.arcLength(max_contour, True)
    if perimeter > 0:
        circularity = 4 * 3.14159 * area / (perimeter * perimeter)
    else:
        circularity = 0
    
    if aspect_ratio > 3 or aspect_ratio < 0.33:
        return "crack"
    elif circularity < 0.4:
        return "crack"
    else:
        return "pothole"


def extract_bboxes_from_mask(mask_frame, damage_type="pothole"):
    if mask_frame is None:
        return []
    
    if len(mask_frame.shape) == 3:
        gray = cv2.cvtColor(mask_frame, cv2.COLOR_BGR2GRAY)
    else:
        gray = mask_frame
    
    _, binary = cv2.threshold(gray, 1, 255, cv2.THRESH_BINARY)
    
    contours, _ = cv2.findContours(binary, cv2.RETR_EXTERNAL, cv2.CHAIN_APPROX_SIMPLE)
    
    bboxes = []
    img_height, img_width = gray.shape
    class_id = CLASS_IDS.get(damage_type, 0)
    
    for contour in contours:
        x, y, w, h = cv2.boundingRect(contour)
        area = w * h
        
        if area >= MIN_AREA:
            x_center = (x + w / 2) / img_width
            y_center = (y + h / 2) / img_height
            norm_w = w / img_width
            norm_h = h / img_height
            
            x_center = max(0, min(1, x_center))
            y_center = max(0, min(1, y_center))
            norm_w = max(0, min(1, norm_w))
            norm_h = max(0, min(1, norm_h))
            
            bboxes.append(f"{class_id} {x_center:.6f} {y_center:.6f} {norm_w:.6f} {norm_h:.6f}")
    
    return bboxes


def save_frame(frame, annotations, video_name, frame_idx):
    global train_count, val_count
    
    random_val = random.random()
    split = "train" if random_val < TRAIN_RATIO else "val"
    
    base_name = f"{video_name}_frame{frame_idx:04d}"
    
    img_path = OUTPUT_DIR / "images" / split / f"{base_name}.jpg"
    cv2.imwrite(str(img_path), frame, [cv2.IMWRITE_JPEG_QUALITY, 95])
    
    label_path = OUTPUT_DIR / "labels" / split / f"{base_name}.txt"
    with open(label_path, "w") as f:
        for ann in annotations:
            f.write(ann + "\n")
    
    if split == "train":
        train_count += 1
    else:
        val_count += 1
    
    return 1 if annotations else 0


def process_video_pair(rgb_path, mask_path):
    global train_count, val_count
    
    rgb_cap = cv2.VideoCapture(str(rgb_path))
    mask_cap = cv2.VideoCapture(str(mask_path))
    
    if not rgb_cap.isOpened() or not mask_cap.isOpened():
        print(f"Cannot open video pair: {rgb_path.name}")
        rgb_cap.release()
        mask_cap.release()
        return 0, 0
    
    frame_count = 0
    frames_with_damage = 0
    
    while True:
        rgb_ret, rgb_frame = rgb_cap.read()
        mask_ret, mask_frame = mask_cap.read()
        
        if not rgb_ret or not mask_ret:
            break
        
        if frame_count % FRAME_INTERVAL == 0:
            damage_type = analyze_damage_type(mask_frame)
            bboxes = extract_bboxes_from_mask(mask_frame, damage_type)
            
            save_frame(rgb_frame, bboxes, rgb_path.stem, frame_count)
            
            if bboxes:
                frames_with_damage += 1
        
        frame_count += 1
    
    rgb_cap.release()
    mask_cap.release()
    
    return frame_count // FRAME_INTERVAL, frames_with_damage


def process_all_videos(video_pairs):
    total_frames = 0
    total_with_damage = 0
    
    for i, pair in enumerate(video_pairs):
        print(f"Processing video {i+1}/{len(video_pairs)}: {pair['rgb'].name}")
        
        frames, damage_frames = process_video_pair(pair["rgb"], pair["mask"])
        total_frames += frames
        total_with_damage += damage_frames
        
        if (i + 1) % 50 == 0:
            print(f"  Progress: {i+1}/{len(video_pairs)} videos, {total_frames} frames, {total_with_damage} with damage")
            gc.collect()
    
    return total_frames, total_with_damage


def create_data_yaml():
    yaml_content = f"""# Road Damage Detection Dataset
# Generated from pothole_video dataset with mask annotations
# Supports pothole and crack detection
# Date: {datetime.now().strftime('%Y-%m-%d %H:%M:%S')}

path: {OUTPUT_DIR.absolute().as_posix()}
train: images/train
val: images/val

nc: {len(CLASS_NAMES)}
names: {CLASS_NAMES}
"""
    
    yaml_path = OUTPUT_DIR / "data.yaml"
    with open(yaml_path, "w", encoding="utf-8") as f:
        f.write(yaml_content)
    
    print(f"Created data.yaml at {yaml_path}")
    return yaml_path


def main():
    global train_count, val_count
    
    print("="*50)
    print("Video to YOLO Dataset Converter")
    print("Supports: Pothole and Crack Detection")
    print("="*50)
    
    create_directories()
    
    video_pairs = get_video_pairs()
    if not video_pairs:
        print("No video pairs found!")
        return
    
    print(f"\nProcessing {len(video_pairs)} video pairs...")
    total_frames, total_with_damage = process_all_videos(video_pairs)
    
    print("\n" + "="*50)
    print("Dataset Statistics")
    print("="*50)
    print(f"Total frames extracted: {total_frames}")
    print(f"Frames with damage: {total_with_damage}")
    print(f"Training samples: {train_count}")
    print(f"Validation samples: {val_count}")
    print(f"Classes: {CLASS_NAMES}")
    print("="*50)
    
    print("\nCreating data.yaml...")
    yaml_path = create_data_yaml()
    
    print("\n" + "="*50)
    print("Conversion completed successfully!")
    print("="*50)
    print(f"\nDataset location: {OUTPUT_DIR}")
    print(f"Config file: {yaml_path}")
    print("\nTo train your model, run:")
    print(f"  python scripts/train_pothole_model.py")


if __name__ == "__main__":
    main()
