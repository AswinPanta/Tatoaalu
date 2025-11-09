# Music Assets for Hot Potato Game

This directory contains music files that play during the Hot Potato game rounds.

## How It Works

1. **Random Selection**: The game randomly selects one music file from this directory
2. **Random Duration**: Music plays for 10-30 seconds (randomized each round)
3. **Elimination**: When music stops, the player holding the potato is eliminated
4. **Loop Continue**: Process repeats until one winner remains

## Supported Formats

- `.mp3` (recommended)
- `.wav`
- `.ogg`
- `.m4a`

## File Naming Convention

Name your music files as:
- `song1.mp3`
- `song2.mp3`
- `song3.mp3`
- etc.

## Adding Your Own Music

1. Copy your music files to this directory
2. Rename them following the convention above
3. Update the `MUSIC_FILES` array in `MusicManager.java` if needed

## Default Behavior

If no music files are found, the game will use a timer-based fallback system that still provides the random 10-30 second gameplay timing without actual music.

## Sample Music Sources

You can add royalty-free music from:
- YouTube Audio Library
- Freesound.org
- Zapsplat (free tier)
- Local music files (ensure you have rights to use them)

## Technical Notes

- Files should be relatively short (30 seconds to 2 minutes)
- The game will loop them if they're shorter than the random play duration
- Keep file sizes reasonable for mobile app distribution
- Test volume levels to ensure they're consistent across files

## Quick Start

To get started immediately, add at least one music file named `song1.mp3` to this directory.