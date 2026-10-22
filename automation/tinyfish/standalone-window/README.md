# Standalone tab window prototype

This is a small, opt-in Firefox WebExtension prototype. Clicking its toolbar
button moves the current tab into a minimal `popup` window. It does not
automatically run at startup and does not inspect or modify the page.

## Load temporarily

1. Open `about:debugging#/runtime/this-firefox` in Firefox Desktop.
2. Choose **Load Temporary Add-on…**.
3. Select this directory's `manifest.json`.
4. Open the tab you want to detach and click the extension's toolbar button.

Temporary add-ons are removed when Firefox restarts. This prototype has not
been packaged, signed, or tested against a built Firefox binary.

## Notes

- It uses the supported `browser.windows.create({type: "popup"})` and
  `browser.tabs.move()` APIs.
- The extension requires `tabs` and `windows` permissions.
- It does not change the browser's global window mode; only the selected tab
  is moved into a separate popup window.
