# Hot Potato Game - Project Structure 🔥🥔

## 📁 Project Overview

This is a simplified, LAN-only Android Hot Potato game with traditional gameplay mechanics and Mini Militia-style multiplayer discovery.

```
TatoAalu/
├── 📁 app/
│   ├── 📁 build/                           # Build outputs (auto-generated)
│   ├── 📁 src/
│   │   ├── 📁 main/
│   │   │   ├── 📁 java/com/tatoalu/hotpotato/
│   │   │   │   ├── 🎮 MainActivity.java                 # Main menu & local game setup
│   │   │   │   ├── 🎮 GameActivity.java                 # Core Hot Potato gameplay
│   │   │   │   ├── 🌐 RoomBrowserActivity.java          # LAN game discovery (Mini Militia style)
│   │   │   │   ├── 📱 GameView.java                     # Custom game rendering view
│   │   │   │   ├── 🎵 MusicManager.java                 # Random music playback & timing
│   │   │   │   ├── 🌐 LanMultiplayerManager.java        # Simplified LAN networking
│   │   │   │   ├── 🔍 EnhancedLanDiscovery.java         # Advanced local network discovery
│   │   │   │   ├── 📋 RoomListAdapter.java              # RecyclerView adapter for rooms
│   │   │   │   ├── 🎯 LanClient.java                    # LAN client functionality
│   │   │   │   ├── 🎯 LanServer.java                    # LAN server functionality
│   │   │   │   ├── 🏆 LeaderboardActivity.java          # Score tracking
│   │   │   │   ├── 🏆 LeaderboardAdapter.java           # Leaderboard list adapter
│   │   │   │   ├── 🏆 LeaderboardManager.java           # Score management
│   │   │   │   ├── ⚙️ Config.java                       # Game configuration
│   │   │   │   └── ⚙️ GameMode.java                     # Game mode definitions
│   │   │   │
│   │   │   ├── 📁 res/
│   │   │   │   ├── 📁 drawable/                         # Images, icons, and vector graphics
│   │   │   │   │   ├── 🥔 potato.png                    # Main potato icon
│   │   │   │   │   ├── 🔥 bg_flame.xml                  # Fire-themed background
│   │   │   │   │   ├── 🏕️ campfire.png                  # Campfire elimination graphic
│   │   │   │   │   ├── 👤 avatar_red.xml                # Player avatar
│   │   │   │   │   ├── ⏱️ timer_ring.xml                # Circular timer graphic
│   │   │   │   │   ├── 🥔 potato_optimized.xml          # Optimized potato vector
│   │   │   │   │   ├── 👤 player_avatar_optimized.xml   # Optimized player avatar
│   │   │   │   │   ├── 🏕️ campfire_optimized.xml        # Optimized campfire
│   │   │   │   │   ├── 🥇 ic_gold_medal.xml             # Gold medal icon
│   │   │   │   │   ├── 🥈 ic_silver_medal.xml           # Silver medal icon
│   │   │   │   │   ├── 🥉 ic_bronze_medal.xml           # Bronze medal icon
│   │   │   │   │   ├── 🎵 ic_music_note.xml             # Music note icon
│   │   │   │   │   └── 🏅 ic_rank_default.xml           # Default rank icon
│   │   │   │   │
│   │   │   │   ├── 📁 layout/                           # UI layouts
│   │   │   │   │   ├── 🏠 activity_main.xml             # Main menu layout
│   │   │   │   │   ├── 🎮 activity_game.xml             # Game screen layout
│   │   │   │   │   ├── 🌐 activity_room_browser.xml     # LAN browser layout
│   │   │   │   │   ├── 🏆 activity_leaderboard.xml      # Leaderboard layout
│   │   │   │   │   ├── 📝 item_room.xml                 # Room list item layout
│   │   │   │   │   ├── 📝 item_leaderboard.xml          # Leaderboard item layout
│   │   │   │   │   └── 💬 dialog_room_code.xml          # Room code input dialog
│   │   │   │   │
│   │   │   │   ├── 📁 values/                           # App resources
│   │   │   │   │   ├── 🎨 colors.xml                    # Color definitions
│   │   │   │   │   ├── 📝 strings.xml                   # Text strings (English)
│   │   │   │   │   ├── 🎨 styles.xml                    # UI styles and themes
│   │   │   │   │   └── 📋 arrays.xml                    # String arrays for spinners
│   │   │   │   │
│   │   │   │   ├── 📁 values-ne/                        # Nepali language resources
│   │   │   │   │   └── 📝 strings.xml                   # Nepali translations
│   │   │   │   │
│   │   │   │   └── 📁 raw/                              # Raw assets (Lottie animations)
│   │   │   │       ├── 🔥 fire_particles.json          # Fire particle animation
│   │   │   │       ├── 🎵 music_notes.json             # Music note animation
│   │   │   │       ├── 🥔 potato_glow.json             # Glowing potato animation
│   │   │   │       └── 🏆 trophy_fire.json             # Victory trophy animation
│   │   │   │
│   │   │   ├── 📁 assets/                               # Asset files
│   │   │   │   └── 📁 music/                            # Music files for gameplay
│   │   │   │       ├── 📝 README.md                    # Music setup instructions
│   │   │   │       ├── 📝 MUSIC_FILES_GO_HERE.txt      # Placeholder with instructions
│   │   │   │       ├── 🎵 song1.mp3                    # (Add your music files here)
│   │   │   │       ├── 🎵 song2.mp3                    # Random music for gameplay
│   │   │   │       └── 🎵 song3.mp3                    # (Optional - more variety)
│   │   │   │
│   │   │   └── 📄 AndroidManifest.xml                   # App permissions & activities
│   │   │
│   │   └── 📁 test/                                     # Unit tests (optional)
│   │
│   ├── 📄 build.gradle                                  # App-level build configuration
│   ├── 📄 proguard-rules.pro                          # Code obfuscation rules
│   └── 📄 google-services.json.template               # (REMOVED - no Firebase)
│
├── 📁 gradle/                                          # Gradle wrapper files
│   └── 📁 wrapper/
│       ├── 📄 gradle-wrapper.jar                      # Gradle wrapper binary
│       └── 📄 gradle-wrapper.properties               # Gradle version config
│
├── 📄 build.gradle                                     # Project-level build config
├── 📄 settings.gradle                                  # Project settings
├── 📄 gradle.properties                                # Gradle properties
├── 📄 gradlew                                          # Gradle wrapper script (Linux/Mac)
├── 📄 gradlew.bat                                      # Gradle wrapper script (Windows)
├── 📄 local.properties.template                       # Template for SDK path config
├── 📄 .gitignore                                       # Git ignore rules
│
├── 📄 README_SIMPLIFIED.md                            # Main project documentation
├── 📄 PROJECT_STRUCTURE.md                            # This file
├── 📄 setup.sh                                        # Setup script (Linux/Mac)
├── 📄 setup.bat                                       # Setup script (Windows)
└── 📄 quick_build.sh                                  # Quick build script
```

