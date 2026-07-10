# Screenshot Restriction Disabler

An **Xposed/LSPosed module** that disables `FLAG_SECURE` on selected Android apps, allowing screenshots and screen recording on apps that normally block them.

## Features

- ✅ **Master Switch** — Turn the module on/off globally
- 🔍 **App Search** — Search installed apps by name or package
- 📋 **Per-App Toggle** — Selectively enable/disable per app
- 🎛️ **System App Filter** — Show/hide system apps
- 🎨 **Clean Material UI** — Purple-themed, easy to use
- 🔄 **Refresh** — Reload app list anytime
- 🗑️ **Clear Selection** — Reset all selections

## Requirements

- **Rooted** Android device
- **LSPosed** or **Xposed Framework** installed
- Android 7.0+ (API 24+)

## Installation

1. Build the APK (or download the pre-built APK)
2. Install the APK on your device
3. Open **LSPosed Manager**
4. Go to **Modules** → Enable **Screenshot Restriction Disabler**
5. In the module's **Scope**, select the apps you want to remove screenshot restrictions from
6. Open the **Screenshot Restriction Disabler** app
7. Turn on the **Master Switch**
8. Toggle the apps you want to affect
9. **Force stop / restart** the target apps for changes to take effect

## How It Works

The module hooks `Window.setFlags()` and `Window.addFlags()` in the target app's process. When the target app tries to set `FLAG_SECURE` (which blocks screenshots), the hook strips that flag, allowing screenshots and screen recording.

It also hooks `Surface.setSecure()` for apps that use the hidden API directly.

### Key Files

| File | Purpose |
|------|---------|
| `SecureFlagHook.java` | Xposed hook — intercepts FLAG_SECURE calls in target apps |
| `MainActivity.java` | UI — app list with toggles, master switch, search |
| `AppPreferences.java` | SharedPreferences manager (WORLD_READABLE for cross-process access) |
| `AppListAdapter.java` | ListView adapter with filterable app list |
| `AppLoader.java` | Async loader for installed apps |
| `AppInfo.java` | Data model for app entries |

## Build

```bash
# Open in Android Studio and build, or:
./gradlew assembleRelease
```

The APK will be at `app/build/outputs/apk/release/app-release.apk`

## Disclaimer

This module is for educational purposes and personal use only. Some apps use FLAG_SECURE for security reasons (banking, DRM content, private chats). Use responsibly and respect app developers' intentions.

## License

MIT License — Free to use, modify, and distribute.
