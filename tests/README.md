# TatoAalu Multiplayer Tests

This folder contains manual testcases to validate the multiplayer system (NSD, LAN server/client, lobby, state sync, pass flow, error handling, and cleanup).

Device Setup
- Use two Android phones on the same Wi‑Fi network.
- Install the app on both devices.
- Disable VPNs and battery savers that may block multicast/NSD.

Recommended Order
1. `testcase_service_registration.md`
2. `testcase_nsd_discovery.md`
3. `testcase_lobby_matchmaking.md`
4. `testcase_client_connection.md`
5. `testcase_state_sync.md`
6. `testcase_pass_flow.md`
7. `testcase_disconnect_reconnect.md`
8. `testcase_error_handling.md`
9. `testcase_resource_cleanup.md`
10. `testcase_leaderboard.md` (optional)

Capturing Screenshots (ADB)
- List devices: `adb devices`
- Capture host: `adb -s <serialA> exec-out screencap -p > tests/screenshots/<name>_host.png`
- Capture client: `adb -s <serialB> exec-out screencap -p > tests/screenshots/<name>_client.png`

Useful Log Filters
- `TAG_GAME`, `TAG_SERVER`, `TAG_CLIENT`, `TAG_NSD`
- Example: `adb logcat | grep TAG_SERVER`

Notes
- NSD requires multicast; ensure Wi‑Fi AP supports it.
- If discovery errors occur, retry or toggle Wi‑Fi.