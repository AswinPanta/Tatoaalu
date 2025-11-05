# Testcase: Pass Flow (Client → Server → Broadcast)

Purpose
- Validate pass action from clients with immediate local feedback and server validation.

Steps
1. During game, on the client that holds the potato, tap to pass.
2. Confirm immediate local pass animation/UI feedback.
3. Verify server logs `Pass requested by client <index>` and broadcast new holder.
4. Confirm all clients update to the new holder.

Expected Results
- Local pass feedback is immediate.
- Server processes pass and broadcasts consistent state.
- No duplicate passes while `passPending` is true.

Screenshots
- Client pass action: `tests/screenshots/pass_client.png`
- Host after pass: `tests/screenshots/pass_host.png`