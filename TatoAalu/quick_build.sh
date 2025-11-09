#!/bin/bash

# Quick Build Script for Hot Potato Game
# Fast development build without full setup checks

set -e

echo "🚀 Quick Build - Hot Potato Game"
echo "================================"

# Check if gradlew exists
if [[ ! -f "gradlew" ]]; then
    echo "❌ Error: Run this from the TatoAalu project root directory"
    exit 1
fi

# Make gradlew executable
chmod +x gradlew

echo "🧹 Cleaning..."
./gradlew clean --quiet

echo "🔨 Building debug APK..."
./gradlew assembleDebug --quiet

# Check if build succeeded
APK_PATH="app/build/outputs/apk/debug/app-debug.apk"
if [[ -f "$APK_PATH" ]]; then
    echo "✅ Build successful!"
    echo "📦 APK: $APK_PATH"

    # Show APK size
    SIZE=$(du -h "$APK_PATH" | cut -f1)
    echo "📏 Size: $SIZE"

    # Try to install if device connected
    if command -v adb &> /dev/null && adb devices | grep -q "device$"; then
        echo "📱 Installing to connected device..."
        ./gradlew installDebug --quiet
        echo "✅ Installed!"
    else
        echo "📱 No device connected for auto-install"
    fi
else
    echo "❌ Build failed!"
    exit 1
fi

echo "🎉 Ready to play!"
