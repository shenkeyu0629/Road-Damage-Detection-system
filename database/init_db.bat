@echo off
chcp 65001 >nul
echo ========================================
echo 初始化道路病害检测系统数据库
echo ========================================
echo.

set MYSQL_PATH=mysql
set DB_HOST=localhost
set DB_PORT=3306
set DB_USER=root
set DB_PASS=qazwsxasd
set DB_NAME=road_inspection

echo 正在创建数据库...
%MYSQL_PATH% -h%DB_HOST% -P%DB_PORT% -u%DB_USER% -p%DB_PASS% -e "CREATE DATABASE IF NOT EXISTS %DB_NAME% DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;"

if %ERRORLEVEL% NEQ 0 (
    echo 数据库创建失败，请检查MySQL连接配置
    pause
    exit /b 1
)

echo 数据库创建成功！
echo.

echo 正在执行初始化脚本...
%MYSQL_PATH% -h%DB_HOST% -P%DB_PORT% -u%DB_USER% -p%DB_PASS% %DB_NAME% < init.sql

if %ERRORLEVEL% NEQ 0 (
    echo 初始化脚本执行失败
    pause
    exit /b 1
)

echo.
echo ========================================
echo 数据库初始化完成！
echo 数据库名: %DB_NAME%
echo ========================================
pause
