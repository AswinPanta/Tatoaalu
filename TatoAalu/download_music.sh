#!/bin/bash

# Hot Potato Game - Music Download Script
# Downloads CC0/Creative Commons licensed music for the game

set -e

echo "🎵 Hot Potato Game - Music Download Script 🎵"
echo "============================================="

# Create music directory if it doesn't exist
MUSIC_DIR="app/src/main/assets/music"
mkdir -p "$MUSIC_DIR"

echo "📁 Music directory: $MUSIC_DIR"

# Function to download and convert music
download_track() {
    local url="$1"
    local filename="$2"
    local description="$3"

    echo "🎶 Downloading: $description"

    # Check if file already exists
    if [[ -f "$MUSIC_DIR/$filename" ]]; then
        echo "   ✅ Already exists: $filename"
        return
    fi

    # Download the file
    if command -v wget &> /dev/null; then
        wget -q -O "$MUSIC_DIR/$filename" "$url"
    elif command -v curl &> /dev/null; then
        curl -s -L -o "$MUSIC_DIR/$filename" "$url"
    else
        echo "   ❌ Error: Neither wget nor curl found. Please install one of them."
        return 1
    fi

    # Check if download was successful
    if [[ -f "$MUSIC_DIR/$filename" ]] && [[ -s "$MUSIC_DIR/$filename" ]]; then
        echo "   ✅ Downloaded: $filename"
    else
        echo "   ❌ Failed to download: $filename"
        rm -f "$MUSIC_DIR/$filename"
    fi
}

echo ""
echo "🎵 Downloading Creative Commons / CC0 Music Tracks..."
echo ""

# Note: These are placeholder URLs - you need to get actual CC0 music files
# Here are some excellent sources:

echo "📝 IMPORTANT: Actual music files need to be downloaded manually"
echo "   This script creates placeholder instructions."
echo ""

# Create instruction file
cat > "$MUSIC_DIR/DOWNLOAD_INSTRUCTIONS.md" << 'EOF'
# Hot Potato Music Download Instructions

## 🎵 Free Music Sources (CC0/Creative Commons)

### 1. Freesound.org (Best for game music)
Visit: https://freesound.org/
Search terms: "game music", "party music", "upbeat", "loop"
Filter by: Creative Commons 0 (CC0) license

**Recommended tracks:**
- Search: "upbeat game music cc0"
- Search: "party music loop free"
- Search: "electronic dance cc0"

### 2. OpenGameArt.org
Visit: https://opengameart.org/art-search-advanced?keys=&field_art_type_tid%5B%5D=12
Filter: Music, CC0 or CC-BY licenses

### 3. Tallbeard Studios Music Pack
Visit: https://tallbeard.itch.io/music-loop-bundle
License: CC0 (Public Domain)
Contains: 150+ music tracks

### 4. YouTube Audio Library
Visit: https://studio.youtube.com/channel/[YOUR_CHANNEL]/music
Filter: No attribution required

## 📁 File Naming Convention

Rename downloaded files to:
- song1.mp3 (or .wav, .ogg, .m4a)
- song2.mp3
- song3.mp3
- song4.mp3
- song5.mp3

## 🎯 Ideal Characteristics

- **Duration**: 30 seconds to 2 minutes
- **Tempo**: Upbeat, energetic (120-140 BPM ideal)
- **Style**: Electronic, pop, dance, party music
- **File size**: Under 5MB each
- **Format**: MP3, WAV, OGG, or M4A

## 🚀 Quick Download Guide

1. Go to https://freesound.org/
2. Search for "upbeat game music"
3. Filter by "CC0" license
4. Download 3-5 tracks you like
5. Rename them song1.mp3, song2.mp3, etc.
6. Place them in this directory
7. Rebuild the game

## ⚡ Pro Tips

- Look for tracks tagged: "loop", "game", "upbeat", "party", "fun"
- Avoid tracks with vocals (instrumental works better)
- Test different tracks to find ones that fit the game mood
- Keep file sizes reasonable for mobile distribution

EOF

# Create some example SFX files (these we can include directly)
echo ""
echo "🔊 Creating Sound Effects..."

