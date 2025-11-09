# 🔥🥔 Hot Potato Game - Build Instructions

This document provides step-by-step instructions to build and run the Hot Potato Android game.

## 📋 Prerequisites

### Required Software
- **Android Studio** (latest stable version) OR **Android SDK Command Line Tools**
- **Java Development Kit (JDK) 17** or higher
- **Git** (for cloning the repository)

### System Requirements
- **Operating System**: Windows 10+, macOS 10.14+, or Linux (Ubuntu 18.04+ recommended)
- **RAM**: 8GB minimum, 16GB recommended
- **Storage**: 10GB free space for Android SDK and project
- **Network**: Internet connection for dependency downloads

## 🚀 Quick Setup (Recommended)

### Option 1: Automated Setup Scripts

#### For Linux/macOS:
```bash
# Clone the repository
git clone <repository-url>
cd TatoAalu

# Run the automated setup script
chmod +x setup.sh
./setup.sh
```

#### For Windows:
```cmd
# Clone the repository
git clone <repository-url>
cd TatoAalu

# Run the automated setup script
setup.bat
```

The setup scripts will:
- ✅ Check prerequisites
- ✅ Download dependencies
- ✅ Build the project
- ✅ Generate APK
- ✅ Optionally install to connected device

### Option 2: Quick Build (Development)

For faster subsequent builds:

```bash
# Linux/macOS
chmod +x quick_build.sh
./quick_build.sh

# Windows
gradlew.bat assembleDebug
```

## 🛠️ Manual Setup

### Step 1: Install Android SDK

