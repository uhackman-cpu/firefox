# TinyFish-style goal automation for Firefox

This directory is the initial, isolated integration layer for goal-driven browser automation in the `uhackman-cpu/firefox` fork.

## Status

This is a starter implementation package, not yet wired into Firefox startup or built into a browser binary. It deliberately does not change existing browser behavior. The local controller must be explicitly enabled by the user before it is started.

## Design

- `goal-runner.mjs`: bounded, cancellable goal/task state machine and action-policy checks.
- `localhost-server.mjs`: opt-in HTTP control server, loopback-only, strict routes and request validation.
- `protocol.md`: proposed endpoints and JSON shapes.
- `security.md`: threat model and required safeguards.

## Intended endpoints

| Method | Path | Purpose |
|---|---|---|
| GET | `/status` | Health and current task metadata |
| GET | `/tabs` | Return a minimal tab summary, excluding private-window tabs by default |
| POST | `/goal` | Validate and enqueue a user-specified goal |
| POST | `/goal/:id/cancel` | Cancel a running goal |
| GET | `/goal/:id` | Read goal status and sanitized result |

This package is intentionally narrower than the supplied Gemini Autonomous Browser interface. It does not expose arbitrary page JavaScript evaluation, arbitrary GitHub commits, token storage, file deletion, or unrestricted network navigation.

## Development and integration

Do not import this module from production startup until the Firefox platform/security review has approved its lifecycle, permission prompts, localization, and test coverage. Run tests with the repository's supported JavaScript test harness after integration into a registered test manifest. The GitHub file API can store code, but cannot perform a full Firefox build or verify browser runtime behavior by itself.
