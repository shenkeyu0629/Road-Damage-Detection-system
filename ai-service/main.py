from fastapi import FastAPI, Request
from fastapi.middleware.cors import CORSMiddleware
from fastapi.staticfiles import StaticFiles
from fastapi.responses import JSONResponse
from contextlib import asynccontextmanager
from pathlib import Path
import json

from app.api.detection import router as detection_router
from app.api.training import router as training_router
from app.core.config import API_HOST, API_PORT

TEST_VIDEOS_DIR = Path(__file__).parent.parent / "pothole_video"
TEST_VIDEOS_DIR.mkdir(parents=True, exist_ok=True)


class UnicodeJSONResponse(JSONResponse):
    def render(self, content) -> bytes:
        return json.dumps(
            content,
            ensure_ascii=False,
            allow_nan=False,
            indent=None,
            separators=(",", ":"),
        ).encode("utf-8")


@asynccontextmanager
async def lifespan(app: FastAPI):
    print("Starting Road Damage Detection AI Service...")
    print(f"API will be available at http://{API_HOST}:{API_PORT}")
    print(f"Test videos directory: {TEST_VIDEOS_DIR}")
    yield
    print("Shutting down AI Service...")


app = FastAPI(
    title="Road Damage Detection AI Service",
    description="基于YOLO8的道路路面病害智能检测API服务",
    version="1.0.0",
    lifespan=lifespan,
    default_response_class=UnicodeJSONResponse
)

app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

app.include_router(detection_router)
app.include_router(training_router)

app.mount("/videos", StaticFiles(directory=str(TEST_VIDEOS_DIR)), name="videos")


@app.get("/")
async def root():
    return {
        "service": "Road Damage Detection AI Service",
        "version": "1.0.0",
        "status": "running",
        "docs": "/docs"
    }


if __name__ == "__main__":
    import uvicorn
    uvicorn.run(app, host=API_HOST, port=API_PORT)
