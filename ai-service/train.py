from __future__ import annotations

import os
import sys
import shutil
from pathlib import Path

sys.path.insert(0, str(Path(__file__).parent))

from ultralytics import YOLO
from app.core.config import BASE_DIR

def train_model(
    data_yaml: str,
    model_name: str = "yolov8n.pt",
    epochs: int = 100,
    imgsz: int = 640,
    batch: int = 16,
    device: str = "0"
):
    model = YOLO(model_name)
    
    results = model.train(
        data=data_yaml,
        epochs=epochs,
        imgsz=imgsz,
        batch=batch,
        device=device,
        project=str(BASE_DIR / "runs" / "detect"),
        name="road_damage",
        exist_ok=True,
        pretrained=True,
        optimizer="auto",
        lr0=0.01,
        lrf=0.01,
        momentum=0.937,
        weight_decay=0.0005,
        warmup_epochs=3,
        warmup_momentum=0.8,
        warmup_bias_lr=0.1,
        box=7.5,
        cls=0.5,
        dfl=1.5,
        pose=12.0,
        kobj=1.0,
        label_smoothing=0.0,
        nbs=64,
        hsv_h=0.015,
        hsv_s=0.7,
        hsv_v=0.4,
        degrees=0.0,
        translate=0.1,
        scale=0.5,
        shear=0.0,
        perspective=0.0,
        flipud=0.0,
        fliplr=0.5,
        mosaic=1.0,
        mixup=0.0,
        copy_paste=0.0,
        auto_augment="randaugment",
        erasing=0.4,
        crop_fraction=1.0,
        patience=50,
        save=True,
        save_period=-1,
        cache=False,
        workers=8,
        verbose=True,
        seed=0,
        deterministic=True,
        single_cls=False,
        rect=False,
        cos_lr=False,
        close_mosaic=10,
        resume=False,
        amp=True,
        fraction=1.0,
        profile=False,
        freeze=None,
        multi_scale=False,
    )
    
    best_weights = BASE_DIR / "runs" / "detect" / "road_damage" / "weights" / "best.pt"
    
    if best_weights.exists():
        target_path = BASE_DIR / "weights" / "best.pt"
        shutil.copy(best_weights, target_path)
        print(f"\nBest weights copied to: {target_path}")
    
    return results

def validate_model(model_path: str, data_yaml: str):
    model = YOLO(model_path)
    results = model.val(data=data_yaml)
    
    print("\nValidation Results:")
    print(f"  mAP50: {results.box.map50:.4f}")
    print(f"  mAP50-95: {results.box.map:.4f}")
    print(f"  Precision: {results.box.mp:.4f}")
    print(f"  Recall: {results.box.mr:.4f}")
    
    return results

if __name__ == "__main__":
    import argparse
    
    parser = argparse.ArgumentParser(description="Train YOLO model for road damage detection")
    parser.add_argument("--mode", type=str, default="train", choices=["train", "validate"], help="Mode: train or validate")
    parser.add_argument("--data", type=str, required=True, help="Path to dataset YAML file")
    parser.add_argument("--model", type=str, default="yolov8n.pt", help="Model name or path")
    parser.add_argument("--epochs", type=int, default=100, help="Number of epochs")
    parser.add_argument("--batch", type=int, default=16, help="Batch size")
    parser.add_argument("--imgsz", type=int, default=640, help="Image size")
    parser.add_argument("--device", type=str, default="0", help="Device (cuda device, i.e. 0 or 0,1,2,3 or cpu)")
    
    args = parser.parse_args()
    
    if args.mode == "train":
        train_model(
            data_yaml=args.data,
            model_name=args.model,
            epochs=args.epochs,
            batch=args.batch,
            imgsz=args.imgsz,
            device=args.device
        )
    elif args.mode == "validate":
        validate_model(
            model_path=args.model,
            data_yaml=args.data
        )
