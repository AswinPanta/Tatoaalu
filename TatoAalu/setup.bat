@echo off
setlocal enabledelayedexpansion

REM Hot Potato Game - Windows Setup and Build Script
REM This script sets up the development environment and builds the Android APK

echo.
echo 🔥🥔 Hot Potato Game - Windows Setup Script 🥔🔥
echo ========================================

REM Check if we're in the right directory
if not exist "gradlew.bat" (
    echo [ERROR] gradlew.bat not found. Please run this script from the TatoAalu project root directory.
    pause
    exit /b 1
)

echo [INFO] Checking Prerequisites...

REM Check for Java
java -version >nul 2>&1
if %errorlevel% neq 0 (
    echo [ERROR] Java not found. Please install Java 17 or higher.
    echo [INFO] Install Java from: https://adoptium.net/
    pause
    exit /b 1
) else (
    for /f "tokens=3" %%g in ('java -version 2^>^&1 ^| findstr /i "version"') do (
        set JAVA_VERSION=%%g
        set JAVA_VERSION=!JAVA_VERSION:"=!
    )
    echo [INFO] Java found: !JAVA_VERSION!
)

REM Check for Android SDK
if "%ANDROID_HOME%"=="" (
    echo [WARNING] ANDROID_HOME not set. Checking for local.properties...
    if not exist "local.properties" (
        echo [ERROR] local.properties not found and ANDROID_HOME not set.
        echo [INFO] Please create local.properties file with your SDK path:
        echo [INFO] 1. Copy local.properties.template to local.properties
        echo [INFO] 2. Edit local.properties and set your Android SDK path
        echo [INFO]    Example: sdk.dir=C:\\Users\\YourName\\AppData\\Local\\Android\\Sdk
        pause
        exit /b 1
    ) else (
        echo [INFO] Found local.properties file
    )
) else (
    echo [INFO] Android SDK found: %ANDROID_HOME%
)

echo.
echo [INFO] 🧹 Cleaning Project...
call gradlew.bat clean
if %errorlevel% neq 0 (
    echo [ERROR] Clean failed. Check error messages above.
    pause
    exit /b 1
)

echo.
echo [INFO] 📦 Downloading Dependencies...
call gradlew.bat build --dry-run
if %errorlevel% neq 0 (
    echo [ERROR] Dependency download failed. Check your internet connection.
    pause
    exit /b 1
)

echo.
echo [INFO] 🎵 Setting up Music Directory...
set MUSIC_DIR=app\src\main\assets\music
if not exist "%MUSIC_DIR%" (
    echo [INFO] Creating music directory...
    mkdir "%MUSIC_DIR%" 2>nul
)

REM Count music files
set MUSIC_COUNT=0
for %%f in ("%MUSIC_DIR%\*.mp3" "%MUSIC_DIR%\*.wav" "%MUSIC_DIR%\*.ogg" "%MUSIC_DIR%\*.m4a") do (
    if exist "%%f" set /a MUSIC_COUNT+=1
)

if %MUSIC_COUNT% equ 0 (
    echo [WARNING] No music files found in %MUSIC_DIR%
    echo [INFO] The game will use timer-based fallback ^(still works great!^)
    echo [INFO] To add music:
    echo [INFO] 1. Add MP3/WAV files to %MUSIC_DIR%
    echo [INFO] 2. Name them: song1.mp3, song2.mp3, etc.
    echo [INFO] 3. Rebuild the project
) else (
    echo [INFO] Found %MUSIC_COUNT% music files
)

echo.
echo [INFO] 🔨 Building Project...

REM Build debug APK
echo [INFO] Building debug APK...
call gradlew.bat assembleDebug
if %errorlevel% neq 0 (
    echo [ERROR] ❌ Build failed. Check the error messages above.
    pause
    exit /b 1
)

REM Check if build was successful
set APK_PATH=app\build\outputs\apk\debug\app-debug.apk
if exist "%APK_PATH%" (
    echo [INFO] ✅ Build successful!
    echo [INFO] Debug APK location: %APK_PATH%

    REM Get APK size
    for %%A in ("%APK_PATH%") do set APK_SIZE=%%~zA
    set /a APK_SIZE_MB=!APK_SIZE!/1024/1024
    echo [INFO] APK size: !APK_SIZE_MB! MB
) else (
    echo [ERROR] ❌ Build failed. APK not found.
    pause
    exit /b 1
)

