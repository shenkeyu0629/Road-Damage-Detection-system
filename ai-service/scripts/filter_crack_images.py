import os
import cv2
import random
from pathlib import Path

BASE_DIR = Path(__file__).resolve().parent.parent
IMAGES_DIR = BASE_DIR / "datasets" / "train" / "images"
LABELS_DIR = BASE_DIR / "datasets" / "train" / "labels"
OUTPUT_DIR = BASE_DIR / "training_data"
OUTPUT_DIR.mkdir(parents=True, exist_ok=True)

CRACK_CLASSES = {0, 1, 2}
OTHER_CLASSES = {3, 4}

def find_crack_only_images():
    crack_only_images = []
    
    for label_file in LABELS_DIR.glob("*.txt"):
        with open(label_file, 'r') as f:
            lines = f.readlines()
        
        if not lines:
            continue
        
        classes_in_file = set()
        for line in lines:
            parts = line.strip().split()
            if parts:
                class_id = int(parts[0])
                classes_in_file.add(class_id)
        
        if classes_in_file.issubset(CRACK_CLASSES) and len(classes_in_file) > 0:
            image_name = label_file.stem + ".jpg"
            image_path = IMAGES_DIR / image_name
            if image_path.exists():
                crack_only_images.append(image_path)
    
    return crack_only_images

def create_video_from_images(image_paths, output_path, duration_seconds=2, fps=10):
    if not image_paths:
        print("No images to create video")
        return None
    
    random.shuffle(image_paths)
    selected_images = image_paths[:min(20, len(image_paths))]
    
    print(f"Selected {len(selected_images)} images for video")
    
    first_image = cv2.imread(str(selected_images[0]))
    height, width = first_image.shape[:2]
    
    total_frames = duration_seconds * fps
    frames_per_image = max(1, total_frames // len(selected_images))
    
    fourcc = cv2.VideoWriter_fourcc(*'mp4v')
    video_path = output_path / "crack_training_video.mp4"
    out = cv2.VideoWriter(str(video_path), fourcc, fps, (width, height))
    
    for img_path in selected_images:
        img = cv2.imread(str(img_path))
        if img is not None:
            img_resized = cv2.resize(img, (width, height))
            for _ in range(frames_per_image):
                out.write(img_resized)
    
    remaining_frames = total_frames - (len(selected_images) * frames_per_image)
    for i in range(remaining_frames):
        img = cv2.imread(str(selected_images[i % len(selected_images)]))
        if img is not None:
            img_resized = cv2.resize(img, (width, height))
            out.write(img_resized)
    
    out.release()
    print(f"Video created: {video_path}")
    print(f"Video duration: {duration_seconds}s, FPS: {fps}, Total frames: {total_frames}")
    
    return video_path

def copy_selected_images_with_labels(image_paths, output_dir):
    import shutil
    
    images_output = output_dir / "images"
    labels_output = output_dir / "labels"
    images_output.mkdir(parents=True, exist_ok=True)
    labels_output.mkdir(parents=True, exist_ok=True)
    
    random.shuffle(image_paths)
    selected = image_paths[:min(20, len(image_paths))]
    
    for img_path in selected:
        shutil.copy(img_path, images_output / img_path.name)
        
        label_path = LABELS_DIR / (img_path.stem + ".txt")
        if label_path.exists():
            shutil.copy(label_path, labels_output / label_path.name)
    
    print(f"Copied {len(selected)} images and labels to {output_dir}")
    return selected

if __name__ == "__main__":
    print("Finding images containing only Crack...")
    crack_images = find_crack_only_images()
    print(f"Found {len(crack_images)} images with only Crack")
    
    if crack_images:
        print("\nCreating video from selected images...")
        video_path = create_video_from_images(crack_images, OUTPUT_DIR, duration_seconds=2, fps=10)
        
        print("\nCopying selected images with labels...")
        selected = copy_selected_images_with_labels(crack_images, OUTPUT_DIR)
        
        print("\nSelected images:")
        for img in selected:
            print(f"  - {img.name}")
