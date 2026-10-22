# Local goal-control protocol (prototype)

Base URL: `http://127.0.0.1:8900`. The server is opt-in and only listens when explicitly started by an approved integration.

## GET /status

Returns service health and counts by goal state. It does not return page contents or credentials.

## POST /goal

Request:
```json
{ "goal": "Summarize the visible article", "maxSteps": 10 }
```

Constraints: JSON object, goal 1–4000 characters, `maxSteps` integer 1–25. Returns HTTP 202 and an in-memory task record. This prototype does not itself understand natural language; a reviewed adapter must interpret goals and propose bounded steps.

## GET /goals and GET /goal/:id

Return task status, bounded step descriptions, optional sanitized result, and sanitized error information. Tasks currently exist only in server memory.

## POST /goal/:id/tick

Asks the injected adapter to advance a task by one step. The calling controller should poll deliberately and stop on a terminal state. Consequential actions must return `requiresConfirmation` from the adapter; the runner transitions to `waiting_confirmation`.

## POST /goal/:id/confirm

Request: `{ "approved": true }`. Explicit false cancels the task. Approval should only be sent after the browser has displayed the proposed action to the user; approval is not implied by task creation.

## POST /goal/:id/cancel

Requests cancellation. A browser adapter must honor cancellation before every side-effecting step.

## Deliberately omitted

No `/eval`, arbitrary page JavaScript, unrestricted network requests, GitHub token endpoint, direct commit/push endpoint, filesystem endpoint, APK downloading, or shell-command route is provided by this module. Those capabilities carry a separate privileged security boundary and are not appropriate for a generic localhost goal endpoint.
