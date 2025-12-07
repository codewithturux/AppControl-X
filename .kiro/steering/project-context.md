# AppControlX Project Context

## Project Overview
AppControlX adalah Android app untuk mengontrol aplikasi menggunakan Root atau Shizuku. App ini bisa freeze/unfreeze apps, manage battery optimization, dan batch operations.

## Tech Stack
- **Language**: Kotlin 1.9
- **Min SDK**: 29 (Android 10)
- **Target SDK**: 34 (Android 14)
- **Architecture**: MVVM + Repository Pattern
- **DI**: Hilt 2.50
- **Database**: Room 2.6
- **Preferences**: DataStore
- **Async**: Coroutines + Flow
- **Root**: libsu by topjohnwu
- **Shizuku**: Shizuku-API by RikkaApps
- **Logging**: Timber
- **Crash Reporting**: Firebase Crashlytics
- **CI/CD**: GitHub Actions

## Project Structure
```
app/src/main/java/com/appcontrolx/
├── data/
│   ├── local/          # Room Database, DataStore
│   └── repository/     # Data repositories
├── di/                 # Hilt modules (AppModule, DatabaseModule)
├── executor/           # Command execution (RootExecutor, ShizukuExecutor)
├── model/              # Data classes (AppInfo, ExecutionMode)
├── presentation/
│   └── viewmodel/      # ViewModels
├── rollback/           # State management & rollback
├── service/            # Business logic (AppFetcher, BatteryPolicyManager, PermissionBridge)
├── ui/                 # Activities, Fragments, Adapters
├── utils/              # Helpers (Constants, SafetyValidator)
└── worker/             # WorkManager workers
```

## Key Files
- `App.kt` - Application class dengan Hilt, Timber, Crashlytics init
- `PermissionBridge.kt` - Deteksi execution mode (Root/Shizuku/None)
- `RootExecutor.kt` - Execute shell commands dengan security validation
- `BatteryPolicyManager.kt` - Manage app freeze, background restriction
- `SafetyValidator.kt` - Protect critical system apps
- `AppRepository.kt` - Central data access layer
- `AppListViewModel.kt` - UI state management

## Coding Standards
1. Gunakan Kotlin idioms (scope functions, null safety, etc.)
2. Follow MVVM pattern - logic di ViewModel, UI di Fragment
3. Inject dependencies via Hilt, jangan manual instantiation
4. Gunakan Flow untuk reactive data
5. Log dengan Timber, bukan Log.d
6. Handle errors dengan Result<T>
7. Validate package names sebelum execute commands
8. Protect critical system apps (SafetyValidator)

## Security Rules
- NEVER execute arbitrary shell commands
- ALWAYS validate package names dengan regex
- ALWAYS check SafetyValidator sebelum actions
- Block dangerous commands (rm -rf, reboot, format, etc.)
- Use command whitelist di RootExecutor

## UI Guidelines
- Material 3 design
- ViewBinding untuk views
- Card-based layouts
- Status badges untuk app states
- Haptic feedback untuk selections
- Accessibility support (content descriptions)

## Testing
- Unit tests di `app/src/test/`
- Use Mockito untuk mocking
- Use Turbine untuk Flow testing
- Target 50%+ code coverage

## Build Commands
```bash
./gradlew assembleDebug      # Debug build
./gradlew assembleRelease    # Release build (minified)
./gradlew test               # Run unit tests
./gradlew lint               # Run lint checks
```

## Common Issues
1. Root access gagal → Check Shell.getShell() dipanggil dulu
2. Type mismatch Result<String> vs Result<Unit> → Use .map { }
3. NPE di Fragment → Use safe binding access pattern
4. Mode tidak persist → Check Constants.PREFS_EXECUTION_MODE

## GitHub Repository
https://github.com/risunCode/AppControl-X
