import os
from pathlib import Path

BASE_DIR = Path(__file__).resolve().parent.parent.parent

MODEL_PATH = os.getenv("MODEL_PATH", str(BASE_DIR / "weights" / "best.pt"))
DATASET_PATH = os.getenv("DATASET_PATH", str(BASE_DIR / "datasets"))

UPLOAD_DIR = BASE_DIR / "uploads"
RESULT_DIR = BASE_DIR / "results"
UPLOAD_DIR.mkdir(parents=True, exist_ok=True)
RESULT_DIR.mkdir(parents=True, exist_ok=True)

DAMAGE_CLASSES = {
    0: {"name": "裂缝", "name_en": "Crack", "level": "minor"},
    1: {"name": "坑洞", "name_en": "Pothole", "level": "severe"},
}

DAMAGE_LEVELS = {
    "minor": {"name": "轻微", "score": 1, "color": "#4CAF50", "description": "不影响行车安全，建议观察"},
    "severe": {"name": "严重", "score": 2, "color": "#F44336", "description": "影响行车安全，需要立即维修"},
    "none": {"name": "无", "score": 0, "color": "#9E9E9E", "description": "已修补区域"},
}

ALARM_THRESHOLDS = {
    "severe_count": 1,
    "total_damage_area_ratio": 0.1,
}

DATABASE_URL = os.getenv(
    "DATABASE_URL",
    "mysql+pymysql://root:qazwsxasd@localhost:3306/road_inspection?charset=utf8mb4"
)

API_HOST = os.getenv("API_HOST", "0.0.0.0")
API_PORT = int(os.getenv("API_PORT", 8001))
