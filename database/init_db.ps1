$ErrorActionPreference = "Stop"

Write-Host "========================================"
Write-Host "初始化道路病害检测系统数据库"
Write-Host "========================================"
Write-Host ""

$MySqlUser = "root"
$MySqlPass = "qazwsxasd"
$DbName = "road_inspection"
$ScriptPath = Split-Path -Parent $MyInvocation.MyCommand.Path
$InitSql = Join-Path $ScriptPath "init.sql"

Write-Host "正在创建数据库 $DbName ..."

$createDbCmd = "CREATE DATABASE IF NOT EXISTS $DbName DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;"
mysql -u $MySqlUser -p$MySqlPass -e $createDbCmd

if ($LASTEXITCODE -ne 0) {
    Write-Host "数据库创建失败，请检查MySQL连接配置" -ForegroundColor Red
    Read-Host "按回车键退出"
    exit 1
}

Write-Host "数据库创建成功！" -ForegroundColor Green
Write-Host ""

Write-Host "正在执行初始化脚本..."
Get-Content $InitSql -Encoding UTF8 | mysql -u $MySqlUser -p$MySqlPass $DbName

if ($LASTEXITCODE -ne 0) {
    Write-Host "初始化脚本执行失败" -ForegroundColor Red
    Read-Host "按回车键退出"
    exit 1
}

Write-Host ""
Write-Host "========================================"
Write-Host "数据库初始化完成！" -ForegroundColor Green
Write-Host "数据库名: $DbName"
Write-Host "========================================"
Read-Host "按回车键退出"
