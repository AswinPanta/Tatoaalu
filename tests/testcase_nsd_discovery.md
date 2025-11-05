# Testcase: NSD Discovery (Client Mode)

Purpose
- Verify that clients discover the host game service via Network Service Discovery (NSD) and connect successfully.

Prerequisites
- Two Android phones on the same Wi‑Fi network.
- App installed on both devices.
- Ensure Wi‑Fi is enabled and stable.

Setup
- Phone A = Host device.
- Phone B = Client device.

Steps
1. On Phone A, open the app and tap `Host Multiplayer`.
2. Confirm the lobby UI appears: countdown text, player status, and `Start Game` button disabled.
3. On Phone B, open the app and tap `Join Multiplayer`.
4. Observe discovery status on Phone B. When a service is resolved, it should automatically connect.
5. Verify Phone B UI shows `Joined as #<index>`.

Expected Results
- Phone B discovers the service and connects.
- `Joined as #<index>` appears on Phone B.
- No discovery error toast/label.

Screenshots
- Host lobby: `tests/screenshots/nsd_host_lobby.png`
- Client joined: `tests/screenshots/nsd_client_joined.png`

Optional: Capture via ADB
- List devices: `adb devices`
- Capture on host: `adb -s <serialA> exec-out screencap -p > tests/screenshots/nsd_host_lobby.png`
- Capture on client: `adb -s <serialB> exec-out screencap -p > tests/screenshots/nsd_client_joined.png`