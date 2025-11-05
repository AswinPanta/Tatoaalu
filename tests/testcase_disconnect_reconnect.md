# Testcase: Disconnect and Reconnect

Purpose
- Verify graceful handling of disconnects and successful re-discovery/reconnect.

Steps
1. With an active game, host taps back and confirms exit; server stops.
2. Observe client shows disconnected state message.
3. Host reopens `Host Multiplayer`.
4. Client taps `Join Multiplayer` again.

Expected Results
- Client handles disconnect without crashes.
- Re-discovery works; client rejoins and receives state.

Screenshots
- Client disconnected: `tests/screenshots/disconnect_client.png`
- Client rejoined: `tests/screenshots/reconnect_client.png`
# Testcase: Disconnect and Reconnect

Purpose
- Verify graceful handling of disconnects and successful re-discovery/reconnect.

Steps
1. With an active game, host taps back and confirms exit; server stops.
2. Observe client shows disconnected state message (or timer stops and UI indicates disconnection).
3. Host reopens `Host Multiplayer`.
4. Client taps `Join Multiplayer` again and waits for `Joined as #<index>`.

Expected Results
- Client handles disconnect without crashes.
- Re-discovery works; client rejoins and receives state.

Screenshots
- Client disconnected: `tests/screenshots/disconnect_client.png`
- Client rejoined: `tests/screenshots/reconnect_client.png`