## 🎯 Key Components

### 🎮 Core Game Classes

| File | Purpose | Key Features |
|------|---------|--------------|
| `MainActivity.java` | Main menu & setup | Player name input, local game launcher, LAN browser access |
| `GameActivity.java` | Core gameplay | Hot Potato logic, music integration, player elimination |
| `GameView.java` | Custom game rendering | Visual effects, player avatars, potato animations |
| `MusicManager.java` | Music system | Random music playback, 10-30s timing, fallback system |

### 🌐 LAN Multiplayer Classes

| File | Purpose | Key Features |
|------|---------|--------------|
| `RoomBrowserActivity.java` | Game discovery UI | Mini Militia-style room browser, auto-discovery |
| `EnhancedLanDiscovery.java` | Network discovery | UDP broadcasts, NSD integration, room detection |
| `LanMultiplayerManager.java` | Simplified networking | LAN-only connectivity, game state sync |
| `RoomListAdapter.java` | Room list UI | RecyclerView adapter, connection indicators |

### 🏆 Support Classes

| File | Purpose | Key Features |
|------|---------|--------------|
| `LeaderboardManager.java` | Score tracking | Win counts, player statistics, local storage |
| `Config.java` | Game configuration | Constants, settings, gameplay parameters |
| `LanServer.java` / `LanClient.java` | Network communication | TCP/UDP handling, message protocols |

## 🎨 UI Resources

### 📱 Layouts
- **Main Menu**: Player setup, game mode selection, LAN browser access
- **Game Screen**: Circular timer, player indicators, elimination effects
- **Room Browser**: Room list, connection status, create/join buttons
- **Leaderboard**: Win statistics, player rankings

