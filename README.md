# Quest Environment Picker

A root-based tool for managing Meta Quest environments, vistas, and footprints via `oculuspreferences`.

## Features

- **Tabbed UI** — Environments | Vistas | Footprints
- **Scanner** — Detects installed `.environment.`, `.env.vista.`, and `.env.footprint.` APKs
- **Apply** — Sets env keys with one tap per type
- **Set Default** — Sets `environment_default` + `default_footprint` without changing active env
- **As Env** — Apply a footprint as the main environment
- **Set Defaults** — Reverts env, vista, and footprint to stock (haven2025 / central / nuxd fallback)
- **Restart VRS** — Force-stops `com.oculus.vrshell`
- **Uninstall** — Root-based `pm uninstall` with protection for stock packages
- **Protected packages** — `central`, `haven2025`, `nuxd` cannot be uninstalled

## Requirements

- **Root access** (Magisk) — `su` must be available
- **`oculuspreferences`** binary on device
- Meta Quest running v74+

## Keys Managed

| Key | Purpose |
|-----|---------|
| `environment_selected` | Active environment |
| `environment_default` | Fallback environment |
| `resolved_environment` | Resolved environment |
| `default_footprint` | Footprint fallback |
| `default_vista` | Vista fallback |
| `resolved_vista` | Resolved vista |
| `environment_vista_selected` | Active vista |

## Stock Defaults

| Type | URI |
|------|-----|
| Environment | `apk://com.meta.shell.env.footprint.haven2025/assets/scene.zip` |
| Vista | `apk://com.meta.shell.env.vista.central/assets/scene.zip` |
| Footprint | `apk://com.meta.shell.env.footprint.haven2025/assets/scene.zip` |

## Build

```bash
./gradlew assembleDebug
adb install -r app/build/outputs/apk/debug/app-debug.apk
```
