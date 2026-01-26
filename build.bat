@echo off
setlocal enabledelayedexpansion

echo Building OG-Essentials Plugin...
echo ================================
echo.

REM Check if Maven is installed
where mvn >nul 2>&1
if %errorlevel% neq 0 (
    echo Error: Maven is not installed or not in PATH
    echo Please install Maven 3.6+ to build this plugin
    exit /b 1
)

REM Check Maven version
echo Checking Maven version...
for /f "tokens=3" %%i in ('mvn -version ^| findstr /i "Apache Maven"') do set MAVEN_VERSION=%%i
echo Maven version: %MAVEN_VERSION%
echo.

REM Extract version from pom.xml
set "PLUGIN_VERSION="
for /f "tokens=2 delims=<>" %%i in ('findstr /i "<version>" pom.xml') do (
    if "!PLUGIN_VERSION!"=="" (
        set "PLUGIN_VERSION=%%i"
    )
)
if "!PLUGIN_VERSION!"=="" (
    echo Error: Could not determine plugin version from pom.xml
    exit /b 1
)
echo Detected plugin version: !PLUGIN_VERSION!
echo.

REM Clean previous builds
echo Cleaning previous builds...
call mvn clean
if %errorlevel% neq 0 (
    echo.
    echo Error: Failed to clean previous builds
    exit /b 1
)

REM Build plugin
echo.
echo Building plugin...
call mvn package

set "JAR_FILE=target\OGEssentials-!PLUGIN_VERSION!.jar"

if %errorlevel% equ 0 (
    echo.
    echo Build successful!
    echo Plugin JAR file created in: !JAR_FILE!
    echo.
    echo To install:
    echo 1. Copy the JAR file to your server's plugins folder
    echo 2. Restart your server
    echo 3. The plugin will load all commands, AFK, homes, TPA, 3x3 pickaxe, custom enchants, and more
) else (
    echo.
    echo Build failed!
    echo Please check the error messages above
    exit /b 1
)
PAUSE