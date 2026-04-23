@echo off
chcp 65001 >nul
echo ========================================
echo 道路路面病害智能检测系统 - 依赖安装
echo ========================================
echo.

echo [1/4] 检查Java环境...
java -version
if %ERRORLEVEL% NEQ 0 (
    echo 错误: 未检测到Java环境，请先安装JDK 21
    pause
    exit /b 1
)
echo.

echo [2/4] 安装AI服务依赖...
cd ai-service
pip install -r requirements.txt -i https://pypi.tuna.tsinghua.edu.cn/simple
if %ERRORLEVEL% NEQ 0 (
    echo 警告: AI服务依赖安装可能存在问题
)
cd ..
echo.

echo [3/4] 安装后端依赖...
cd backend
call mvnw.cmd dependency:resolve -q
if %ERRORLEVEL% NEQ 0 (
    echo 正在下载后端依赖...
    call mvnw.cmd clean install -DskipTests
)
cd ..
echo.

echo [4/4] 安装前端依赖...
cd frontend
call npm install
if %ERRORLEVEL% NEQ 0 (
    echo 警告: 前端依赖安装可能存在问题
)
cd ..
echo.

echo ========================================
echo 依赖安装完成！
echo.
echo 环境信息:
echo   - Java: 21
echo   - Python: 3.14
echo   - MySQL: 8.0.45
echo.
echo 下一步: 运行 start.bat 启动系统
echo ========================================
pause