echo.
echo 📱 Installation Instructions...
echo [INFO] 1. Enable Developer Options on your Android device:
echo [INFO]    Settings ^> About Phone ^> Tap 'Build Number' 7 times
echo [INFO]
echo [INFO] 2. Enable USB Debugging:
echo [INFO]    Settings ^> Developer Options ^> USB Debugging
echo [INFO]
echo [INFO] 3. Install the APK:
echo [INFO]    Option A: gradlew.bat installDebug ^(if device connected via USB^)
echo [INFO]    Option B: Copy %APK_PATH% to your device and install
echo [INFO]

echo.
echo 🌐 LAN Multiplayer Setup...
echo [INFO] For LAN multiplayer to work:
echo [INFO] 1. All devices must be on the same WiFi network
echo [INFO] 2. Router should allow device-to-device communication
echo [INFO] 3. Firewalls should not block the app ^(ports 54567-54568^)
echo [INFO]

echo.
echo 🎮 How to Play...
echo [INFO] Local Game:
echo [INFO] 1. Enter player names ^(2-4 players^)
echo [INFO] 2. Tap 'Quick Play ^(Local^)'
echo [INFO] 3. Pass device around when it's each player's turn
echo [INFO]
echo [INFO] LAN Multiplayer:
echo [INFO] 1. One device: Tap 'Browse LAN Games' ^> 'Create Room'
echo [INFO] 2. Other devices: Tap 'Browse LAN Games' ^> Join the room
echo [INFO] 3. Host starts the game when all players joined
echo [INFO]

echo.
echo 🏗️ Development Commands...
echo [INFO] Build commands:
echo [INFO]   gradlew.bat assembleDebug    - Build debug APK
echo [INFO]   gradlew.bat assembleRelease  - Build release APK
echo [INFO]   gradlew.bat installDebug     - Install debug APK to connected device
echo [INFO]   gradlew.bat clean            - Clean build files
echo [INFO]
echo [INFO] Testing commands:
echo [INFO]   gradlew.bat test             - Run unit tests
echo [INFO]   gradlew.bat connectedAndroidTest - Run instrumented tests
echo [INFO]

echo.
echo 🔧 Troubleshooting...
echo [INFO] Common issues:
echo [INFO] 1. Build fails with SDK error:
echo [INFO]    - Check local.properties has correct SDK path
echo [INFO]    - Ensure Android SDK is properly installed
echo [INFO]
echo [INFO] 2. Gradle sync issues:
echo [INFO]    - Run: gradlew.bat clean
echo [INFO]    - Check internet connection for dependency downloads
echo [INFO]
echo [INFO] 3. LAN multiplayer not working:
echo [INFO]    - Ensure all devices on same WiFi network
echo [INFO]    - Check router settings allow device communication
echo [INFO]    - Restart the app and try again
echo [INFO]

echo.
echo 📚 Resources...
echo [INFO] Project documentation: README_SIMPLIFIED.md
echo [INFO] Add music files to: %MUSIC_DIR%
echo [INFO] Logs location: app\build\outputs\logs\
echo [INFO]

echo ========================================
echo 🎉 Setup Complete! Ready to play Hot Potato! 🎉
echo ========================================

REM Optional: Ask if user wants to install now
where adb >nul 2>&1
if %errorlevel% equ 0 (
    adb devices | findstr "device$" >nul 2>&1
    if !errorlevel! equ 0 (
        echo.
        set /p INSTALL="🔌 Android device detected. Install now? (y/N): "
        if /i "!INSTALL!"=="y" (
            echo [INFO] Installing APK to connected device...
            call gradlew.bat installDebug
            if !errorlevel! equ 0 (
                echo [INFO] ✅ Installation complete! Check your device.
            ) else (
                echo [ERROR] Installation failed. Try manual installation.
            )
        )
    )
)

echo.
echo [INFO] Happy gaming! May the odds be ever in your favor when the music stops! 🔥🥔

echo.
pause
