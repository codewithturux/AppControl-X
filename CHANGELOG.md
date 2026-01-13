# Changelog

All notable changes to AppControlX.

## [2.0.0] - 2026-01 (v2 Rewrite)

### 🎉 Complete Rewrite
This is a complete rewrite of AppControlX with modern architecture and new features.

### Added
- **Dashboard** - System monitoring with real-time updates
  - CPU usage and temperature
  - Battery status and temperature
  - RAM and Storage usage
  - Network status
  - Display info (resolution, refresh rate)
  - GPU info (requires root)
  - Device info with uptime and deep sleep time
- **Setup Wizard** - Guided first-time setup with mode selection
- **Mode Loss Detection** - Automatic detection when Root/Shizuku access is lost
- **Display Refresh Rate Control** - Set min/max refresh rate (Root/Shizuku)
- **Feature Quick Access Cards** - Navigate to features from Dashboard
- **Batch Progress UI** - Visual progress during batch operations

### Changed
- **Architecture** - Complete rewrite with clean MVVM + Hilt DI
- **Package Structure** - New organized structure:
  - `domain/executor/` - Command executors
  - `domain/manager/` - Business logic managers
  - `domain/scanner/` - App scanning
  - `domain/monitor/` - System monitoring
  - `domain/validator/` - Safety validation
  - `data/model/` - Data classes
  - `ui/` - UI components
- **App Detection** - More accurate using dumpsys + PackageManager
- **Material 3** - Updated to latest Material Design 3
- **Navigation** - Bottom navigation with Dashboard, Apps, Settings

### Removed
- Old code moved to `.old` folder (now deleted)
- Legacy architecture patterns

---

## [1.1.0] - 2025-12

### Added
- Showcase website (index.html) with responsive design, 3 themes, image gallery with lightbox
- Expanded background ops viewer in app detail (WAKE_LOCK, START_FOREGROUND, BOOT_COMPLETED, etc.)
- Other Projects backlinks section in website

### Fixed
- App info sheet stacking bug when returning from recent apps
- Duplicate Activity Launcher in Tools layout (removed from Advanced section)
- ProGuard rules for Rollback/ActionLog Gson serialization in release builds
- Removed setupwizard from SafetyValidator restrictions

### Changed
- Autostart Manager now supports 13 OEM brands (Xiaomi, OPPO, Vivo, Huawei, OnePlus, Samsung, ASUS, Sony, Lenovo, ZTE, Meizu, Transsion)
- Activity Launcher moved to Apps section
- Added RUN_ANY_IN_BACKGROUND hint in app detail sheet
- Hero section redesigned with app preview image

---

## [1.0.0] - 2025-12

### Added

#### Core Features
- Setup wizard with mode selection (Root/Shizuku/View-Only)
- Disclaimer screen with risk acknowledgment
- App list with User/System app filter
- Status filter (All/Running/Stopped/Frozen/Restricted)
- Search functionality for apps
- Batch selection and operations with progress tracking

#### App Actions
- Freeze/Unfreeze apps (disable without uninstall)
- Force Stop running apps
- Restrict/Allow Background (both buttons always visible)
- Clear Cache with size preview
- Clear Data
- Uninstall (current user)
- Launch App
- Open System Settings (AOSP App Info)

#### App Detail
- Quick actions grid layout (3 columns)
- Background state display (RUN_IN_BACKGROUND / WAKE_LOCK)
- Real-time status refresh after actions

#### Tools
- Activity Launcher with search, expand/collapse, hold to copy path
- QColor / Display Color (Qualcomm, MediaTek MiraVision, AOSP, Samsung)
- Extra Dim (Android 12+)
- Notification Log
- Notification History
- Battery Optimization
- Power Mode
- Device Info
- Device Diagnostic
- Unknown Sources management
- Manage Apps (AOSP)

#### Action Logs & Rollback
- Action history with timestamps
- Rollback support for battery actions (Freeze/Unfreeze, Restrict/Allow)
- State snapshots before actions
- Works with both Root and Shizuku modes

#### Settings
- Theme selection (System/Light/Dark)
- Language (System/English/Indonesian)
- Mode switching with restart prompt
- Safety settings (Confirm actions, Protect system apps)
- View action logs with count
- Clear snapshots
- Reset setup wizard

#### About Page
- App info with version
- Stats (User apps, System apps, Actions count)
- Device info (Model, Android version)
- Quick links (GitHub, Star, Bug Report, Share)
- Sticky footer with credits

#### Safety & Security
- SafetyValidator blocks critical system apps
- Protected packages: AOSP, Google, Xiaomi/MIUI, Samsung, OPPO, Vivo, Huawei, OnePlus, Nothing, ASUS, Sony, Motorola
- Command whitelist in RootExecutor
- Package name validation

#### Platform Support
- Root mode via libsu
- Shizuku mode with UserService (full features, no root)
- View-Only mode for browsing

#### UI/UX
- Material 3 design
- Dark mode support
- ViewBinding
- Card-based layouts
- Status badges (Running, Stopped, Frozen, Restricted)
- Haptic feedback on selections
- Icons for all tools
- Compact About page with sticky footer

### Changed
- Running detection uses inverse logic (no STOPPED/FROZEN badge = RUNNING)
- Action logs readable without active executor
- Tools redesigned with icons and better organization
- About page made more compact

### Removed
- Timber logging (replaced with android.util.Log)
- Unused presentation/viewmodel folder
- Unused data/repository folder
- Unused worker folder
- Gradient backgrounds
- Emoji in UI hints

### Fixed
- QColorActivity typo (case sensitive)
- Action logs not saving
- Shizuku executor service binding
- Settings log count for all modes
- App list refresh after actions
- Background state parsing from appops
- Running filter sync with badge logic

---

## Architecture (v2)

```
com.appcontrolx/
├── App.kt                    # Application class with @HiltAndroidApp
├── di/                       # Hilt DI modules
│   ├── AppModule.kt
│   └── ExecutorModule.kt
├── domain/
│   ├── executor/             # Command execution
│   │   ├── CommandExecutor.kt
│   │   ├── RootExecutor.kt
│   │   ├── ShizukuExecutor.kt
│   │   └── PermissionBridge.kt
│   ├── manager/              # Business logic
│   │   ├── AppControlManager.kt
│   │   ├── BatteryManager.kt
│   │   ├── ActionLogger.kt
│   │   ├── DisplayManager.kt
│   │   └── ModeWatcher.kt
│   ├── scanner/              # App scanning
│   │   └── AppScanner.kt
│   ├── monitor/              # System monitoring
│   │   └── SystemMonitor.kt
│   └── validator/            # Safety validation
│       └── SafetyValidator.kt
├── data/
│   └── model/                # Data classes
│       ├── AppInfo.kt
│       ├── ExecutionMode.kt
│       ├── SystemInfo.kt
│       └── ActionLog.kt
└── ui/
    ├── MainActivity.kt
    ├── setup/                # Setup wizard
    ├── dashboard/            # Dashboard with system info
    ├── applist/              # App list and detail
    ├── settings/             # Settings
    ├── history/              # Action history
    └── components/           # Reusable UI components
```

## Tech Stack

- Kotlin 1.9
- Min SDK 29 (Android 10)
- Target SDK 34 (Android 14)
- MVVM + Hilt DI
- Material 3 with Dynamic Colors
- Coroutines + Flow
- libsu 5.2.2 (Root)
- Shizuku-API 13.1.5
- Navigation Component
- ViewBinding
