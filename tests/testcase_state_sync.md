# Testcase: State Sync (state, tick, burn, gameStarted)

Purpose
- Validate server broadcasting of game state and events to all clients.

Steps
1. Start game from host after lobby.
2. Observe client timer updates (tick events).
3. Trigger a burn (let timer expire on a holder) and observe burn message.
4. Verify all devices show the same loser name.

Expected Results
- Clients receive synchronized `tick` updates.
- `burn` event shows consistent loser across devices.

Screenshots
- Tick on host: `tests/screenshots/state_tick_host.png`
- Tick on client: `tests/screenshots/state_tick_client.png`
- Burn event host: `tests/screenshots/state_burn_host.png`
- Burn event client: `tests/screenshots/state_burn_client.png`