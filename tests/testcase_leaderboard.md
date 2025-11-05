# Testcase: Leaderboard (Optional)

Purpose
- Validate leaderboard read/write operations (requires Firebase setup).

Prerequisites
- Valid `google-services.json` configured and network access.

Steps
1. Play a game to completion.
2. Trigger score submission in `LeaderboardManager` (if wired to UI).
3. Verify top scores load without error.

Expected Results
- Scores read/write succeed per Firebase rules.

Screenshots
- Leaderboard screen: `tests/screenshots/leaderboard.png`