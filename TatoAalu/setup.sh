#!/bin/bash

# Hot Potato Game - Setup and Build Script
# This script sets up the development environment and builds the Android APK

set -e

echo "🔥🥔 Hot Potato Game - Setup Script 🥔🔥"
echo "========================================"

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Function to print colored output
print_status() {
    echo -e "${GREEN}[INFO]${NC} $1"
}

print_warning() {
    echo -e "${YELLOW}[WARNING]${NC} $1"
}

print_error() {
    echo -e "${RED}[ERROR]${NC} $1"
}

print_header() {
    echo -e "${BLUE}$1${NC}"
}

# Check if we're in the right directory
if [[ ! -f "gradlew" ]]; then
    print_error "gradlew not found. Please run this script from the TatoAalu project root directory."
    exit 1
fi

print_header "🔍 Checking Prerequisites..."

# Check for Java
if command -v java &> /dev/null; then
    JAVA_VERSION=$(java -version 2>&1 | head -n1 | cut -d'"' -f2)
    print_status "Java found: $JAVA_VERSION"
else
    print_error "Java not found. Please install Java 17 or higher."
    print_status "Install Java: https://adoptium.net/"
    exit 1
fi

# Check for Android SDK
if [[ -z "$ANDROID_HOME" ]]; then
    print_warning "ANDROID_HOME not set. Checking for local.properties..."
    if [[ ! -f "local.properties" ]]; then
        print_error "local.properties not found and ANDROID_HOME not set."
        print_status "Please create local.properties file with your SDK path:"
        print_status "1. Copy local.properties.template to local.properties"
        print_status "2. Edit local.properties and set your Android SDK path"
        print_status "   Example: sdk.dir=/path/to/Android/Sdk"
        exit 1
    else
        print_status "Found local.properties file"
    fi
else
    print_status "Android SDK found: $ANDROID_HOME"
fi

# Make gradlew executable
print_status "Making gradlew executable..."
chmod +x gradlew

print_header "🧹 Cleaning Project..."
./gradlew clean

print_header "📦 Downloading Dependencies..."
./gradlew build --dry-run

print_header "🎵 Setting up Music Directory..."
MUSIC_DIR="app/src/main/assets/music"
if [[ ! -d "$MUSIC_DIR" ]]; then
    print_status "Creating music directory..."
    mkdir -p "$MUSIC_DIR"
fi

# Check if user has music files
MUSIC_COUNT=$(find "$MUSIC_DIR" -name "*.mp3" -o -name "*.wav" -o -name "*.ogg" -o -name "*.m4a" | wc -l)
if [[ $MUSIC_COUNT -eq 0 ]]; then
    print_warning "No music files found in $MUSIC_DIR"
    print_status "The game will use timer-based fallback (still works great!)"
    print_status "To add music:"
    print_status "1. Add MP3/WAV files to $MUSIC_DIR"
    print_status "2. Name them: song1.mp3, song2.mp3, etc."
    print_status "3. Rebuild the project"
else
    print_status "Found $MUSIC_COUNT music files"
fi

print_header "🔨 Building Project..."

# Build debug APK
print_status "Building debug APK..."
./gradlew assembleDebug

# Check if build was successful
APK_PATH="app/build/outputs/apk/debug/app-debug.apk"
if [[ -f "$APK_PATH" ]]; then
    print_status "✅ Build successful!"
    print_status "Debug APK location: $APK_PATH"

    # Get APK size
    APK_SIZE=$(du -h "$APK_PATH" | cut -f1)
    print_status "APK size: $APK_SIZE"
else
    print_error "❌ Build failed. Check the error messages above."
    exit 1
fi

print_header "📱 Installation Instructions..."
print_status "1. Enable Developer Options on your Android device:"
print_status "   Settings > About Phone > Tap 'Build Number' 7 times"
print_status ""
print_status "2. Enable USB Debugging:"
print_status "   Settings > Developer Options > USB Debugging"
print_status ""
print_status "3. Install the APK:"
print_status "   Option A: ./gradlew installDebug (if device connected via USB)"
print_status "   Option B: Copy $APK_PATH to your device and install"
print_status ""

print_header "🌐 LAN Multiplayer Setup..."
print_status "For LAN multiplayer to work:"
print_status "1. All devices must be on the same WiFi network"
print_status "2. Router should allow device-to-device communication"
print_status "3. Firewalls should not block the app (ports 54567-54568)"
print_status ""

print_header "🎮 How to Play..."
print_status "Local Game:"
print_status "1. Enter player names (2-4 players)"
print_status "2. Tap 'Quick Play (Local)'"
print_status "3. Pass device around when it's each player's turn"
print_status ""
print_status "LAN Multiplayer:"
print_status "1. One device: Tap 'Browse LAN Games' > 'Create Room'"
print_status "2. Other devices: Tap 'Browse LAN Games' > Join the room"
print_status "3. Host starts the game when all players joined"
print_status ""

print_header "🏗️ Development Commands..."
print_status "Build commands:"
print_status "  ./gradlew assembleDebug    - Build debug APK"
print_status "  ./gradlew assembleRelease  - Build release APK"
print_status "  ./gradlew installDebug     - Install debug APK to connected device"
print_status "  ./gradlew clean            - Clean build files"
print_status ""
print_status "Testing commands:"
print_status "  ./gradlew test             - Run unit tests"
print_status "  ./gradlew connectedAndroidTest - Run instrumented tests"
print_status ""

print_header "🔧 Troubleshooting..."
print_status "Common issues:"
print_status "1. Build fails with SDK error:"
print_status "   - Check local.properties has correct SDK path"
print_status "   - Ensure Android SDK is properly installed"
print_status ""
print_status "2. Gradle sync issues:"
print_status "   - Run: ./gradlew clean"
print_status "   - Check internet connection for dependency downloads"
print_status ""
print_status "3. LAN multiplayer not working:"
print_status "   - Ensure all devices on same WiFi network"
print_status "   - Check router settings allow device communication"
print_status "   - Restart the app and try again"
print_status ""

print_header "📚 Resources..."
print_status "Project documentation: README_SIMPLIFIED.md"
print_status "Add music files to: $MUSIC_DIR"
print_status "Logs location: app/build/outputs/logs/"
print_status ""

echo "========================================"
echo -e "${GREEN}🎉 Setup Complete! Ready to play Hot Potato! 🎉${NC}"
echo "========================================"

# Optional: Ask if user wants to install now
if command -v adb &> /dev/null; then
    if adb devices | grep -q "device$"; then
        echo ""
        read -p "🔌 Android device detected. Install now? (y/N): " -n 1 -r
        echo
        if [[ $REPLY =~ ^[Yy]$ ]]; then
            print_status "Installing APK to connected device..."
            ./gradlew installDebug
            print_status "✅ Installation complete! Check your device."
        fi
    fi
fi

echo ""
print_status "Happy gaming! May the odds be ever in your favor when the music stops! 🔥🥔"
