# Testcase: Resource Cleanup (onPause/onDestroy)

Purpose
- Ensure `GameActivity` properly stops NSD, server, client, handlers, and releases multicast lock.

Steps
1. Start a host game.
2. Background the app (Home button) and then return.
3. Open logs filtered by `TAG_GAME`, `TAG_NSD`, `TAG_SERVER`, `TAG_CLIENT`.
4. Exit game via back; confirm resources cleaned.

Expected Results
- `onPause` stops discovery/client as appropriate.
- `onDestroy` releases NSD, server, client, multicast lock without leaks.

Screenshots
- None required; attach log excerpts if possible.