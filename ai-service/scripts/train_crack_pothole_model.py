import os
import shutil
import random
from pathlib import Path
from ultralytics import YOLO

BASE_DIR = Path(__file__).resolve().parent.parent
DATASETS_DIR = BASE_DIR / "datasets"
TRAIN_DATASET = DATASETS_DIR / "train"
POTHOLE_DATASET = DATASETS_DIR / "pothole_dataset"
OUTPUT_DATASET = DATASETS_DIR / "crack_pothole_dataset"

CRACK_CLASSES = {0, 1, 2}
POTHOLE_CLASS = {4}

def create_merged_dataset():
    print("Creating merged dataset for Crack and Pothole detection...")
    
    for split in ['train', 'val']:
        (OUTPUT_DATASET / 'images' / split).mkdir(parents=True, exist_ok=True)
        (OUTPUT_DATASET / 'labels' / split).mkdir(parents=True, exist_ok=True)
    
    crack_images = []
    pothole_images = []
    
    print("\nProcessing train dataset...")
    labels_dir = TRAIN_DATASET / "labels"
    images_dir = TRAIN_DATASET / "images"
    
    for label_file in labels_dir.glob("*.txt"):
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
        
        image_name = label_file.stem + ".jpg"
        image_path = images_dir / image_name
        
        if not image_path.exists():
            continue
        
        if classes_in_file.issubset(CRACK_CLASSES) and len(classes_in_file) > 0:
            crack_images.append((image_path, label_file, 'crack'))
        elif classes_in_file == POTHOLE_CLASS:
            pothole_images.append((image_path, label_file, 'pothole'))
    
    print(f"Found {len(crack_images)} crack images")
    print(f"Found {len(pothole_images)} pothole images")
    
    print("\nProcessing pothole dataset...")
    for split in ['train', 'val']:
        pothole_images_dir = POTHOLE_DATASET / 'images' / split
        pothole_labels_dir = POTHOLE_DATASET / 'labels' / split
        
        if not pothole_images_dir.exists():
            continue
        
        for image_file in pothole_images_dir.glob("*.jpg"):
            label_file = pothole_labels_dir / (image_file.stem + ".txt")
            if label_file.exists():
                with open(label_file, 'r') as f:
                    lines = f.readlines()
                
                has_pothole = False
                has_crack = False
                new_lines = []
                
                for line in lines:
                    parts = line.strip().split()
                    if parts:
                        class_id = int(parts[0])
                        if class_id == 0:
                            parts[0] = '1'
                            new_lines.append(' '.join(parts) + '\n')
                            has_pothole = True
                        elif class_id == 1:
                            parts[0] = '0'
                            new_lines.append(' '.join(parts) + '\n')
                            has_crack = True
                
                if has_pothole:
                    pothole_images.append((image_file, label_file, 'pothole', new_lines))
                if has_crack:
                    crack_images.append((image_file, label_file, 'crack', new_lines))
    
    print(f"\nTotal crack images: {len(crack_images)}")
    print(f"Total pothole images: {len(pothole_images)}")
    
    random.shuffle(crack_images)
    random.shuffle(pothole_images)
    
    train_ratio = 0.8
    crack_train_count = int(len(crack_images) * train_ratio)
    pothole_train_count = int(len(pothole_images) * train_ratio)
    
    train_images = crack_images[:crack_train_count] + pothole_images[:pothole_train_count]
    val_images = crack_images[crack_train_count:] + pothole_images[pothole_train_count:]
    
    random.shuffle(train_images)
    random.shuffle(val_images)
    
    print(f"\nTrain set: {len(train_images)} images")
    print(f"Val set: {len(val_images)} images")
    
    print("\nCopying train images...")
    for item in train_images:
        if len(item) == 4:
            image_path, label_path, damage_type, new_lines = item
        else:
            image_path, label_path, damage_type = item
            with open(label_path, 'r') as f:
                lines = f.readlines()
            new_lines = []
            for line in lines:
                parts = line.strip().split()
                if parts:
                    class_id = int(parts[0])
                    if class_id in CRACK_CLASSES:
                        parts[0] = '0'
                        new_lines.append(' '.join(parts) + '\n')
                    elif class_id in POTHOLE_CLASS:
                        parts[0] = '1'
                        new_lines.append(' '.join(parts) + '\n')
        
        dest_image = OUTPUT_DATASET / 'images' / 'train' / image_path.name
        dest_label = OUTPUT_DATASET / 'labels' / 'train' / (image_path.stem + '.txt')
        
        shutil.copy(image_path, dest_image)
        with open(dest_label, 'w') as f:
            f.writelines(new_lines)
    
    print("Copying val images...")
    for item in val_images:
        if len(item) == 4:
            image_path, label_path, damage_type, new_lines = item
        else:
            image_path, label_path, damage_type = item
            with open(label_path, 'r') as f:
                lines = f.readlines()
            new_lines = []
            for line in lines:
                parts = line.strip().split()
                if parts:
                    class_id = int(parts[0])
                    if class_id in CRACK_CLASSES:
                        parts[0] = '0'
                        new_lines.append(' '.join(parts) + '\n')
                    elif class_id in POTHOLE_CLASS:
                        parts[0] = '1'
                        new_lines.append(' '.join(parts) + '\n')
        
        dest_image = OUTPUT_DATASET / 'images' / 'val' / image_path.name
        dest_label = OUTPUT_DATASET / 'labels' / 'val' / (image_path.stem + '.txt')
        
        shutil.copy(image_path, dest_image)
        with open(dest_label, 'w') as f:
            f.writelines(new_lines)
    
    data_yaml_content = f"""path: {OUTPUT_DATASET.as_posix()}
train: images/train
val: images/val

nc: 2
names: ['Crack', 'Pothole']
"""
    
    data_yaml_path = OUTPUT_DATASET / "data.yaml"
    with open(data_yaml_path, 'w', encoding='utf-8') as f:
        f.write(data_yaml_content)
    
    print(f"\nDataset created at: {OUTPUT_DATASET}")
    print(f"Data YAML: {data_yaml_path}")
    
    return str(data_yaml_path)

