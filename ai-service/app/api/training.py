from __future__ import annotations

import os
from fastapi import APIRouter, HTTPException, BackgroundTasks
from typing import Any

from app.services.trainer import trainer
from app.models.schemas import TrainRequest, TrainResponse, ValidateRequest, ValidateResponse

router = APIRouter(prefix="/api/v1/train", tags=["Training"])


@router.post("/create-dataset-yaml")
async def create_dataset_yaml(
    train_images: str,
    val_images: str,
    class_names: list
) -> dict[str, Any]:
    if not os.path.exists(train_images):
        raise HTTPException(status_code=404, detail="Train images directory not found")
    if not os.path.exists(val_images):
        raise HTTPException(status_code=404, detail="Validation images directory not found")
    
    yaml_path = trainer.create_dataset_yaml(
        train_images=train_images,
        val_images=val_images,
        class_names=class_names
    )
    
    return {
        "success": True,
        "yaml_path": yaml_path
    }


@router.post("/start", response_model=TrainResponse)
async def start_training(request: TrainRequest, background_tasks: BackgroundTasks):
    if not os.path.exists(request.data_yaml):
        raise HTTPException(status_code=404, detail="Dataset YAML file not found")
    
    result = trainer.train(
        data_yaml=request.data_yaml,
        model_name=request.model_name,
        epochs=request.epochs,
        imgsz=request.imgsz,
        batch=request.batch,
        device=request.device
    )
    
    return result


@router.post("/validate", response_model=ValidateResponse)
async def validate_model(request: ValidateRequest):
    if not os.path.exists(request.model_path):
        raise HTTPException(status_code=404, detail="Model file not found")
    if not os.path.exists(request.data_yaml):
        raise HTTPException(status_code=404, detail="Dataset YAML file not found")
    
    result = trainer.validate(
        model_path=request.model_path,
        data_yaml=request.data_yaml
    )
    
    return result


@router.post("/export")
async def export_model(
    model_path: str,
    format: str = "onnx"
) -> dict[str, Any]:
    if not os.path.exists(model_path):
        raise HTTPException(status_code=404, detail="Model file not found")
    
    result = trainer.export_model(
        model_path=model_path,
        format=format
    )
    
    return result
