package com.appcontrolx.domain.monitor

import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.wifi.WifiManager
import android.os.BatteryManager
import android.os.Build
import android.os.Environment
import android.os.StatFs
import android.os.SystemClock
import android.view.WindowManager
import com.appcontrolx.data.model.*
import com.appcontrolx.domain.executor.CommandExecutor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.coroutineContext

/**
 * System monitor for collecting real-time device information.
 * 
 * Provides CPU, battery, RAM, storage, network, display, GPU, and device info.
 * Supports real-time monitoring via Flow for dashboard updates.
 * 
 * Requirements: 0.1.1-0.1.9, 0.2.1-0.2.7
 */
@Singleton
class SystemMonitor @Inject constructor(
    private val context: Context
) {
    
    private var executor: CommandExecutor? = null
    
    // CPU state for calculating usage delta
    private var lastCpuTotal: Long = 0
    private var lastCpuIdle: Long = 0
    
    // Monitoring state
    @Volatile
    private var _isMonitoring = false
    
    /**
     * Whether real-time monitoring is currently active.
     */
    val isMonitoring: Boolean
        get() = _isMonitoring
    
    /**
     * Set the command executor for root-only operations.
     * Call this when execution mode is determined.
     */
    fun setExecutor(executor: CommandExecutor?) {
        this.executor = executor
    }
    
    /**
     * Stop the real-time monitoring.
     * This sets a flag that will cause the monitoring Flow to complete.
     * The collector should also cancel its coroutine scope for immediate effect.
     */
    fun stopMonitoring() {
        _isMonitoring = false
    }
    
    /**
     * Reset CPU usage calculation state.
     * Call this when starting fresh monitoring.
     */
    fun resetCpuState() {
        lastCpuTotal = 0
        lastCpuIdle = 0
    }
    
    // ==================== CPU Info ====================
    
    /**
     * Get current CPU usage percentage by reading /proc/stat.
     * Calculates delta between two readings for accurate usage.
     * 
     * @return CPU usage as percentage (0-100)
     */
    suspend fun getCpuUsage(): Float = withContext(Dispatchers.IO) {
        try {
            val statFile = File("/proc/stat")
            if (!statFile.exists()) return@withContext 0f
            
            val line = statFile.readLines().firstOrNull { it.startsWith("cpu ") }
                ?: return@withContext 0f
            
            val parts = line.split("\\s+".toRegex()).drop(1).take(7)
            if (parts.size < 4) return@withContext 0f
            
            val user = parts[0].toLongOrNull() ?: 0L
            val nice = parts[1].toLongOrNull() ?: 0L
            val system = parts[2].toLongOrNull() ?: 0L
            val idle = parts[3].toLongOrNull() ?: 0L
            val iowait = parts.getOrNull(4)?.toLongOrNull() ?: 0L
            val irq = parts.getOrNull(5)?.toLongOrNull() ?: 0L
            val softirq = parts.getOrNull(6)?.toLongOrNull() ?: 0L
            
            val total = user + nice + system + idle + iowait + irq + softirq
            val idleTime = idle + iowait
            
            if (lastCpuTotal == 0L) {
                // First reading, store values and return 0
                lastCpuTotal = total
                lastCpuIdle = idleTime
                return@withContext 0f
            }
            
            val totalDelta = total - lastCpuTotal
            val idleDelta = idleTime - lastCpuIdle
            
            lastCpuTotal = total
            lastCpuIdle = idleTime
            
            if (totalDelta <= 0) return@withContext 0f
            
            val usage = ((totalDelta - idleDelta).toFloat() / totalDelta) * 100f
            usage.coerceIn(0f, 100f)
        } catch (e: Exception) {
            0f
        }
    }

    
    /**
     * Get CPU temperature from thermal zones.
     * Tries multiple thermal zone paths to find CPU temperature.
     * 
     * @return Temperature in Celsius, or null if unavailable
     */
    suspend fun getCpuTemperature(): Float? = withContext(Dispatchers.IO) {
        try {
            // Common thermal zone paths for CPU temperature
            val thermalPaths = listOf(
                "/sys/class/thermal/thermal_zone0/temp",
                "/sys/class/thermal/thermal_zone1/temp",
                "/sys/devices/virtual/thermal/thermal_zone0/temp",
                "/sys/devices/system/cpu/cpu0/cpufreq/cpu_temp",
                "/sys/class/hwmon/hwmon0/temp1_input"
            )
            
            for (path in thermalPaths) {
                val file = File(path)
                if (file.exists() && file.canRead()) {
                    val temp = file.readText().trim().toLongOrNull()
                    if (temp != null && temp > 0) {
                        // Temperature is usually in millidegrees, convert to Celsius
                        val celsius = if (temp > 1000) temp / 1000f else temp.toFloat()
                        // Sanity check: CPU temp should be between 0 and 150°C
                        if (celsius in 0f..150f) {
                            return@withContext celsius
                        }
                    }
                }
            }
            null
        } catch (e: Exception) {
            null
        }
    }
    
    /**
     * Get number of CPU cores.
     * 
     * @return Number of CPU cores
     */
    fun getCpuCores(): Int {
        return Runtime.getRuntime().availableProcessors()
    }
    
    /**
     * Get current frequency for each CPU core in MHz.
     * Reads from /sys/devices/system/cpu/cpuX/cpufreq/scaling_cur_freq.
     * 
     * @return List of frequencies in MHz for each core, empty list if unavailable
     */
    suspend fun getCoreFrequencies(): List<Long> = withContext(Dispatchers.IO) {
        val frequencies = mutableListOf<Long>()
        val cpuDir = File("/sys/devices/system/cpu/")
        
        var coreIndex = 0
        while (true) {
            val freqFile = File(cpuDir, "cpu$coreIndex/cpufreq/scaling_cur_freq")
            if (!freqFile.exists()) break
            
            try {
                if (freqFile.canRead()) {
                    val freqKhz = freqFile.readText().trim().toLongOrNull() ?: 0L
                    // Convert kHz to MHz
                    frequencies.add(freqKhz / 1000)
                } else {
                    frequencies.add(0L)
                }
            } catch (e: Exception) {
                frequencies.add(0L)
            }
            coreIndex++
        }
        
        frequencies
    }
    
    /**
     * Get complete CPU info including usage, temperature, core count, and frequencies.
     * 
     * @return CpuInfo data class
     */
    suspend fun getCpuInfo(): CpuInfo {
        return CpuInfo(
            usagePercent = getCpuUsage(),
            temperature = getCpuTemperature(),
            cores = getCpuCores(),
            coreFrequencies = getCoreFrequencies()
        )
    }
    
    // ==================== Battery Info ====================
    
    /**
     * Get battery information from BatteryManager.
     * 
     * @return BatteryInfo data class
     */
    fun getBatteryInfo(): BatteryInfo {
        val batteryIntent = context.registerReceiver(
            null,
            IntentFilter(Intent.ACTION_BATTERY_CHANGED)
        )
        
        val level = batteryIntent?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
        val scale = batteryIntent?.getIntExtra(BatteryManager.EXTRA_SCALE, 100) ?: 100
        val percent = if (scale > 0) (level * 100) / scale else 0
        
        val status = batteryIntent?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) ?: -1
        val isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
                status == BatteryManager.BATTERY_STATUS_FULL
        
        // Temperature is in tenths of a degree Celsius
        val tempRaw = batteryIntent?.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, 0) ?: 0
        val temperature = tempRaw / 10f
        
        val healthInt = batteryIntent?.getIntExtra(BatteryManager.EXTRA_HEALTH, -1) ?: -1
        val health = when (healthInt) {
            BatteryManager.BATTERY_HEALTH_GOOD -> "Good"
            BatteryManager.BATTERY_HEALTH_OVERHEAT -> "Overheat"
            BatteryManager.BATTERY_HEALTH_DEAD -> "Dead"
            BatteryManager.BATTERY_HEALTH_OVER_VOLTAGE -> "Over Voltage"
            BatteryManager.BATTERY_HEALTH_COLD -> "Cold"
            else -> "Unknown"
        }
        
        val technology = batteryIntent?.getStringExtra(BatteryManager.EXTRA_TECHNOLOGY) ?: "Unknown"
        
        return BatteryInfo(
            percent = percent,
            isCharging = isCharging,
            temperature = temperature,
            health = health,
            technology = technology
        )
    }
    
    // ==================== RAM Info ====================
    
    /**
     * Get RAM memory information from ActivityManager.
     * 
     * @return RamInfo data class
     */
    fun getRamInfo(): RamInfo {
        val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val memInfo = ActivityManager.MemoryInfo()
        activityManager.getMemoryInfo(memInfo)
        
        val totalBytes = memInfo.totalMem
        val availableBytes = memInfo.availMem
        val usedBytes = totalBytes - availableBytes
        
        return RamInfo(
            usedBytes = usedBytes,
            freeBytes = availableBytes,
            totalBytes = totalBytes
        )
    }

    
    // ==================== Storage Info ====================
    
    /**
     * Get internal storage information using StatFs.
     * 
     * @return StorageInfo data class
     */
    fun getStorageInfo(): StorageInfo {
        val path = Environment.getDataDirectory()
        val stat = StatFs(path.path)
        
        val blockSize = stat.blockSizeLong
        val totalBlocks = stat.blockCountLong
        val availableBlocks = stat.availableBlocksLong
        
        val totalBytes = totalBlocks * blockSize
        val availableBytes = availableBlocks * blockSize
        val usedBytes = totalBytes - availableBytes
        
        return StorageInfo(
            usedBytes = usedBytes,
            totalBytes = totalBytes
        )
    }
    
    // ==================== Network Info ====================
    
    /**
     * Get network connection information.
     * 
     * @return NetworkInfo data class
     */
    @Suppress("DEPRECATION")
    fun getNetworkInfo(): NetworkInfo {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        
        val network = connectivityManager.activeNetwork
        val capabilities = network?.let { connectivityManager.getNetworkCapabilities(it) }
        
        if (capabilities == null) {
            return NetworkInfo(
                type = NetworkType.NONE,
                isConnected = false,
                signalStrength = null,
                signalPercent = null,
                signalDbm = null,
                ssid = null
            )
        }
        
        val isConnected = capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
        
        val type = when {
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> NetworkType.WIFI
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> NetworkType.MOBILE
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> NetworkType.ETHERNET
            else -> NetworkType.NONE
        }
        
        var ssid: String? = null
        var signalStrength: Int? = null
        var signalPercent: Int? = null
        var signalDbm: Int? = null
        
        if (type == NetworkType.WIFI) {
            try {
                val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
                val wifiInfo = wifiManager.connectionInfo
                ssid = wifiInfo.ssid?.replace("\"", "")?.takeIf { it != "<unknown ssid>" }
                
                // Get RSSI (signal strength in dBm)
                val rssi = wifiInfo.rssi
                signalDbm = rssi
                
                // Convert RSSI to signal bars (0-4)
                signalStrength = WifiManager.calculateSignalLevel(rssi, 5)
                
                // Convert RSSI to percentage (typical range: -100 dBm to -30 dBm)
                // -30 dBm = 100%, -100 dBm = 0%
                signalPercent = ((rssi + 100) * 100 / 70).coerceIn(0, 100)
            } catch (e: Exception) {
                // WiFi info not available
            }
        }
        
        return NetworkInfo(
            type = type,
            isConnected = isConnected,
            signalStrength = signalStrength,
            signalPercent = signalPercent,
            signalDbm = signalDbm,
            ssid = ssid
        )
    }
    
    // ==================== Display Info ====================
    
    /**
     * Get display information including resolution and refresh rate.
     * 
     * @return DisplayInfo data class
     */
    @Suppress("DEPRECATION")
    fun getDisplayInfo(): DisplayInfo {
        val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
        val display = windowManager.defaultDisplay
        
        val metrics = context.resources.displayMetrics
        
        val refreshRate = display.refreshRate
        
        return DisplayInfo(
            widthPx = metrics.widthPixels,
            heightPx = metrics.heightPixels,
            refreshRate = refreshRate,
            density = metrics.densityDpi
        )
    }
    
    // ==================== GPU Info ====================
    
    /**
     * Get GPU information via getprop (requires root for some properties).
     * 
     * @return GpuInfo or null if unavailable
     */
    suspend fun getGpuInfo(): GpuInfo? = withContext(Dispatchers.IO) {
        try {
            // Try to get GPU info from system properties
            val gpuProps = listOf(
                "ro.hardware.egl",
                "ro.hardware.vulkan",
                "ro.board.platform",
                "ro.hardware"
            )
            
            var model: String? = null
            var vendor: String? = null
            
            // Try using root executor if available
            executor?.let { exec ->
                for (prop in gpuProps) {
                    val result = exec.execute("getprop $prop")
                    result.getOrNull()?.trim()?.takeIf { it.isNotEmpty() }?.let {
                        if (model == null) model = it
                    }
                }
                
                // Try to get vendor
                exec.execute("getprop ro.hardware.chipname").getOrNull()?.trim()?.let {
                    if (it.isNotEmpty()) vendor = it
                }
            }
            
            // Fallback: try reading from /proc/cpuinfo for hardware info
            if (model == null) {
                try {
                    val cpuInfo = File("/proc/cpuinfo").readText()
                    val hardwareLine = cpuInfo.lines().find { it.startsWith("Hardware") }
                    model = hardwareLine?.substringAfter(":")?.trim()
                } catch (e: Exception) {
                    // Ignore
                }
            }
            
            // Use Build.HARDWARE as last resort
            if (model == null) {
                model = Build.HARDWARE
            }
            
            model?.let {
                GpuInfo(model = it, vendor = vendor)
            }
        } catch (e: Exception) {
            null
        }
    }

    
    // ==================== Device Info ====================
    
    /**
     * Get device uptime since last boot.
     * 
     * @return Uptime in milliseconds
     */
    fun getUptime(): Long {
        return SystemClock.elapsedRealtime()
    }
    
    /**
     * Get deep sleep time (requires root).
     * Deep sleep = uptime - awake time
     * 
     * @return Deep sleep time in milliseconds, or null if unavailable
     */
    suspend fun getDeepSleepTime(): Long? = withContext(Dispatchers.IO) {
        try {
            // Deep sleep = elapsed realtime - uptime (CPU active time)
            val elapsedRealtime = SystemClock.elapsedRealtime()
            val uptimeMs = SystemClock.uptimeMillis()
            
            // The difference is the time spent in deep sleep
            val deepSleep = elapsedRealtime - uptimeMs
            
            if (deepSleep >= 0) deepSleep else null
        } catch (e: Exception) {
            null
        }
    }
    
    /**
     * Get kernel version from /proc/version.
     * 
     * @return Kernel version string
     */
    private fun getKernelVersion(): String {
        return try {
            val version = File("/proc/version").readText().trim()
            // Extract just the version number (e.g., "5.4.147-android12-9-00001")
            val regex = """Linux version (\S+)""".toRegex()
            regex.find(version)?.groupValues?.get(1) ?: version.take(50)
        } catch (e: Exception) {
            System.getProperty("os.version") ?: "Unknown"
        }
    }
    
    /**
     * Get complete device information.
     * 
     * @return DeviceInfo data class
     */
    suspend fun getDeviceInfo(): DeviceInfo {
        return DeviceInfo(
            brand = Build.BRAND.replaceFirstChar { it.uppercase() },
            model = Build.MODEL,
            device = Build.DEVICE,
            androidVersion = Build.VERSION.RELEASE,
            androidCodename = getAndroidCodename(Build.VERSION.SDK_INT),
            apiLevel = Build.VERSION.SDK_INT,
            buildNumber = Build.DISPLAY,
            kernelVersion = getKernelVersion(),
            socName = getSocName(),
            uptimeMs = getUptime(),
            deepSleepMs = getDeepSleepTime()
        )
    }
    
    /**
     * Get Android version codename based on API level.
     */
    private fun getAndroidCodename(apiLevel: Int): String? {
        return when (apiLevel) {
            36 -> "Baklava"
            35 -> "VanillaIceCream"
            34 -> "UpsideDownCake"
            33 -> "Tiramisu"
            32 -> "Sv2"
            31 -> "S"
            30 -> "R"
            29 -> "Q"
            28 -> "Pie"
            27, 26 -> "Oreo"
            25, 24 -> "Nougat"
            23 -> "Marshmallow"
            22, 21 -> "Lollipop"
            20, 19 -> "KitKat"
            else -> null
        }
    }
    
    /**
     * Get SoC/processor name from system properties.
     */
    private fun getSocName(): String? {
        return try {
            // Try to get SoC name from various system properties
            val socProps = listOf(
                "ro.soc.model",
                "ro.hardware.chipname",
                "ro.board.platform"
            )
            
            for (prop in socProps) {
                val value = getSystemProperty(prop)
                if (!value.isNullOrBlank()) {
                    return value
                }
            }
            
            // Fallback to Build.HARDWARE
            Build.HARDWARE.takeIf { it.isNotBlank() && it != "unknown" }
        } catch (e: Exception) {
            null
        }
    }
    
    /**
     * Get system property value using reflection.
     */
    @Suppress("PrivateApi")
    private fun getSystemProperty(key: String): String? {
        return try {
            val systemProperties = Class.forName("android.os.SystemProperties")
            val get = systemProperties.getMethod("get", String::class.java)
            val value = get.invoke(null, key) as? String
            value?.takeIf { it.isNotBlank() }
        } catch (e: Exception) {
            null
        }
    }
    
    // ==================== App Counts ====================
    
    /**
     * Get count of installed user and system apps.
     * 
     * @return AppCounts data class
     */
    fun getAppCounts(): AppCounts {
        val packageManager = context.packageManager
        val packages = packageManager.getInstalledApplications(PackageManager.GET_META_DATA)
        
        var userApps = 0
        var systemApps = 0
        
        for (app in packages) {
            if ((app.flags and ApplicationInfo.FLAG_SYSTEM) != 0) {
                systemApps++
            } else {
                userApps++
            }
        }
        
        return AppCounts(
            userApps = userApps,
            systemApps = systemApps
        )
    }
    
    // ==================== Real-time Monitoring ====================
    
    /**
     * Start real-time system monitoring.
     * Emits SystemSnapshot at specified interval.
     * Call stopMonitoring() to stop the flow.
     * 
     * @param intervalMs Update interval in milliseconds (default 2000ms)
     * @return Flow of SystemSnapshot
     * 
     * Requirements: 0.1.9
     */
    fun startMonitoring(intervalMs: Long = 2000): Flow<SystemSnapshot> = flow {
        _isMonitoring = true
        resetCpuState()
        
        // Initial CPU reading to establish baseline
        getCpuUsage()
        delay(100) // Small delay for accurate first reading
        
        while (_isMonitoring && coroutineContext.isActive) {
            val snapshot = SystemSnapshot(
                cpu = getCpuInfo(),
                battery = getBatteryInfo(),
                ram = getRamInfo(),
                storage = getStorageInfo(),
                network = getNetworkInfo(),
                timestamp = System.currentTimeMillis()
            )
            emit(snapshot)
            delay(intervalMs)
        }
        
        _isMonitoring = false
    }.flowOn(Dispatchers.IO)
    
    /**
     * Get a single system snapshot.
     * Useful for one-time readings.
     * 
     * @return SystemSnapshot
     */
    suspend fun getSnapshot(): SystemSnapshot {
        return SystemSnapshot(
            cpu = getCpuInfo(),
            battery = getBatteryInfo(),
            ram = getRamInfo(),
            storage = getStorageInfo(),
            network = getNetworkInfo(),
            timestamp = System.currentTimeMillis()
        )
    }
}
