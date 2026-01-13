package com.appcontrolx.data.model

/**
 * CPU information including usage and temperature.
 */
data class CpuInfo(
    val usagePercent: Float,
    val temperature: Float?,  // Celsius, null if unavailable
    val cores: Int
)

/**
 * Battery information including charge level and health.
 */
data class BatteryInfo(
    val percent: Int,
    val isCharging: Boolean,
    val temperature: Float,   // Celsius
    val health: String,
    val technology: String
)

/**
 * RAM memory information.
 */
data class RamInfo(
    val usedBytes: Long,
    val freeBytes: Long,
    val totalBytes: Long
) {
    val usedPercent: Float
        get() = if (totalBytes > 0) (usedBytes.toFloat() / totalBytes) * 100 else 0f
}

/**
 * Internal storage information.
 */
data class StorageInfo(
    val usedBytes: Long,
    val totalBytes: Long
) {
    val freeBytes: Long
        get() = totalBytes - usedBytes
    
    val usedPercent: Float
        get() = if (totalBytes > 0) (usedBytes.toFloat() / totalBytes) * 100 else 0f
}


/**
 * Network connection type.
 */
enum class NetworkType {
    WIFI,
    MOBILE,
    ETHERNET,
    NONE
}

/**
 * Network connection information.
 */
data class NetworkInfo(
    val type: NetworkType,
    val isConnected: Boolean,
    val signalStrength: Int?,  // 0-4 bars, null if unavailable
    val ssid: String?          // WiFi name, null if not WiFi
)

/**
 * Display information including resolution and refresh rate.
 */
data class DisplayInfo(
    val widthPx: Int,
    val heightPx: Int,
    val refreshRate: Float,    // Hz
    val density: Int           // DPI
) {
    val resolution: String
        get() = "${widthPx}x${heightPx}"
}

/**
 * GPU information (requires root to retrieve).
 */
data class GpuInfo(
    val model: String,
    val vendor: String?
)

/**
 * Device hardware and software information.
 */
data class DeviceInfo(
    val brand: String,
    val model: String,
    val device: String,
    val androidVersion: String,
    val apiLevel: Int,
    val buildNumber: String,
    val kernelVersion: String,
    val uptimeMs: Long,
    val deepSleepMs: Long?     // requires root
)

/**
 * Count of installed applications.
 */
data class AppCounts(
    val userApps: Int,
    val systemApps: Int
) {
    val total: Int
        get() = userApps + systemApps
}

/**
 * Snapshot of system state at a point in time.
 * Used for real-time dashboard updates.
 */
data class SystemSnapshot(
    val cpu: CpuInfo,
    val battery: BatteryInfo,
    val ram: RamInfo,
    val storage: StorageInfo,
    val network: NetworkInfo,
    val timestamp: Long
)
