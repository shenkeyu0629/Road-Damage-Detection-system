"""
视频数据集转YOLO格式数据集转换脚本
将pothole_video目录下的视频转换为YOLO训练数据集
"""

import os
import cv2
import shutil
import random
from pathlib import Path
from ultralytics import YOLO

BASE_DIR = Path(r"C:\Users\55120\Desktop\Road Damage Detection")
VIDEO_DIR = BASE_DIR / "pothole_video" / "pothole_video"
OUTPUT_DIR = BASE_DIR / "ai-service" / "datasets" / "road_damage_dataset"
MODEL_PATH = BASE_DIR / "ai-service" / "weights" / "best.pt"

CLASS_NAMES = {
    0: "Longitudinal Crack",
    1: "Transverse Crack", 
    2: "Alligator Crack",
    3: "Other Corruption",
    4: "Pothole"
}

FRAME_INTERVAL = 10
CONF_THRESHOLD = 0.3
TRAIN_RATIO = 0.8


def create_directories():
    for split in ["train", "val"]:
        (OUTPUT_DIR / "images" / split).mkdir(parents=True, exist_ok=True)
        (OUTPUT_DIR / "labels" / split).mkdir(parents=True, exist_ok=True)
    print(f"Created directory structure at {OUTPUT_DIR}")


def get_all_videos():
    videos = []
    for split in ["train", "test"]:
        rgb_dir = VIDEO_DIR / split / "rgb"
        if rgb_dir.exists():
            for video_file in rgb_dir.glob("*.mp4"):
                videos.append(video_file)
    print(f"Found {len(videos)} videos")
    return videos


def extract_frames(video_path, frame_interval=FRAME_INTERVAL):
    frames = []
    cap = cv2.VideoCapture(str(video_path))
    
    if not cap.isOpened():
        print(f"Cannot open video: {video_path}")
        return frames
    
    frame_count = 0
    while True:
        ret, frame = cap.read()
        if not ret:
            break
        
        if frame_count % frame_interval == 0:
            frames.append((frame_count, frame.copy()))
        
        frame_count += 1
    
    cap.release()
    return frames


def auto_label_frame(model, frame, conf_threshold=CONF_THRESHOLD):
    results = model(frame, conf=conf_threshold, verbose=False)
    
    annotations = []
    if results and len(results) > 0:
        result = results[0]
        boxes = result.boxes
        
        if boxes is not None:
            img_height, img_width = frame.shape[:2]
            
            for box in boxes:
                x1, y1, x2, y2 = box.xyxy[0].cpu().numpy()
                class_id = int(box.cls[0])
                confidence = float(box.conf[0])
                
                x_center = ((x1 + x2) / 2) / img_width
                y_center = ((y1 + y2) / 2) / img_height
                width = (x2 - x1) / img_width
                height = (y2 - y1) / img_height
                
                x_center = max(0, min(1, x_center))
                y_center = max(0, min(1, y_center))
                width = max(0, min(1, width))
                height = max(0, min(1, height))
                
                annotations.append(f"{class_id} {x_center:.6f} {y_center:.6f} {width:.6f} {height:.6f}")
    
    return annotations


def process_videos(videos, model):
    all_frames_data = []
    
    for i, video_path in enumerate(videos):
        print(f"Processing video {i+1}/{len(videos)}: {video_path.name}")
        
        frames = extract_frames(video_path)
        
        for frame_idx, frame in frames:
            annotations = auto_label_frame(model, frame)
            
            all_frames_data.append({
                "video_name": video_path.stem,
                "frame_idx": frame_idx,
                "frame": frame,
                "annotations": annotations
            })
    
    return all_frames_data


def save_dataset(frames_data):
    random.shuffle(frames_data)
    
    split_idx = int(len(frames_data) * TRAIN_RATIO)
    train_data = frames_data[:split_idx]
    val_data = frames_data[split_idx:]
    
    print(f"\nSaving {len(train_data)} training samples and {len(val_data)} validation samples")
    
    for split, data in [("train", train_data), ("val", val_data)]:
        for i, item in enumerate(data):
            base_name = f"{item['video_name']}_frame{item['frame_idx']:04d}"
            
            img_path = OUTPUT_DIR / "images" / split / f"{base_name}.jpg"
            cv2.imwrite(str(img_path), item["frame"])
            
            label_path = OUTPUT_DIR / "labels" / split / f"{base_name}.txt"
            with open(label_path, "w") as f:
                for ann in item["annotations"]:
                    f.write(ann + "\n")
    
    print(f"Dataset saved to {OUTPUT_DIR}")


def create_data_yaml():
    yaml_content = f"""# Road Damage Detection Dataset
# Generated from pothole_video dataset

path: {OUTPUT_DIR.absolute()}
train: images/train
val: images/val

nc: {len(CLASS_NAMES)}
names: {list(CLASS_NAMES.values())}
"""
    
    yaml_path = OUTPUT_DIR / "data.yaml"
    with open(yaml_path, "w", encoding="utf-8") as f:
        f.write(yaml_content)
    
    print(f"Created data.yaml at {yaml_path}")
    return yaml_path


def print_statistics(frames_data):
    total_frames = len(frames_data)
    frames_with_damage = sum(1 for f in frames_data if f["annotations"])
    total_annotations = sum(len(f["annotations"]) for f in frames_data)
    
    class_counts = {i: 0 for i in range(len(CLASS_NAMES))}
    for f in frames_data:
        for ann in f["annotations"]:
            class_id = int(ann.split()[0])
            class_counts[class_id] = class_counts.get(class_id, 0) + 1
    
    print("\n" + "="*50)
    print("Dataset Statistics")
    print("="*50)
    print(f"Total frames extracted: {total_frames}")
    print(f"Frames with damage: {frames_with_damage}")
    print(f"Total annotations: {total_annotations}")
    print(f"\nAnnotations by class:")
    for class_id, count in class_counts.items():
        class_name = CLASS_NAMES.get(class_id, f"Unknown({class_id})")
        print(f"  {class_name}: {count}")
    print("="*50)


def main():
    print("="*50)
    print("Video to YOLO Dataset Converter")
    print("="*50)
    
    create_directories()
    
    videos = get_all_videos()
    if not videos:
        print("No videos found!")
        return
    
    print(f"\nLoading YOLO model from {MODEL_PATH}")
    model = YOLO(str(MODEL_PATH))
    
    print("\nProcessing videos...")
    frames_data = process_videos(videos, model)
    
    if not frames_data:
        print("No frames extracted!")
        return
    
    print_statistics(frames_data)
    
    print("\nSaving dataset...")
    save_dataset(frames_data)
    
    print("\nCreating data.yaml...")
    yaml_path = create_data_yaml()
    
    print("\n" + "="*50)
    print("Conversion completed successfully!")
    print("="*50)
    print(f"\nDataset location: {OUTPUT_DIR}")
    print(f"Config file: {yaml_path}")
    print("\nTo train your model, run:")
    print(f"  yolo detect train data={yaml_path} model=yolov8n.pt epochs=100")


if __name__ == "__main__":
    main()
