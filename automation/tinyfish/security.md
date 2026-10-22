# Security boundaries

The automation service is a prototype and is not wired into Firefox startup.

## Mandatory before production use

1. **Explicit opt-in and visible state.** Never start a listener by default. Browser chrome must show a clear running indicator, provide a stop control, and use a dedicated user preference.
2. **Loopback only.** Bind only to `127.0.0.1`; reject non-loopback Host headers and cross-origin browser requests. Do not expose the service on `0.0.0.0`, LAN addresses, or IPv6 wildcard interfaces.
3. **Origin/CSRF protections.** Host and Origin checks are a minimum, not authentication. The integrated Gecko transport must also apply an unguessable per-session capability or equivalent local authorization and validate request provenance. Browsers and extensions must not be able to issue privileged requests implicitly.
4. **No secrets in the protocol.** Do not provide a route to upload or persist GitHub tokens, passwords, cookies, or local file contents. Keep credentials in platform credential storage where needed.
5. **Confirmation for consequential actions.** Submitting forms, purchases, posting content, changing account settings, deleting files, downloading/executing code, and committing/pushing changes require a visible review and explicit user approval. A natural-language goal is not sufficient approval.
6. **Least privilege.** Keep page interaction behind a narrow, reviewed adapter. Do not expose arbitrary JavaScript execution, raw CDP, shell access, or unrestricted URL fetches.
7. **Resource limits.** Enforce body size, goal length, step count, navigation limits, deadlines, concurrency bounds, and cancellation. Redact sensitive page contents from logs.
8. **Safe tab handling.** Exclude private browsing contexts unless the user explicitly opts in per task; do not silently read credentials or private form values.
9. **Testing.** Add unit tests and Gecko integration tests for request validation, cross-origin rejection, cancellation, prompt injection in page text, user confirmation, private windows, and listener shutdown.
10. **Review.** Have Firefox security and browser-platform owners review the implementation before shipping or enabling it by default.

The prototype's Host/Origin checks alone are not a security boundary against local malware and must not be described as authentication.
