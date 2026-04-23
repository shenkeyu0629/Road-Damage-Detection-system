@echo off
chcp 65001 >nul
echo ========================================
echo 道路路面病害智能检测系统启动脚本
echo ========================================
echo.

echo [1/3] 启动AI检测服务...
cd ai-service
start "AI Service" cmd /k "python main.py"
cd ..
timeout /t 8 /nobreak >nul

echo [2/3] 启动Spring Boot后端服务...
cd backend
start "Backend Service" cmd /k "mvnw.cmd spring-boot:run"
cd ..
timeout /t 20 /nobreak >nul

echo [3/3] 启动Vue前端服务...
cd frontend
start "Frontend Service" cmd /k "npm run dev"
cd ..

echo.
echo ========================================
echo 所有服务启动完成！
echo.
echo AI服务地址: http://localhost:8001
echo 后端服务地址: http://localhost:8080/api
echo 前端服务地址: http://localhost:3000
echo API文档地址: http://localhost:8080/api/doc.html
echo.
echo 默认登录账号: admin / admin123
echo ========================================
pause
