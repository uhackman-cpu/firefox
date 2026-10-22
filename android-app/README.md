# TinyFish Browser for Android

Native Android browser prototype with Mozilla GeckoView, the Gecko rendering engine used by Firefox. The app UI currently includes address/search, back/forward/reload, and multiple GeckoSession-backed tabs.

## Engine

This app uses `org.mozilla.geckoview:geckoview`; it does **not** use Android System WebView. GeckoView is Mozilla's embeddable Gecko engine. See the [official GeckoView quick start](https://mozilla.github.io/geckoview/consumer/docs/geckoview-quick-start).

## Build

Pushes to `automation/tinyfish-goal-api` that change `android-app/**` or its APK workflow trigger GitHub Actions. Download the `tinyfish-geckoview-debug-apk` artifact from a successful **TinyFish Android APK** workflow run.

Minimum Android version: Android 8.0 (API 26).

## Current limitations

This is a GeckoView-based browser shell, not a full fork/build of Firefox for Android. It does not yet include Firefox Sync, add-ons UI, tracking protection settings, desktop mode, or the TinyFish goal automation integration inside the Android app. APK availability depends on the GeckoView build completing successfully in CI.