def train_model(data_yaml, epochs=50, batch=16, device='0'):
    print(f"\nStarting training with {epochs} epochs...")
    
    pretrained_model = BASE_DIR / "weights" / "best.pt"
    if pretrained_model.exists():
        print(f"Using local pretrained model: {pretrained_model}")
        model = YOLO(str(pretrained_model))
    else:
        print("Using yolov8n.pt from cache or downloading...")
        model = YOLO("yolov8n.pt")
    
    results = model.train(
        data=data_yaml,
        epochs=epochs,
        imgsz=640,
        batch=batch,
        device=device,
        project=str(BASE_DIR / "runs" / "detect"),
        name="crack_pothole_model",
        exist_ok=True,
        pretrained=True,
        optimizer="auto",
        lr0=0.01,
        patience=20,
        save=True,
        verbose=True,
    )
    
    best_weights = BASE_DIR / "runs" / "detect" / "crack_pothole_model" / "weights" / "best.pt"
    
    if best_weights.exists():
        target_path = BASE_DIR / "weights" / "best.pt"
        shutil.copy(best_weights, target_path)
        print(f"\nBest weights copied to: {target_path}")
    
    return results

if __name__ == "__main__":
    import argparse
    
    parser = argparse.ArgumentParser(description="Train Crack and Pothole detection model")
    parser.add_argument("--skip-dataset", action="store_true", help="Skip dataset creation")
    parser.add_argument("--epochs", type=int, default=50, help="Number of epochs")
    parser.add_argument("--batch", type=int, default=16, help="Batch size")
    parser.add_argument("--device", type=str, default="0", help="Device (0 or cpu)")
    
    args = parser.parse_args()
    
    if not args.skip_dataset:
        data_yaml = create_merged_dataset()
    else:
        data_yaml = str(OUTPUT_DATASET / "data.yaml")
    
    train_model(
        data_yaml=data_yaml,
        epochs=args.epochs,
        batch=args.batch,
        device=args.device
    )
