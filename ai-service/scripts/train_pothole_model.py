"""
YOLO模型训练脚本 - 针对裂缝和坑洞优化
使用更小的batch和图像尺寸确保训练成功
"""

import os
from pathlib import Path
from ultralytics import YOLO

BASE_DIR = Path(r"C:\Users\55120\Desktop\Road Damage Detection")
DATASET_DIR = BASE_DIR / "ai-service" / "datasets" / "pothole_dataset"
DATA_YAML = DATASET_DIR / "data.yaml"
OUTPUT_DIR = BASE_DIR / "ai-service" / "runs" / "train"
WEIGHTS_DIR = BASE_DIR / "ai-service" / "weights"

TRAIN_CONFIG = {
    "model": "yolov8n.pt",
    "epochs": 50,
    "imgsz": 320,
    "batch": 4,
    "patience": 15,
    "lr0": 0.01,
    "weight_decay": 0.0005,
    "mosaic": 0.8,
    "mixup": 0.0,
    "copy_paste": 0.0,
    "degrees": 5.0,
    "translate": 0.05,
    "scale": 0.2,
    "fliplr": 0.5,
    "flipud": 0.0,
    "hsv_h": 0.01,
    "hsv_s": 0.3,
    "hsv_v": 0.2,
    "close_mosaic": 5,
    "save_period": 10,
    "conf": 0.25,
    "iou": 0.5,
}


def train_model():
    print("="*50)
    print("YOLO Model Training - Road Damage Detection")
    print("Classes: pothole, crack")
    print("="*50)
    
    if not DATA_YAML.exists():
        print(f"Error: Dataset config not found at {DATA_YAML}")
        print("Please run video_to_yolo_with_mask.py first to create the dataset.")
        return None
    
    print(f"\nDataset config: {DATA_YAML}")
    print(f"Output directory: {OUTPUT_DIR}")
    
    model = YOLO(TRAIN_CONFIG["model"])
    
    print("\nStarting training...")
    print(f"  Model: {TRAIN_CONFIG['model']}")
    print(f"  Epochs: {TRAIN_CONFIG['epochs']}")
    print(f"  Image size: {TRAIN_CONFIG['imgsz']}")
    print(f"  Batch size: {TRAIN_CONFIG['batch']}")
    
    results = model.train(
        data=str(DATA_YAML),
        epochs=TRAIN_CONFIG["epochs"],
        imgsz=TRAIN_CONFIG["imgsz"],
        batch=TRAIN_CONFIG["batch"],
        patience=TRAIN_CONFIG["patience"],
        lr0=TRAIN_CONFIG["lr0"],
        weight_decay=TRAIN_CONFIG["weight_decay"],
        mosaic=TRAIN_CONFIG["mosaic"],
        mixup=TRAIN_CONFIG["mixup"],
        copy_paste=TRAIN_CONFIG["copy_paste"],
        degrees=TRAIN_CONFIG["degrees"],
        translate=TRAIN_CONFIG["translate"],
        scale=TRAIN_CONFIG["scale"],
        fliplr=TRAIN_CONFIG["fliplr"],
        flipud=TRAIN_CONFIG["flipud"],
        hsv_h=TRAIN_CONFIG["hsv_h"],
        hsv_s=TRAIN_CONFIG["hsv_s"],
        hsv_v=TRAIN_CONFIG["hsv_v"],
        close_mosaic=TRAIN_CONFIG["close_mosaic"],
        save_period=TRAIN_CONFIG["save_period"],
        conf=TRAIN_CONFIG["conf"],
        iou=TRAIN_CONFIG["iou"],
        project=str(OUTPUT_DIR.parent),
        name="train",
        exist_ok=True,
        verbose=True,
        optimizer="SGD",
        cos_lr=True,
        workers=2,
        device="cpu",
        cache=False,
    )
    
    WEIGHTS_DIR.mkdir(parents=True, exist_ok=True)
    
    best_weights = OUTPUT_DIR / "weights" / "best.pt"
    if best_weights.exists():
        target_path = WEIGHTS_DIR / "best.pt"
        import shutil
        shutil.copy(best_weights, target_path)
        print(f"\nBest weights saved to: {target_path}")
    
    print("\n" + "="*50)
    print("Training completed!")
    print("="*50)
    
    return results


def validate_model():
    print("\n" + "="*50)
    print("Model Validation")
    print("="*50)
    
    best_weights = WEIGHTS_DIR / "best.pt"
    if not best_weights.exists():
        print(f"Error: Trained model not found at {best_weights}")
        return None
    
    model = YOLO(str(best_weights))
    
    results = model.val(data=str(DATA_YAML), imgsz=TRAIN_CONFIG["imgsz"])
    
    print(f"\nmAP50: {results.box.map50:.4f}")
    print(f"mAP50-95: {results.box.map:.4f}")
    print(f"Precision: {results.box.mp:.4f}")
    print(f"Recall: {results.box.mr:.4f}")
    
    return results


if __name__ == "__main__":
    train_model()
    validate_model()
