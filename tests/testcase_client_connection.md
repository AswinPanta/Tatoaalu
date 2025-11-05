# Testcase: Client Connection and Welcome

Purpose
- Ensure clients connect to host and receive `welcome` with `yourIndex`.

Steps
1. Host starts lobby.
2. Client joins; wait for connection.
3. Observe client UI for `Joined as #<index>`.

Expected Results
- Client receives `welcome` message with correct index.
- No socket timeouts or disconnect errors.

Screenshots
- Client joined: `tests/screenshots/client_welcome.png`