# Testcase: Lobby Matchmaking and Start

Purpose
- Validate the 45s matchmaking lobby flow, player join status, and manual start.

Steps
1. Host opens `Host Multiplayer`.
2. Client joins via `Join Multiplayer`; repeat for multiple clients as available.
3. Observe player status updates on host (joined players listed or count reflected).
4. Verify `Start Game` becomes enabled when at least 2 players are present.
5. Host taps `Start Game`.

Expected Results
- Lobby countdown visible (`45s` window).
- Clients show joined status.
- On start, `gameStarted` is broadcast; game timer becomes visible and lobby elements are hidden.

Screenshots
- Lobby before join: `tests/screenshots/lobby_pre_join.png`
- Lobby with players: `tests/screenshots/lobby_players.png`
- Game started on host: `tests/screenshots/lobby_game_started_host.png`
- Game started on client: `tests/screenshots/lobby_game_started_client.png`