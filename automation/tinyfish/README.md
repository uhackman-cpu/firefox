# TinyFish-style goal automation for Firefox

This directory is the initial, isolated integration layer for goal-driven browser automation in the `uhackman-cpu/firefox` fork.

## Status

This is a prototype package, not yet wired into Firefox startup or built into a browser binary. The local controller starts only when explicitly launched by the user. The HTTP transport is Node.js-based and is not a Gecko-native Firefox service.

## Features in this branch

- `goal-runner.mjs`: bounded, cancellable goal/task state machine and explicit confirmation states.
- `localhost-server.mjs`: opt-in HTTP control server with strict routes, request limits, loopback checks, origin checks, and a required injected authorization callback.
- `launch.mjs`: local prototype launcher; select a port with `TINYFISH_PORT` and provide a secret with `TINYFISH_TOKEN`.
- `standalone-window/`: a temporary-loadable WebExtension prototype that moves the active tab into a minimal popup window when its toolbar button is clicked.
- `protocol.md` and `security.md`: protocol and threat model.
- `*.test.mjs`: Node test coverage for goal state and HTTP behavior.

## Run the local API prototype

Requires Node.js 22 or later:

```sh
TINYFISH_PORT=8900 TINYFISH_TOKEN="$(openssl rand -hex 32)" node automation/tinyfish/launch.mjs
```

The port can be changed to any available TCP port from 1024 through 65535, for example `TINYFISH_PORT=8795`. The service binds only to `127.0.0.1`; it does not bind to LAN interfaces. Requests must include `Authorization: Bearer <TINYFISH_TOKEN>`.

**Important:** this prototype's goal adapter deliberately reports that no Firefox adapter is installed. It can demonstrate task lifecycle and transport behavior but cannot yet control page tabs or execute browser actions.

## Try the standalone window extension

1. Open `about:debugging#/runtime/this-firefox` in Firefox Desktop.
2. Choose **Load Temporary Add-on…** and select `standalone-window/manifest.json`.
3. Select a tab and click the extension toolbar button to move it into a popup window.

## Tests

Run:

```sh
node --test automation/tinyfish/*.test.mjs
```

A GitHub Actions workflow runs these prototype tests on relevant pull requests and pushes.

## Remaining integration work

This is not a full Firefox build and is not integrated into Firefox startup. A Gecko-native transport, reviewed browser actor/adapter, visible permission/consent UI, test manifest registration, localization, security review, and actual Firefox build remain necessary. The extension is a prototype and has not been packaged, signed, or tested against a built browser binary.
