# mihon-extension-submanhwa

Two Mihon extensions (separate APKs) for **submanhwa.com**:
- **SubManhwa (ES)** — Spanish
- **SubManhwa (EN)** — English

This repository is structured so GitHub Actions builds both APKs automatically on each push/tag and uploads them as artifacts.

## Modules
- `extensions/es-submanhwa`
- `extensions/en-submanhwa`

## Install in Mihon
1. Download the APK for your language from GitHub Actions artifacts (or Releases).
2. Install on your Android device.
3. Open **Mihon → Browse → Extensions**, the catalog should appear under *Installed*.

## Custom repo (optional)
You can turn this repo into a Mihon catalog by serving `repo/index.min.json` and the APKs under `repo/apk/...`. We include a starter `repo/index.min.json` that you can update after your first Release.

## Build locally
```bash
./gradlew :extensions:es-submanhwa:assembleRelease
./gradlew :extensions:en-submanhwa:assembleRelease
```
APK outputs:
- `extensions/es-submanhwa/build/outputs/apk/release/es-submanhwa-release.apk`
- `extensions/en-submanhwa/build/outputs/apk/release/en-submanhwa-release.apk`
