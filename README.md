# Quest Environment Picker

A hybrid root and rootless environment picker for Meta Quest, based on xAstroBoy's original root environment manager.

## Features

- Automatic Magisk/`su` root detection
- Direct rooted switching through `oculuspreferences`
- Correct handling of environments, vistas, and footprints
- Installed environment package discovery on rooted devices
- Rootless Shizuku fallback for NoRoot-Spoof Home APKs
- Automatic Home APK discovery in `Download`, `Quest Homes`, `QuestHomes`, and `Homes`
- APK validation through `assets/scene.zip`
- Search by Home name or package name
- Quest-friendly Material 3 landscape UI
- VR Shell reload after direct root switching

## Root mode

Install the `Rooted-System` environment packages normally. The picker discovers them and updates the matching preferences:

- Environment: `environment_selected`, `environment_default`, `resolved_environment`
- Vista: `default_vista`, `resolved_vista`, `environment_vista_selected`
- Footprint: `default_footprint`

Shizuku is not required when root is available.

## Rootless mode

Start Shizuku through Wireless ADB and copy compatible `NoRoot-Spoof` APKs to the Quest `Download` folder or a dedicated Home folder. The picker keeps the APK available and installs the selected Home over `com.meta.shell.env.footprint.haven2025`.

## Build

Java 17 and Android SDK 35 are required.

```bash
./gradlew :app:assembleDebug
```

The debug APK is written to `app/build/outputs/apk/debug/app-debug.apk`.

## Credits

- Original Quest Environment Picker and root preference mapping: [xAstroBoy](https://github.com/xAstroBoy)
- Hybrid Shizuku workflow and Material 3 picker UI contributed by Nikita with Codex assistance

This is an unofficial community project and is not affiliated with Meta, Shizuku, or Quest Home Porter.
