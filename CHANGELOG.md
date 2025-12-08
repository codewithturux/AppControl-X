# Changelog

All notable changes to AppControlX.

## [1.0.1] - 2024-12

### Fixed
- App info sheet stacking bug when returning from recent apps
- Duplicate Activity Launcher in Tools layout (removed from Advanced section)
- ProGuard rules for Rollback/ActionLog Gson serialization in release builds
- Removed setupwizard from SafetyValidator restrictions

### Changed
- Autostart Manager now supports 13 OEM brands (Xiaomi, OPPO, Vivo, Huawei, OnePlus, Samsung, ASUS, Sony, Lenovo, ZTE, Meizu, Transsion)
- Activity Launcher moved to Apps section
- Added RUN_ANY_IN_BACKGROUND hint in app detail sheet

---

## [1.0.0] - 2024-12

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

## Architecture

```
com.appcontrolx/
├── data/local/         # Room Database (DAO, Entity)
├── di/                 # Hilt modules
├── executor/           # RootExecutor, ShizukuExecutor
├── model/              # AppInfo, ExecutionMode
├── rollback/           # RollbackManager, ActionLog
├── service/            # AppFetcher, BatteryPolicyManager, PermissionBridge
├── ui/                 # Activities, Fragments, Adapters, BottomSheets
└── utils/              # Constants, SafetyValidator
```

## Tech Stack

- Kotlin 1.9
- Min SDK 29 (Android 10)
- Target SDK 34 (Android 14)
- MVVM + Hilt DI
- Material 3
- Coroutines + Flow
- libsu (Root)
- Shizuku-API
