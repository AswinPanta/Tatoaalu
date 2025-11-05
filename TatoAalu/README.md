# Tato Aalu – Hot Potato (Android, Java)

A lightweight, production-ready base for a "hot potato" game built in Android Studio (Java).

## Features
- Java Views (no Kotlin/Compose) for wide compatibility
- Avatars arranged in a circle; tap to pass the potato
- Random burn timer (3.5–7.5s) triggers game over
- Clean separation between Activity and custom `GameView`
- Release build with R8/ProGuard enabled

## Getting Started
1. Open Android Studio.
2. Click "Open" and select this folder: `TatoAalu`.
3. Let Gradle sync; if prompted to upgrade AGP/Gradle, accept.
4. Run on an emulator or device (Android 6.0+).

## Project Structure
- `app/src/main/java/com/tatoalu/hotpotato/`
  - `MainActivity.java`: Menu and player count.
  - `GameActivity.java`: Hosts the game.
  - `GameView.java`: Rendering and gameplay loop.
- `app/src/main/res/drawable/`: Avatars, `potato.png`, `campfire.png`.
- `app/src/main/res/layout/`: XML layouts for activities.
- `app/proguard-rules.pro`: Basic keep rules for release.

## Notes
- Icon uses `@drawable/potato`. Replace with a proper adaptive icon later.
- All bundled images are public domain from Openclipart.
- Tuning: adjust sizes, thresholds, and names in `GameView.java`.

## Next Steps
- Add sounds (pass, burn) via `SoundPool`.
- Add settings: difficulty, avatar selection.
- Add animations for passes (ValueAnimator) and particle effects.
- Add a scoreboard and multiple rounds.