# Create simple SFX using system commands (if available)
create_sfx() {
    local filename="$1"
    local description="$2"
    local duration="$3"
    local freq="$4"

    echo "🔊 Creating: $description"

    # Try to create simple tone (requires sox if available)
    if command -v sox &> /dev/null; then
        sox -n "$MUSIC_DIR/../sfx/$filename" synth "$duration" sine "$freq" fade 0.1 "$duration" 0.1 vol 0.3
        echo "   ✅ Created: $filename"
    else
        echo "   ⚠️  Sox not found - skipping SFX generation"
        echo "   💡 Install sox: brew install sox (macOS) or apt-get install sox (Linux)"
    fi
}

# Create SFX directory
mkdir -p "$MUSIC_DIR/../sfx"

# Create some simple sound effects
if command -v sox &> /dev/null; then
    create_sfx "elimination_beep.wav" "Elimination sound" "0.5" "800"
    create_sfx "victory_fanfare.wav" "Victory sound" "1.0" "1200"
    create_sfx "music_start.wav" "Music start sound" "0.3" "600"
    create_sfx "countdown_tick.wav" "Countdown tick" "0.2" "1000"
else
    echo "🔊 SFX creation requires 'sox' - creating instructions instead"
    cat > "$MUSIC_DIR/../sfx/SFX_INSTRUCTIONS.md" << 'EOF'
# Sound Effects for Hot Potato

## 🔊 Required SFX Files

Create these sound files and place them in this directory:

1. **elimination_beep.wav** - Sound when player is eliminated
2. **victory_fanfare.wav** - Sound when someone wins
3. **music_start.wav** - Sound when music starts
4. **countdown_tick.wav** - Countdown sound

## 🎵 Sources for SFX

### Freesound.org
- Search: "game beep", "victory", "fanfare", "countdown"
- Filter: CC0 license

### Zapsplat (Free Tier)
- Visit: https://zapsplat.com/
- Category: Game Sounds

### Generate Your Own
Install sox: `brew install sox` (macOS) or `apt install sox` (Linux)

```bash
# Elimination beep
sox -n elimination_beep.wav synth 0.5 sine 800 fade 0.1 0.5 0.1 vol 0.3

# Victory fanfare
sox -n victory_fanfare.wav synth 1.0 sine 1200 fade 0.1 1.0 0.1 vol 0.3

# Music start
sox -n music_start.wav synth 0.3 sine 600 fade 0.1 0.3 0.1 vol 0.3

# Countdown tick
sox -n countdown_tick.wav synth 0.2 sine 1000 fade 0.1 0.2 0.1 vol 0.3
```

EOF
fi

echo ""
echo "📊 Summary:"
echo "=========="

MUSIC_COUNT=$(find "$MUSIC_DIR" -name "*.mp3" -o -name "*.wav" -o -name "*.ogg" -o -name "*.m4a" | wc -l)
SFX_COUNT=$(find "$MUSIC_DIR/../sfx" -name "*.wav" -o -name "*.mp3" | wc -l 2>/dev/null || echo "0")

echo "🎵 Music files found: $MUSIC_COUNT"
echo "🔊 SFX files created: $SFX_COUNT"
echo ""

if [[ $MUSIC_COUNT -eq 0 ]]; then
    echo "⚠️  No music files found!"
    echo "   📋 Follow instructions in: $MUSIC_DIR/DOWNLOAD_INSTRUCTIONS.md"
    echo "   🎯 The game will use timer-based fallback until you add music"
else
    echo "✅ Music files ready! The game will use them for random playback."
fi

echo ""
echo "📚 Next Steps:"
echo "=============="
echo "1. 📖 Read: $MUSIC_DIR/DOWNLOAD_INSTRUCTIONS.md"
echo "2. 🎵 Download 3-5 music tracks from recommended sources"
echo "3. 📝 Rename them to: song1.mp3, song2.mp3, etc."
echo "4. 📁 Place them in: $MUSIC_DIR/"
echo "5. 🔨 Rebuild the game: ./gradlew assembleDebug"
echo "6. 🎮 Enjoy Hot Potato with music!"
echo ""
echo "💡 Tip: Even without music, the game works great with timer-based fallback!"
echo ""
echo "🎉 Setup complete! Happy gaming! 🔥🥔"
