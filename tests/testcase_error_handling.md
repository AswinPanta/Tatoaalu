# Testcase: Error Handling (Discovery, Unknown Types)

Purpose
- Validate UI/Logs for discovery failures and unknown message types.

Steps
1. Temporarily disable Wi‑Fi on client; attempt `Join Multiplayer`.
2. Observe discovery error label on client.
3. (Protocol) Send an invalid/unknown type from a test client; observe server/client logs.

Expected Results
- Client displays discovery error when NSD fails.
- Unknown types are logged by server (`Unknown message type`) and client.

Screenshots
- Client discovery error: `tests/screenshots/error_discovery_client.png`