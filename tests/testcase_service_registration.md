# Testcase: Service Registration (Host Mode)

Purpose
- Verify host registers its service (via `NsdHelper.registerService`) so clients can discover it.

Prerequisites
- Same as NSD Discovery testcase.

Steps
1. On Phone A, tap `Host Multiplayer`.
2. Observe lobby appears. Wait 3–5 seconds for NSD registration.
3. On Phone B, tap `Join Multiplayer`.
4. Confirm Phone B resolves a service (no discovery error), then connects.
5. If possible, observe logs filtered by tag `TAG_NSD` to confirm registration/resolution events.

Expected Results
- Host registers service without error.
- Client resolves a service and connects.

Screenshots
- Host lobby: `tests/screenshots/service_reg_host.png`
- Client discovery: `tests/screenshots/service_reg_client.png`