### 🎨 Visual Assets
- **Fire Theme**: Orange/red color scheme, flame backgrounds
- **Potato Graphics**: Main game icon, animated potato effects
- **Material Design 3**: Modern UI components, smooth animations

## 🔧 Build System

### 📦 Dependencies
```gradle
// Core Android
androidx.appcompat:appcompat:1.7.0
androidx.core:core:1.13.1
com.google.android.material:material:1.12.0

// UI Components
androidx.constraintlayout:constraintlayout:2.2.1
androidx.recyclerview:recyclerview:1.3.2
androidx.swiperefreshlayout:swiperefreshlayout:1.1.0

// Animations
com.airbnb.android:lottie:6.1.0

// Networking
com.squareup.okhttp3:okhttp:4.12.0
```

### 🚀 Build Scripts
- `setup.sh` / `setup.bat`: Full project setup and build
- `quick_build.sh`: Fast development builds
- `gradlew`: Standard Gradle wrapper commands

## 🎵 Music System

### 📁 Music Directory Structure
```
app/src/main/assets/music/
├── song1.mp3    # Add your music files here
├── song2.mp3    # Supported: MP3, WAV, OGG, M4A
├── song3.mp3    # Files should be 30s-2min long
└── README.md    # Setup instructions
```

### 🎶 Music Features
- **Random Selection**: Picks different songs each round
- **Random Duration**: 10-30 seconds of play time
- **Fallback System**: Works without music files using timer
- **Fade Effects**: Smooth music transitions

## 🌐 LAN Multiplayer Architecture

### 🔗 Network Protocols
- **Discovery**: UDP broadcasts on port 54568
- **Game Data**: TCP connections on port 54567
- **Service Discovery**: Android NSD integration
- **Room Codes**: 4-digit numeric codes for easy sharing

### 📡 Connection Flow
1. **Host creates room** → Starts server, broadcasts availability
2. **Clients discover rooms** → Auto-detection via UDP/NSD
3. **Join room** → TCP connection established
4. **Game sync** → Real-time state synchronization
5. **Play together** → Music stops simultaneously on all devices

## 🎮 Gameplay Features

### 🔥 Traditional Hot Potato Rules
- **Individual turns**: Each player gets the potato individually
- **Music timing**: Random 10-30 second music playback
- **Elimination**: Player holding potato when music stops is out
- **Progressive rounds**: Continue until one winner remains

### 🏆 Game Modes
- **Local Multiplayer**: 2-4 players on same device
- **LAN Multiplayer**: Multiple devices, same WiFi network
- **Scoring System**: Win tracking, leaderboards

## 🚀 Getting Started

### 📋 Prerequisites
- **Android SDK**: API 23+ (Android 6.0+)
- **Java**: Version 17 or higher
- **Build Tools**: Android Studio or Gradle

### ⚡ Quick Start
1. **Setup**: Run `setup.sh` (Linux/Mac) or `setup.bat` (Windows)
2. **Build**: `./gradlew assembleDebug`
3. **Install**: `./gradlew installDebug` or transfer APK manually
4. **Play**: Launch app, enter names, start playing!

### 🎵 Adding Music (Optional)
1. Copy MP3/WAV files to `app/src/main/assets/music/`
2. Name them: `song1.mp3`, `song2.mp3`, etc.
3. Rebuild project
4. Enjoy random music during gameplay!

## 🔍 Architecture Highlights

### ✨ What Makes This Special
- **No Firebase**: Pure local networking, no cloud dependency
- **Traditional Gameplay**: Authentic Hot Potato rules with individual turns
- **Mini Militia-Style Discovery**: Smooth room browser, auto-discovery
- **Music-Driven**: Real random timing, not predictable timers
- **Modern UI**: Material Design 3, fire theme, smooth animations
- **Cross-Device Sync**: Perfect synchronization across all players

### 🧹 Simplified Architecture
- ❌ Removed complex UnifiedMultiplayerManager
- ❌ Removed Firebase dependencies
- ❌ Removed Musical Chairs mode
- ✅ LAN-only networking
- ✅ Traditional Hot Potato focus
- ✅ Clean, maintainable code

---

**🎉 Ready to build and play! Follow the setup instructions and enjoy the hot potato action! 🔥🥔**