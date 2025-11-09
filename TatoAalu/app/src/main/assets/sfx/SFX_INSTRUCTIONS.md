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

