# AppControlX ProGuard Rules

# Keep attributes for debugging
-keepattributes *Annotation*
-keepattributes SourceFile,LineNumberTable
-keep public class * extends java.lang.Exception

# Keep Shizuku (critical for release builds)
-keep class rikka.shizuku.** { *; }
-keep class moe.shizuku.** { *; }
-keepclassmembers class * implements android.os.IInterface {
    public *;
}

# Keep libsu
-keep class com.topjohnwu.superuser.** { *; }

# Keep AIDL interfaces
-keep class com.appcontrolx.IShellService { *; }
-keep class com.appcontrolx.IShellService$* { *; }
-keep class * extends android.os.Binder { *; }

# Keep Shizuku UserService - NEW PACKAGE STRUCTURE
-keep class com.appcontrolx.domain.executor.ShellService { *; }
-keep class com.appcontrolx.domain.executor.ShizukuExecutor { *; }
-keep class com.appcontrolx.domain.executor.ShizukuExecutor$* { *; }

# Keep all executors - NEW PACKAGE STRUCTURE
-keep class com.appcontrolx.domain.executor.** { *; }

# Keep all managers
-keep class com.appcontrolx.domain.manager.** { *; }

# Keep all validators
-keep class com.appcontrolx.domain.validator.** { *; }

# Keep all scanners
-keep class com.appcontrolx.domain.scanner.** { *; }

# Keep all monitors
-keep class com.appcontrolx.domain.monitor.** { *; }

# Keep Data Models - NEW PACKAGE STRUCTURE
-keep class com.appcontrolx.data.model.** { *; }
-keep class com.appcontrolx.data.preferences.** { *; }
-keep class com.appcontrolx.data.repository.** { *; }

# Keep UI State classes
-keep class com.appcontrolx.ui.applist.AppListUiState { *; }
-keep class com.appcontrolx.ui.dashboard.DashboardUiState { *; }

# Keep ViewModels
-keep class * extends androidx.lifecycle.ViewModel {
    <init>();
}
-keep class * extends androidx.lifecycle.AndroidViewModel {
    <init>(android.app.Application);
}

# Hilt
-dontwarn com.google.errorprone.annotations.**
-keep class dagger.hilt.** { *; }
-keep class javax.inject.** { *; }
-keep class * extends dagger.hilt.android.internal.managers.ViewComponentManager$FragmentContextWrapper { *; }

# Room
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class *
-dontwarn androidx.room.paging.**

# Gson
-keepattributes Signature
-keepattributes *Annotation*
-dontwarn sun.misc.**
-keep class com.google.gson.** { *; }
-keep class * implements com.google.gson.TypeAdapter
-keep class * implements com.google.gson.TypeAdapterFactory
-keep class * implements com.google.gson.JsonSerializer
-keep class * implements com.google.gson.JsonDeserializer

# Coroutines
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}
-keepclassmembernames class kotlinx.** {
    volatile <fields>;
}

# Remove logging in release
-assumenosideeffects class android.util.Log {
    public static *** d(...);
    public static *** v(...);
    public static *** i(...);
}
