# AppControlX

A powerful Android application for controlling app behavior, battery optimization, and system management — using Root or Shizuku.

## Features

### App Control
- **Freeze/Unfreeze** - Disable apps without uninstalling (keeps data intact)
- **Uninstall** - Remove apps for current user while preserving data
- **Force Stop** - Immediately terminate running applications
- **Clear Cache/Data** - Clean app storage with size preview
- **Batch Operations** - Apply actions to multiple apps with progress tracking

### Battery Optimization
- **Restrict Background** - Block apps from running in background
- **Allow Background** - Permit background execution
- **Real-time Status** - View current background restriction status per app

### Tools
- **Activity Launcher** - Launch hidden activities from any app (hold to copy path)
- **QColor / Display Color** - Qualcomm QColor, MediaTek MiraVision, AOSP, Samsung
- **Extra Dim** - Reduce screen brightness below minimum (Android 12+)
- **Notification Log/History** - Access notification records
- **Battery Optimization** - View app battery consumption
- **Power Mode** - Quick access to power settings
- **Device Info & Diagnostic** - System information
- **Unknown Sources** - Control app install permissions
- **Manage Apps (AOSP)** - Pure AOSP app manager

### Action Logs & Rollback
- **Action History** - Track all operations with timestamps
- **Rollback** - Reverse battery actions (Freeze/Unfreeze, Restrict/Allow)
- **State Snapshots** - Automatic backup before actions

### UI/UX
- **Material 3 Design** - Modern, clean interface with icons
- **Dark Mode** - Full dark theme support
- **Multi-language** - English & Indonesian
- **Search & Filter** - Quick app discovery by name, package, or status

## Screenshots

| Setup | Main | App Info | Batch | Activity Launcher |
|:-----:|:----:|:--------:|:-----:|:-----------------:|
| ![Setup 1](https://github.com/user-attachments/assets/b54ea7eb-d2cb-452e-8914-435f0126fd1c) | ![Main Apps](https://github.com/user-attachments/assets/2f45871b-f2b0-4b1b-aa01-433516caa608) | ![App Info](https://github.com/user-attachments/assets/1085fad6-011e-4ab6-9e48-a7a1310319da) | ![Batch](https://github.com/user-attachments/assets/88001520-721a-436a-9dfc-c372ebe03790) | ![Activity Launcher](https://github.com/user-attachments/assets/1e634bb6-4d75-4072-87f8-ead4d4eac504) |

| Tools | Settings | About | Blocklist |
|:-----:|:--------:|:-----:|:---------:|
| ![Tools](https://github.com/user-attachments/assets/cc4cac8c-cc98-417c-b46c-c338aa017383) | ![Settings](https://github.com/user-attachments/assets/1bd61725-a9e1-40d3-8725-6b709cf0abdf) | ![About](https://github.com/user-attachments/assets/c5074985-cb74-4097-b051-6df73d5ce89e) | ![Blocklist](https://github.com/user-attachments/assets/71e4b960-6ce5-4805-8f44-e86d53eb675a) |

## Platform Support

| Platform | Version | Support |
|----------|---------|---------|
| Android Stock | 10 - 15 | Full |
| MIUI/HyperOS | 12+ | Full |
| Samsung OneUI | 3+ | Full |
| ColorOS/Realme | 11+ | Full |
| OxygenOS | 11+ | Full |
| Custom ROM | Android 10+ | Full |

### Protected System Apps
SafetyValidator blocks critical system packages from being disabled/frozen to prevent bricking. Covers AOSP, Google, Xiaomi, Samsung, OPPO, Vivo, Huawei, OnePlus, Nothing, ASUS, Sony, Motorola, and more.

## Requirements

- Android 10+ (API 29)
- One of the following:
  - **Root access** (Magisk recommended)
  - **Shizuku** installed and activated (full features, no root needed)

## Installation

### From Release
1. Download the latest APK from [Releases](https://github.com/risunCode/AppControl-X/releases)
2. Install on your device
3. Complete the setup wizard

### Build from Source
```bash
git clone https://github.com/risunCode/AppControl-X.git
cd AppControl-X
./gradlew assembleDebug
```

## Architecture

MVVM architecture with Hilt dependency injection.

```
com.appcontrolx/
├── data/local/         # Room Database
├── di/                 # Hilt modules
├── executor/           # Command execution (Root/Shizuku)
├── model/              # Data classes
├── rollback/           # Action logs & rollback
├── service/            # Business logic
├── ui/                 # Activities, Fragments, Adapters
└── utils/              # Helpers & validators
```

### Key Components

| Component | Description |
|-----------|-------------|
| `PermissionBridge` | Detects execution mode (Root/Shizuku/None) |
| `RootExecutor` | Executes commands via libsu with security validation |
| `ShizukuExecutor` | Executes commands via Shizuku UserService |
| `BatteryPolicyManager` | Manages appops and battery settings |
| `RollbackManager` | Action logs and state snapshots |
| `SafetyValidator` | Prevents actions on critical system apps |

## Tech Stack

- **Language**: Kotlin 1.9
- **Min SDK**: 29 (Android 10)
- **Target SDK**: 34 (Android 14)
- **Architecture**: MVVM + Hilt
- **UI**: Material 3, ViewBinding
- **Async**: Coroutines + Flow
- **Root**: [libsu](https://github.com/topjohnwu/libsu)
- **Shizuku**: [Shizuku-API](https://github.com/RikkaApps/Shizuku-API)

## Commands Reference

### App Control
```bash
pm disable-user --user 0 <package>    # Freeze
pm enable <package>                    # Unfreeze
pm uninstall -k --user 0 <package>    # Uninstall
am force-stop <package>                # Force stop
pm clear --cache-only <package>        # Clear cache
pm clear <package>                     # Clear data
```

### Battery Control
```bash
appops set <package> RUN_IN_BACKGROUND ignore    # Restrict
appops set <package> RUN_IN_BACKGROUND allow     # Allow
appops set <package> WAKE_LOCK ignore            # Disable wake lock
```

## Changelog

See [CHANGELOG.md](CHANGELOG.md) for full history.

## Contributing

1. Fork the repository
2. Create a feature branch
3. Commit your changes
4. Push and open a Pull Request

## License

GPL-3.0 License - see [LICENSE](LICENSE)

## Credits

Made with ❤️ by [risunCode](https://github.com/risunCode)

### Acknowledgments

- [libsu](https://github.com/topjohnwu/libsu) - Root shell library
- [Shizuku](https://github.com/RikkaApps/Shizuku) - Elevated API access
