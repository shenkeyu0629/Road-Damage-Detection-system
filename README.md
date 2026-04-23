# Road Pavement Disease Recognition System
## Project Overview
This project is a complete intelligent detection system for road pavement diseases, integrating AI image recognition, backend services, and a frontend interactive interface. It enables automated identification and analysis of pavement diseases such as cracks, potholes, and subsidence. Built on Java, Python, and frontend technology stacks, the system features core capabilities including disease detection, result visualization, and data management, making it suitable for the general survey and evaluation of pavement diseases in scenarios like road maintenance and municipal engineering.

## Features
- **Multi-type disease recognition**: Supports intelligent detection of common road pavement diseases such as cracks, potholes, subsidence, and bumps.
- **Multi-terminal collaboration**: Integrates AI inference services, Java backend services, and a frontend visualization interface to implement an end-to-end detection process.
- **Environment adaptation**: Adapted to the Windows system, providing one-click installation and startup scripts to lower the deployment threshold.
- **Efficient inference**: Optimized based on lightweight AI models, balancing detection accuracy and inference speed to meet the requirements of engineering applications.

## Environment Requirements
- **Operating System**: Windows 10/11 (64-bit)
- **Java Environment**: JDK 21 (mandatory, required for backend service operation)
- **Python Version**: 3.14 (required for AI service operation)
- **MySQL Version**: 8.0.45 (required for data storage, need to be installed and configured in advance)
- **Others**: Uninterrupted network connection (to download resources during dependency installation); Administrator privileges (optional, to avoid installation failures due to insufficient permissions)

## Installation Steps
### 1. Preparations
- Confirm that JDK 21 is installed and environment variables are configured (verify by executing `java -version` in the command line).
- Confirm that Python 3.14 is installed and environment variables are configured (verify by executing `python --version`).
- Confirm that MySQL 8.0.45 is installed, create a database, and record the account and password (to be used in subsequent backend configuration).

### 2. One-click Dependency Installation
1. Download the complete project code to the local machine and enter the project root directory (the directory containing the `install.bat` file).
2. Double-click to run the `install.bat` script, or execute the following command in the command line:
```bash
install.bat
```
3. Wait for the script to automatically complete the following operations:
   - Check if the Java environment meets the requirements.
   - Install Python dependencies for the AI service (ai-service directory) (accelerated using Tsinghua PyPI mirror).
   - Install Maven dependencies for the backend service (backend directory).
   - Install npm dependencies for the frontend service (frontend directory).

### Installation Notes
- If the prompt "Java environment not detected" appears during script execution, install JDK 21 and configure environment variables before re-running the script.
- If a "warning" appears during AI/frontend dependency installation, manually enter the corresponding directory and re-execute the installation command:
  - Manual installation of AI dependencies: `cd ai-service && pip install -r requirements.txt -i https://pypi.tuna.tsinghua.edu.cn/simple`
  - Manual installation of frontend dependencies: `cd frontend && npm install`
- If the initial download of backend dependencies is slow, the script will automatically trigger the complete installation process; please wait patiently.

## Quick Start
### 1. Pre-start Configuration
- Enter the `backend` directory, modify the database configuration file (e.g., application.yml), and fill in the MySQL address, account, password, and database name.
- (Optional) Enter the `ai-service` directory, modify the model configuration file, and adjust parameters such as disease detection thresholds and model paths.

### 2. Start the System
Double-click to run the `start.bat` script in the project root directory (ensure `install.bat` has been executed first). The script will start the following services in sequence:
- AI inference service
- Java backend service
- Frontend Web service

### 3. Use the System
1. After startup, open a browser and access the frontend address (default is usually `http://localhost:8080`, subject to frontend configuration).
2. Upload road pavement images (supports single/batch upload).
3. Click the "Detect" button; the system will automatically identify the type, location, and severity of pavement diseases in the images.
4. View the detection result report, which supports export, printing, and other operations.

## Directory Structure
```
Road-Damage-Detection-system/
├── ai-service/            # AI inference service directory (Python)
│   ├── requirements.txt   # AI service dependency list
│   └── [Model files/Inference code]
├── backend/               # Backend service directory (Java)
│   ├── mvnw.cmd           # Maven wrapper script
│   ├── pom.xml            # Maven dependency configuration
│   └── [Source code/Configuration files]
├── frontend/              # Frontend interface directory (Vue/React, etc.)
│   ├── package.json       # npm dependency configuration
│   └── [Source code/Static resources]
├── install.bat            # One-click dependency installation script
├── start.bat              # One-click system startup script
└── README.md              # Project documentation