#### Using Android Studio (Easiest):
1. Download and install [Android Studio](https://developer.android.com/studio)
2. Launch Android Studio
3. Follow the setup wizard to install Android SDK
4. Note the SDK location (usually `~/Library/Android/sdk` on macOS or `C:\Users\[username]\AppData\Local\Android\Sdk` on Windows)

#### Using Command Line Tools:
1. Download [SDK Command Line Tools](https://developer.android.com/studio#command-tools)
2. Extract to desired location
3. Set `ANDROID_HOME` environment variable
4. Install required SDK components:
   ```bash
   sdkmanager "platform-tools" "platforms;android-34" "build-tools;34.0.0"
   ```

### Step 2: Configure SDK Location

Create `local.properties` file in project root:

```properties
# Replace with your actual Android SDK path
sdk.dir=/Users/YourUsername/Library/Android/sdk

# Examples for different OS:
# macOS: sdk.dir=/Users/YourUsername/Library/Android/sdk
# Windows: sdk.dir=C\:\\Users\\YourUsername\\AppData\\Local\\Android\\Sdk  
# Linux: sdk.dir=/home/YourUsername/Android/Sdk
```

**Important**: Never commit `local.properties` to version control!

### Step 3: Verify Java Installation

```bash
java -version
# Should show Java 17 or higher

javac -version
# Should show matching version
```

If Java 17+ is not installed:
- **macOS**: `brew install openjdk@17`
- **Windows**: Download from [Adoptium](https://adoptium.net/)
- **Linux**: `sudo apt install openjdk-17-jdk`

### Step 4: Build the Project

#### Clean and Build:
```bash
# Linux/macOS
./gradlew clean
./gradlew assembleDebug

# Windows
gradlew.bat clean
gradlew.bat assembleDebug
```

#### Build Release Version:
```bash
# Linux/macOS
./gradlew assembleRelease

# Windows
gradlew.bat assembleRelease
```

### Step 5: Install APK

#### Install to Connected Device:
```bash
# Enable USB debugging on your Android device first
# Linux/macOS
./gradlew installDebug

# Windows
gradlew.bat installDebug
```

#### Manual Installation:
1. Locate APK: `app/build/outputs/apk/debug/app-debug.apk`
2. Transfer to Android device
3. Enable "Install from Unknown Sources"
4. Install the APK

## 🎵 Adding Music Files (Optional)

### Step 1: Prepare Music Files
- **Format**: MP3, WAV, OGG, or M4A
- **Duration**: 30 seconds to 2 minutes recommended
- **Size**: Keep under 5MB each for reasonable APK size
- **Content**: Upbeat, party-style music works best

### Step 2: Add to Project
1. Navigate to: `app/src/main/assets/music/`
2. Add your music files
3. Rename them to: `song1.mp3`, `song2.mp3`, `song3.mp3`, etc.
4. Rebuild the project

### Step 3: Test
- The game will automatically detect and use your music files
- If no music files are found, the game uses a timer-based fallback

## 📱 Testing

### Local Game Testing
1. Install APK on device
2. Launch "Tato Aalu"
3. Enter player names
4. Tap "Quick Play (Local)"
5. Test gameplay by passing device around

### LAN Multiplayer Testing
1. Install APK on multiple devices
2. Ensure all devices are on same WiFi network
3. On one device: "Browse LAN Games" → "Create Room"
4. On other devices: "Browse LAN Games" → Join the room
5. Host starts the game

## 🐛 Troubleshooting

### Build Issues

#### "SDK location not found"
- Create `local.properties` with correct `sdk.dir` path
- Ensure Android SDK is properly installed

#### "Java version incompatible"
- Install Java 17 or higher
- Set `JAVA_HOME` environment variable

#### "Gradle sync failed"
- Check internet connection
- Try: `./gradlew clean` then rebuild

#### "Out of memory"
- Add to `gradle.properties`:
  ```properties
  org.gradle.jvmargs=-Xmx4g -XX:MaxMetaspaceSize=1g
  ```

### Runtime Issues

#### "App won't install"
- Enable "Install from Unknown Sources"
- Check device has enough storage
- Try uninstalling previous versions

#### "LAN multiplayer not working"
- Ensure all devices on same WiFi network
- Check router allows device-to-device communication
- Disable VPNs on devices
- Try restarting the app

#### "No sound/music"
- Check device volume settings
- Verify music files are in correct format
- Check app has audio permissions

### Performance Issues

#### "App runs slowly"
- Close other apps
- Restart device
- Check available RAM/storage
- Try building release version

#### "Large APK size"
- Remove unused music files
- Use smaller/compressed audio files
- Enable ProGuard in release builds

## 🏗️ Development

### Project Structure
```
TatoAalu/
├── app/src/main/java/com/tatoalu/hotpotato/
│   ├── MainActivity.java              # Main menu
│   ├── GameActivity.java              # Game logic
│   ├── RoomBrowserActivity.java       # LAN multiplayer
│   ├── MusicManager.java              # Audio system
│   └── ...
├── app/src/main/res/                  # UI resources
├── app/src/main/assets/music/         # Music files
└── ...
```

### Key Components
- **MainActivity**: Game setup and local play
- **GameActivity**: Core Hot Potato gameplay
- **RoomBrowserActivity**: LAN room discovery
- **MusicManager**: Random music playback system
- **EnhancedLanDiscovery**: Network discovery

### Build Variants
- **Debug**: For development, includes debugging info
- **Release**: Optimized for distribution, ProGuard enabled

### Gradle Commands
```bash
# Build commands
./gradlew assembleDebug              # Build debug APK
./gradlew assembleRelease            # Build release APK
./gradlew installDebug              # Install debug to device
./gradlew clean                     # Clean build files

# Testing commands
./gradlew test                      # Unit tests
./gradlew connectedAndroidTest      # Device tests
./gradlew lint                      # Code analysis

# Other useful commands
./gradlew dependencies              # Show dependencies
./gradlew projects                  # Show project structure
./gradlew tasks                     # Show all available tasks
```

## 📦 Distribution

### Debug APK (Development)
- Location: `app/build/outputs/apk/debug/app-debug.apk`
- Signed with debug key
- Includes debugging information
- Larger file size

### Release APK (Production)
- Location: `app/build/outputs/apk/release/app-release.apk`
- Optimized and obfuscated
- Smaller file size
- Requires signing for distribution

### APK Signing (Release)
1. Generate keystore:
   ```bash
   keytool -genkey -v -keystore my-release-key.keystore -keyalg RSA -keysize 2048 -validity 10000 -alias my-key-alias
   ```

2. Add to `app/build.gradle`:
   ```gradle
   android {
       signingConfigs {
           release {
               storeFile file("path/to/my-release-key.keystore")
               storePassword "store-password"
               keyAlias "my-key-alias"
               keyPassword "key-password"
           }
       }
       buildTypes {
           release {
               signingConfig signingConfigs.release
               minifyEnabled true
           }
       }
   }
   ```

3. Build signed APK:
   ```bash
   ./gradlew assembleRelease
   ```

## 🌟 Features Overview

### Core Gameplay
- ✅ Traditional Hot Potato rules
- ✅ Individual turn-based play
- ✅ Random music timing (10-30 seconds)
- ✅ Progressive player elimination
- ✅ Local multiplayer (2-4 players)

### LAN Multiplayer
- ✅ Mini Militia-style room browser
- ✅ Automatic room discovery
- ✅ 4-digit room codes
- ✅ Real-time synchronization
- ✅ Connection quality indicators

### Audio System
- ✅ Random music selection
- ✅ Custom music support
- ✅ Fallback timer system
- ✅ Smooth fade effects

### Modern UI
- ✅ Material Design 3
- ✅ Fire-themed graphics
- ✅ Smooth animations
- ✅ Responsive layouts

## 🆘 Getting Help

### Common Resources
- **Android Documentation**: https://developer.android.com/docs
- **Gradle Documentation**: https://gradle.org/guides/
- **Material Design**: https://material.io/develop/android

### Project Support
- Check `README_SIMPLIFIED.md` for gameplay instructions
- See `PROJECT_STRUCTURE.md` for detailed architecture
- Review error messages carefully - they usually indicate the exact issue

### Community
- Android development communities on Stack Overflow
- GitHub Issues (if this is an open source project)
- Android development Discord/Reddit communities

---

## ✅ Quick Checklist

Before building, ensure you have:
- [ ] Android SDK installed and configured
- [ ] Java 17+ installed
- [ ] `local.properties` file created with SDK path
- [ ] Internet connection for dependency downloads
- [ ] Sufficient disk space (10GB recommended)

For LAN multiplayer testing:
- [ ] Multiple Android devices available
- [ ] All devices on same WiFi network
- [ ] WiFi allows device-to-device communication

For music features:
- [ ] Music files added to `app/src/main/assets/music/`
- [ ] Files properly named (`song1.mp3`, `song2.mp3`, etc.)
- [ ] File formats are supported (MP3, WAV, OGG, M4A)

---

🎉 **You're ready to build and play Hot Potato!** 

May the odds be ever in your favor when the music stops! 🔥🥔