from __future__ import annotations

import os
import yaml
from pathlib import Path
from typing import Any
from datetime import datetime

from ultralytics import YOLO
from app.core.config import DATASET_PATH, BASE_DIR


class ModelTrainer:
    def __init__(self):
        self.runs_dir = BASE_DIR / "runs"
        self.runs_dir.mkdir(parents=True, exist_ok=True)
    
    def create_dataset_yaml(
        self,
        train_images: str,
        val_images: str,
        class_names: list,
        output_path: str | None = None
    ) -> str:
        dataset_config = {
            "path": str(Path(train_images).parent),
            "train": train_images,
            "val": val_images,
            "names": {i: name for i, name in enumerate(class_names)},
        }
        
        if output_path is None:
            output_path = str(BASE_DIR / "datasets" / "dataset.yaml")
        
        os.makedirs(os.path.dirname(output_path), exist_ok=True)
        
        with open(output_path, 'w', encoding='utf-8') as f:
            yaml.dump(dataset_config, f, allow_unicode=True, default_flow_style=False)
        
        return output_path
    
    def train(
        self,
        data_yaml: str,
        model_name: str = "yolov8n.pt",
        epochs: int = 100,
        imgsz: int = 640,
        batch: int = 16,
        device: str = "0",
        project: str | None = None,
        name: str | None = None,
        **kwargs
    ) -> dict[str, Any]:
        model = YOLO(model_name)
        
        if project is None:
            project = str(self.runs_dir / "detect")
        if name is None:
            name = f"train_{datetime.now().strftime('%Y%m%d_%H%M%S')}"
        
        results = model.train(
            data=data_yaml,
            epochs=epochs,
            imgsz=imgsz,
            batch=batch,
            device=device,
            project=project,
            name=name,
            **kwargs
        )
        
        best_weights = Path(project) / name / "weights" / "best.pt"
        
        return {
            "success": True,
            "project": project,
            "name": name,
            "best_weights": str(best_weights) if best_weights.exists() else None,
            "results": str(results),
        }
    
    def validate(
        self,
        model_path: str,
        data_yaml: str,
        **kwargs
    ) -> dict[str, Any]:
        model = YOLO(model_path)
        results = model.val(data=data_yaml, **kwargs)
        
        return {
            "success": True,
            "model_path": model_path,
            "metrics": {
                "map50": float(results.box.map50) if hasattr(results, 'box') else None,
                "map50_95": float(results.box.map) if hasattr(results, 'box') else None,
                "precision": float(results.box.mp) if hasattr(results, 'box') else None,
                "recall": float(results.box.mr) if hasattr(results, 'box') else None,
            }
        }
    
    def export_model(
        self,
        model_path: str,
        format: str = "onnx",
        output_dir: str | None = None
    ) -> dict[str, Any]:
        model = YOLO(model_path)
        
        export_path = model.export(format=format)
        
        return {
            "success": True,
            "original_path": model_path,
            "export_path": str(export_path),
            "format": format,
        }


trainer = ModelTrainer()
