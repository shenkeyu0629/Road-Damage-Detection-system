from __future__ import annotations

import os
import shutil
import uuid
from datetime import datetime
from fastapi import APIRouter, UploadFile, File, Form, HTTPException, BackgroundTasks
from fastapi.responses import FileResponse

from app.services.detector import detector
from app.models.schemas import (
    DetectionResult, VideoDetectionResult, HealthResponse,
    DetectionRequest, VideoDetectionRequest
)
from app.core.config import UPLOAD_DIR, RESULT_DIR, DAMAGE_CLASSES, DAMAGE_LEVELS

router = APIRouter(prefix="/api/v1/detection", tags=["Detection"])


@router.get("/health", response_model=HealthResponse)
async def health_check():
    return HealthResponse(
        status="healthy",
        model_loaded=detector.model is not None,
        timestamp=datetime.now().isoformat()
    )


@router.get("/classes")
async def get_damage_classes():
    return {
        "classes": DAMAGE_CLASSES,
        "levels": DAMAGE_LEVELS
    }


@router.post("/upload", response_model=dict)
async def upload_image(file: UploadFile = File(...)):
    if file.content_type and not file.content_type.startswith("image/"):
        filename = file.filename or "unknown"
        if not any(filename.lower().endswith(ext) for ext in ['.jpg', '.jpeg', '.png', '.gif', '.bmp']):
            raise HTTPException(status_code=400, detail="File must be an image")
    
    file_ext = os.path.splitext(file.filename or "image.jpg")[1] or ".jpg"
    filename = f"{datetime.now().strftime('%Y%m%d_%H%M%S')}_{uuid.uuid4().hex[:8]}{file_ext}"
    file_path = UPLOAD_DIR / filename
    
    with open(file_path, "wb") as buffer:
        shutil.copyfileobj(file.file, buffer)
    
    return {
        "success": True,
        "filename": filename,
        "file_path": str(file_path),
        "original_name": file.filename
    }


@router.post("/upload-video", response_model=dict)
async def upload_video(file: UploadFile = File(...)):
    allowed_types = ["video/mp4", "video/avi", "video/mov", "video/quicktime", None]
    if file.content_type and file.content_type not in allowed_types:
        filename = file.filename or "unknown"
        if not any(filename.lower().endswith(ext) for ext in ['.mp4', '.avi', '.mov']):
            raise HTTPException(status_code=400, detail="File must be a video (mp4, avi, mov)")
    
    file_ext = os.path.splitext(file.filename or "video.mp4")[1] or ".mp4"
    filename = f"{datetime.now().strftime('%Y%m%d_%H%M%S')}_{uuid.uuid4().hex[:8]}{file_ext}"
    file_path = UPLOAD_DIR / filename
    
    with open(file_path, "wb") as buffer:
        shutil.copyfileobj(file.file, buffer)
    
    return {
        "success": True,
        "filename": filename,
        "file_path": str(file_path),
        "original_name": file.filename
    }


@router.post("/detect", response_model=DetectionResult)
async def detect_damage(request: DetectionRequest):
    if not os.path.exists(request.image_path):
        raise HTTPException(status_code=404, detail="Image file not found")
    
    result = detector.detect(request.image_path, request.conf_threshold)
    
    if request.draw_result:
        output_path = detector.draw_detections(
            request.image_path, 
            result["detections"]
        )
        result["result_image_path"] = output_path
    
    return result


@router.post("/detect-upload", response_model=DetectionResult)
async def detect_uploaded_image(
    file: UploadFile = File(...),
    conf_threshold: float = Form(0.25)
):
    if file.content_type and not file.content_type.startswith("image/"):
        filename = file.filename or "unknown"
        if not any(filename.lower().endswith(ext) for ext in ['.jpg', '.jpeg', '.png', '.gif', '.bmp']):
            raise HTTPException(status_code=400, detail="File must be an image")
    
    file_ext = os.path.splitext(file.filename or "image.jpg")[1] or ".jpg"
    filename = f"{datetime.now().strftime('%Y%m%d_%H%M%S')}_{uuid.uuid4().hex[:8]}{file_ext}"
    file_path = UPLOAD_DIR / filename
    
    with open(file_path, "wb") as buffer:
        shutil.copyfileobj(file.file, buffer)
    
    result = detector.detect(str(file_path), conf_threshold)
    
    output_path = detector.draw_detections(str(file_path), result["detections"])
    result["result_image_path"] = output_path
    result["uploaded_file_path"] = str(file_path)
    
    return result


@router.post("/detect-video", response_model=VideoDetectionResult)
async def detect_video_damage(request: VideoDetectionRequest):
    if not os.path.exists(request.video_path):
        raise HTTPException(status_code=404, detail="Video file not found")
    
    result = detector.detect_video(
        request.video_path,
        conf_threshold=request.conf_threshold,
        frame_interval=request.frame_interval
    )
    
    return result


@router.get("/result/{filename}")
async def get_result_image(filename: str):
    file_path = RESULT_DIR / filename
    if not file_path.exists():
        raise HTTPException(status_code=404, detail="Result file not found")
    
    if filename.endswith('.mp4'):
        return FileResponse(
            file_path,
            media_type="video/mp4",
            filename=filename
        )
    elif filename.endswith('.webm'):
        return FileResponse(
            file_path,
            media_type="video/webm",
            filename=filename
        )
    else:
        return FileResponse(
            file_path,
            media_type="image/jpeg",
            filename=filename
        )


@router.get("/upload/{filename}")
async def get_uploaded_image(filename: str):
    file_path = UPLOAD_DIR / filename
    if not file_path.exists():
        raise HTTPException(status_code=404, detail="File not found")
    
    return FileResponse(
        file_path,
        media_type="image/jpeg",
        filename=filename
    )
