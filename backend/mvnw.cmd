@echo off
@REM Maven Wrapper script for Windows

setlocal EnableDelayedExpansion

set "MAVEN_VERSION=3.9.9"
set "MAVEN_HOME=%USERPROFILE%\.m2\wrapper\dists\apache-maven-3.9.9"
set "MAVEN_CMD=%MAVEN_HOME%\apache-maven-3.9.9\bin\mvn.cmd"

@REM Download and extract Maven if not exists
if not exist "%MAVEN_CMD%" (
    echo Downloading Apache Maven 3.9.9...
    if not exist "%USERPROFILE%\.m2\wrapper\dists" mkdir "%USERPROFILE%\.m2\wrapper\dists"
    
    set "MAVEN_ZIP=%USERPROFILE%\.m2\wrapper\dists\apache-maven-3.9.9-bin.zip"
    
    @REM Try multiple download sources
    echo Trying download from archive.apache.org...
    powershell -NoProfile -ExecutionPolicy Bypass -Command "[Net.ServicePointManager]::SecurityProtocol = [Net.SecurityProtocolType]::Tls12; try { Invoke-WebRequest -Uri 'https://archive.apache.org/dist/maven/maven-3/3.9.9/binaries/apache-maven-3.9.9-bin.zip' -OutFile '!MAVEN_ZIP!' -UseBasicParsing } catch { Write-Host 'Download failed, trying mirror...'; Invoke-WebRequest -Uri 'https://repo.maven.apache.org/maven2/org/apache/maven/apache-maven/3.9.9/apache-maven-3.9.9-bin.zip' -OutFile '!MAVEN_ZIP!' -UseBasicParsing }"
    
    if exist "%USERPROFILE%\.m2\wrapper\dists\apache-maven-3.9.9-bin.zip" (
        echo Extracting Maven...
        powershell -NoProfile -ExecutionPolicy Bypass -Command "Expand-Archive -Path '%USERPROFILE%\.m2\wrapper\dists\apache-maven-3.9.9-bin.zip' -DestinationPath '%MAVEN_HOME%' -Force"
    )
)

@REM Run Maven
if exist "%MAVEN_CMD%" (
    "%MAVEN_CMD%" %*
) else (
    echo.
    echo Failed to download Maven automatically.
    echo Please install Maven manually:
    echo   1. Download from: https://maven.apache.org/download.cgi
    echo   2. Or run: winget install Apache.Maven
    echo   3. Or run: choco install maven
    exit /b 1
)

endlocal
