# Hot Potato - LAN-Only Android Game 🔥🥔

A simplified, traditional Hot Potato party game for Android with LAN multiplayer support.

## 🎮 What is Hot Potato?

Hot Potato is a fast-paced party game where players pass an imaginary "potato" around while music plays. When the music stops randomly, whoever has the potato is eliminated. The last player standing wins!

## 🌟 Features

### ✨ Core Gameplay
- **Traditional Hot Potato Rules**: Music plays, potato passes, music stops randomly, player eliminated
- **Turn-Based Individual Play**: Each player gets their turn with the potato
- **Random Music Duration**: 10-30 seconds of random music per round
- **Progressive Elimination**: Players eliminated one by one until winner remains
- **Local Multiplayer**: 2-4 players on same device

### 🌐 LAN Multiplayer
- **Room Browser**: Mini Militia-style game discovery
- **Easy Room Creation**: Generate 4-digit room codes
- **Auto Discovery**: Automatic detection of nearby games
- **Real-time Sync**: All players see game state in real-time
- **Connection Quality**: Visual indicators for connection strength

### 🎵 Music System
- **Random Music Selection**: Plays different songs each round
- **Custom Music Support**: Add your own music files
- **Fallback System**: Works without music files using timer
- **Fade Effects**: Smooth music transitions

### 🎨 Modern UI
- **Material Design 3**: Beautiful, modern interface
- **Fire Theme**: Hot, fiery visual effects with orange/red colors
- **Smooth Animations**: Engaging transitions and effects
- **Responsive Layout**: Works on all Android screen sizes

## 📱 System Requirements

- **Android 5.0+** (API Level 21+)
- **WiFi Connection** (for LAN multiplayer)
- **Same Network** (all players must be on same WiFi)

## 🚀 Quick Start

### Local Game (Single Device)
1. Open the app
2. Enter player names (2-4 players)
3. Tap "Quick Play (Local)"
4. Pass the device around when it's each player's turn
5. When music stops, that player is eliminated!

### LAN Multiplayer
1. **Host creates room:**
   - Tap "🌐 Browse LAN Games"
   - Enter your name
   - Tap "🏠 Create Room"
   - Share the 4-digit room code with friends

2. **Players join:**
   - Open app on their devices
   - Tap "🌐 Browse LAN Games" 
   - Find the room in the list or wait for auto-discovery
   - Tap "Join" on the desired room

3. **Start playing:**
   - Host starts the game when ready
   - Music plays on all devices
   - Current player's device shows they have the potato
   - When music stops, that player is eliminated
   - Continue until one winner remains!

## 🎵 Adding Custom Music

1. Navigate to `app/src/main/assets/music/` in your project
2. Add your music files (MP3, WAV, OGG, M4A)
3. Name them: `song1.mp3`, `song2.mp3`, etc.
4. Rebuild the app
5. The game will randomly select from your music files

**Tip**: Use short, upbeat songs (30 seconds - 2 minutes work best)

## 🏗️ Technical Architecture

### Core Components
- **MainActivity**: Game setup and local play launcher
- **RoomBrowserActivity**: LAN game discovery and joining
- **GameActivity**: Main game screen and Hot Potato logic
- **MusicManager**: Handles random music playback and timing
- **LanMultiplayerManager**: Simplified LAN networking
- **EnhancedLanDiscovery**: Advanced local network discovery

### Network Architecture
- **UDP Broadcasts**: Fast room discovery
- **TCP Connections**: Reliable game data transmission
- **NSD Integration**: Android Network Service Discovery
- **Real-time Sync**: Game state synchronized across all devices

### Game Flow
```
Local Game:           LAN Multiplayer:
Start App            Start App
↓                    ↓
Enter Names          Browse LAN Games
↓                    ↓
Quick Play           Create/Join Room
↓                    ↓
Hot Potato Game      Wait for Players
↓                    ↓
Music Plays          Host Starts Game
↓                    ↓
Random Stop          Synchronized Hot Potato
↓                    ↓
Player Eliminated    Music Stops on All Devices
↓                    ↓
Repeat Until Winner  Player Eliminated
                     ↓
                     Repeat Until Winner
```

## 🎯 Key Improvements Made

### Simplified Architecture
- ❌ Removed Firebase (no cloud dependency)
- ❌ Removed Musical Chairs (Hot Potato only)
- ❌ Removed complex UnifiedMultiplayerManager
- ✅ Pure LAN-only networking
- ✅ Simplified game modes
- ✅ Traditional Hot Potato rules

### Enhanced LAN Experience
- ✅ Mini Militia-style room browser
- ✅ Automatic room discovery
- ✅ Visual connection quality indicators
- ✅ Real-time player list updates
- ✅ Robust connection handling

### Better Gameplay
- ✅ True random music stopping (10-30 seconds)
- ✅ Individual turn-based play
- ✅ Progressive player elimination
- ✅ Audio feedback for eliminations
- ✅ Victory celebrations

## 🔧 Build Instructions

1. **Clone the repository**
2. **Open in Android Studio**
3. **Add music files** (optional):
   - Create folder: `app/src/main/assets/music/`
   - Add MP3/WAV files named `song1.mp3`, `song2.mp3`, etc.
4. **Build and install** on Android devices
5. **Ensure all devices are on same WiFi network**
6. **Start playing!**

## 🐛 Troubleshooting

### LAN Multiplayer Issues
- **Can't find rooms**: Ensure all devices on same WiFi network
- **Connection failed**: Check firewall/antivirus settings
- **Game desync**: Restart the game and try again

### Music Issues
- **No sound**: Check device volume and permissions
- **Music not random**: Verify music files are properly named
- **Playback errors**: Ensure music files are valid format

### General Issues
- **App crashes**: Check Android version (need 5.0+)
- **Poor performance**: Close other apps, restart device
- **UI problems**: Try different screen orientation

## 🤝 Contributing

This is a simplified, educational implementation focusing on:
- Clean, readable code
- Traditional Hot Potato gameplay
- Reliable LAN multiplayer
- Modern Android development practices

## 📄 License

Educational/Open Source - Feel free to learn from and modify the code!

---

**Enjoy playing Hot Potato! 🔥🥔**

*May the odds be ever in your favor when the music stops!*