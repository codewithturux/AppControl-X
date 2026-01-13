package com.appcontrolx.domain.validator

import com.appcontrolx.data.model.AppAction

/**
 * Validates packages against safety rules to prevent accidental damage to critical system apps.
 * Also validates package names for format correctness and injection attempts.
 */
object SafetyValidator {
    
    // Apps yang TIDAK BOLEH disentuh sama sekali
    // Source: AOSP, OEM docs, community lists (github.com/AuroraOSS/AppWarden, etc)
    private val CRITICAL_PACKAGES = setOf(
        // Self-protection
        "com.appcontrolx",
        
        // === AOSP Core System ===
        "android",
        "com.android.systemui",
        "com.android.settings",
        "com.android.phone",
        "com.android.server.telecom",
        "com.android.providers.settings",
        "com.android.providers.contacts",
        "com.android.providers.telephony",
        "com.android.providers.media",
        "com.android.providers.media.module",
        "com.android.providers.downloads",
        "com.android.providers.calendar",
        "com.android.inputmethod.latin",
        "com.android.launcher3",
        "com.android.packageinstaller",
        "com.android.permissioncontroller",
        "com.android.shell",
        "com.android.se",
        "com.android.nfc",
        "com.android.bluetooth",
        "com.android.wifi",
        "com.android.networkstack",
        "com.android.networkstack.tethering",
        "com.android.captiveportallogin",
        "com.android.localtransport",
        "com.android.location.fused",
        "com.android.keychain",
        "com.android.certinstaller",
        "com.android.webview",
        "com.android.dynsystem",
        "com.android.ons",
        "com.android.sdm.plugins.connmo",
        "com.android.emergency",
        "com.android.incallui",
        "com.android.stk",
        "com.android.cellbroadcastreceiver",
        
        // === Google Core Services ===
        "com.google.android.gms",
        "com.google.android.gsf",
        "com.google.android.gsf.login",
        "com.android.vending",
        "com.google.android.packageinstaller",
        "com.google.android.ext.services",
        "com.google.android.ext.shared",
        "com.google.android.onetimeinitializer",
        "com.google.android.partnersetup",
        "com.google.android.configupdater",
        "com.google.android.providers.media.module",
        "com.google.android.webview",
        "com.google.android.trichromelibrary",
        "com.google.android.overlay.modules.permissioncontroller",

        // === Xiaomi/MIUI/HyperOS Core (DO NOT UNINSTALL OR DISABLE) ===
        "com.miui.system",
        "com.miui.rom",
        "com.miui.core",
        "com.miui.securitycore",
        "com.miui.systemAdSolution",
        "com.xiaomi.xmsf",
        "com.xiaomi.simactivate.service",
        "com.xiaomi.joyose",
        "com.xiaomi.mi_connect_service",
        "com.lbe.security.miui",           // Permission Manager Service
        "com.android.updater",             // System Updater (Xiaomi signed)
        "com.miui.securitycenter",         // Security Center (Xiaomi signed)
        "com.xiaomi.finddevice",           // Find Device (Xiaomi signed)
        "com.miui.home",                   // System Launcher
        "com.miui.guardprovider",          // MIUI Security Component
        "com.xiaomi.market",               // App Store (Xiaomi signed)
        "com.xiaomi.account",              // Xiaomi Account
        "com.miui.packageinstaller",       // Package Installer
        
        // === MIUI Auto-enable packages (DISABLE NOT WORKING) ===
        "com.miui.contentcatcher",
        "com.android.printspooler",
        "com.miui.audiomonitor",
        "com.miui.voicetrigger",
        "com.xiaomi.mircs",
        "com.miui.daemon",
        "com.xiaomi.xmsfkeeper",
        
        // === Samsung Core ===
        "com.samsung.android.providers.context",
        "com.samsung.android.providers.contacts",
        "com.samsung.android.incallui",
        "com.samsung.android.telecom",
        "com.samsung.android.app.telephonyui",
        "com.samsung.android.server.wifi.softap.resources",
        "com.samsung.android.networkstack",
        "com.samsung.android.wifi.resources",
        "com.sec.android.app.launcher",
        "com.sec.android.provider.badge",
        "com.sec.android.inputmethod",
        "com.sec.android.app.samsungapps",  // Galaxy Store
        "com.samsung.android.messaging",
        "com.samsung.android.dialer",
        "com.samsung.android.oneconnect",   // SmartThings
        "com.samsung.android.samsungpass",
        "com.samsung.android.authfw",
        "com.samsung.android.knox.containercore",
        
        // === OPPO/ColorOS/Realme Core ===
        "com.coloros.providers.downloads",
        "com.coloros.systemui",
        "com.coloros.phonemanager",
        "com.coloros.launcher",
        "com.oplus.battery",
        "com.oplus.athena",
        "com.oplus.safecenter",
        "com.heytap.market",                // OPPO/Realme App Store
        "com.realme.findphone",
        "com.oppo.launcher",
        "com.oppo.market",

        // === OnePlus/OxygenOS Core ===
        "com.oneplus.config",
        "net.oneplus.provider.appsettings",
        "com.oneplus.launcher",
        "net.oneplus.launcher",
        "com.oneplus.account",
        
        // === Huawei/HarmonyOS Core ===
        "com.huawei.android.launcher",
        "com.huawei.appmarket",
        "com.huawei.hwid",                  // Huawei ID
        "com.huawei.hms.core",
        "com.huawei.android.hsf",
        "com.huawei.systemserver",
        "com.huawei.android.pushagent",
        
        // === Vivo/FuntouchOS/OriginOS Core ===
        "com.bbk.launcher2",
        "com.vivo.appstore",
        "com.vivo.daemonService",
        "com.vivo.assistant",
        "com.vivo.vivokaraoke",
        
        // === Nothing Phone ===
        "com.nothing.launcher",
        "com.nothing.systemui",
        
        // === ASUS ROG/ZenUI ===
        "com.asus.launcher",
        "com.asus.dm",
        
        // === Sony Xperia ===
        "com.sonymobile.home",
        
        // === Motorola ===
        "com.motorola.launcher3",
        
        // === Root/Shizuku/Tools ===
        "com.topjohnwu.magisk",
        "io.github.vvb2060.magisk",
        "rikka.shizuku",
        "moe.shizuku.privileged.api",
        "eu.chainfire.supersu",
        "me.weishu.kernelsu"
    )
    
    // Apps yang HANYA BOLEH di-force stop (tidak boleh freeze/uninstall/disable)
    // These are security/system apps that can cause issues if disabled
    private val FORCE_STOP_ONLY_PACKAGES = setOf(
        // === Xiaomi/MIUI/HyperOS (CAN DISABLE, NOT RECOMMENDED) ===
        "com.miui.powerkeeper",            // Battery & Performance
        "com.xiaomi.metoknlp",             // Network Location Service
        "com.miui.tsmclient",              // Xiaomi Smart Card
        "com.miui.accessibility",          // Accessibility Service (TTS)
        "com.miui.backup",                 // Backup & Restore
        "com.miui.freeform",               // Freeform Window
        "com.miui.face",                   // Face Recognition
        "com.miui.miwallpaper",            // Desktop Wallpaper
        "com.miui.aod",                    // Always On Display
        "com.miui.securityadd",
        "com.miui.antispam",
        "com.miui.analytics",
        "com.miui.notification",
        "com.miui.hybrid",
        "com.miui.hybrid.accessory",
        "com.miui.mishare.connectivity",
        "com.miui.voiceassist",
        "com.miui.personalassistant",

        // === Samsung Security & Optimization ===
        "com.samsung.android.lool",
        "com.samsung.android.sm",
        "com.samsung.android.sm.devicesecurity",
        "com.samsung.android.sm.policy",
        "com.samsung.android.forest",
        "com.samsung.android.app.smartcapture",
        "com.samsung.android.fmm",
        "com.samsung.android.bixby.agent",
        "com.samsung.android.visionintelligence",
        
        // === OPPO/ColorOS/Realme Security ===
        "com.coloros.safecenter",
        "com.oppo.safe",
        "com.coloros.oppoguardelf",
        "com.coloros.phonemanager",
        "com.heytap.cloud",
        "com.heytap.openid",
        
        // === OnePlus ===
        "com.oneplus.security",
        
        // === Huawei/HarmonyOS ===
        "com.huawei.systemmanager",
        "com.huawei.powergenie",
        "com.huawei.hicloud",
        "com.huawei.iconnect",
        "com.huawei.intelligent",
        
        // === Vivo/FuntouchOS ===
        "com.vivo.permissionmanager",
        "com.iqoo.secure",
        "com.vivo.browser",
        "com.vivo.weather",
        
        // === Realme ===
        "com.realme.security",
        
        // === Nothing Phone ===
        "com.nothing.smartcenter",
        
        // === Google Find My Device ===
        "com.google.android.apps.adm"
    )
    
    // Apps yang perlu warning sebelum action
    private val WARNING_PACKAGES = setOf(
        "com.google.android.apps.messaging",
        "com.google.android.dialer",
        "com.google.android.contacts"
    )
    
    // Regex pattern for valid Android package names
    // Package names must start with a letter, contain only letters, digits, underscores, and dots
    // Each segment must start with a letter
    private val PACKAGE_NAME_PATTERN = Regex("^[a-zA-Z][a-zA-Z0-9_]*(\\.[a-zA-Z][a-zA-Z0-9_]*)+$")
    
    // Characters that could be used for shell injection
    private val INJECTION_CHARACTERS = setOf(';', '&', '|', '`', '$', '\'', '"', '\n', '\r', '\\', '(', ')', '<', '>', '{', '}', '[', ']', ' ')

    /**
     * Validates a list of packages and categorizes them into blocked, warnings, and safe.
     * 
     * @param packages List of package names to validate
     * @return ValidationResult with categorized packages and whether action can proceed
     */
    fun validate(packages: List<String>): ValidationResult {
        val blocked = packages.filter { it in CRITICAL_PACKAGES }
        val warnings = packages.filter { it in WARNING_PACKAGES && it !in CRITICAL_PACKAGES }
        val safe = packages.filter { it !in CRITICAL_PACKAGES && it !in WARNING_PACKAGES }
        
        return ValidationResult(
            blocked = blocked,
            warnings = warnings,
            safe = safe,
            canProceed = blocked.isEmpty()
        )
    }
    
    /**
     * Checks if a package is in the critical list and cannot be modified.
     */
    fun isCritical(packageName: String): Boolean = packageName in CRITICAL_PACKAGES
    
    /**
     * Checks if a package is in the warning list.
     */
    fun isWarning(packageName: String): Boolean = packageName in WARNING_PACKAGES
    
    /**
     * Checks if a package can only be force-stopped (no freeze/uninstall/disable).
     * This is for security apps that shouldn't be disabled but can be temporarily stopped.
     */
    fun isForceStopOnly(packageName: String): Boolean = packageName in FORCE_STOP_ONLY_PACKAGES
    
    /**
     * Gets the set of allowed actions for a package based on its safety category.
     * 
     * @param packageName The package to check
     * @return Set of allowed AppAction values, empty if package is critical
     */
    fun getAllowedActions(packageName: String): Set<AppAction> {
        return when {
            isCritical(packageName) -> emptySet()
            isForceStopOnly(packageName) -> setOf(AppAction.FORCE_STOP)
            else -> AppAction.entries.toSet()
        }
    }
    
    /**
     * Validates that a package name follows the Android package name format.
     * 
     * Valid package names:
     * - Start with a letter
     * - Contain only letters, digits, underscores, and dots
     * - Have at least two segments separated by dots
     * - Each segment starts with a letter
     * 
     * @param packageName The package name to validate
     * @return true if the package name format is valid
     */
    fun validatePackageNameFormat(packageName: String): Boolean {
        if (packageName.isBlank()) return false
        if (packageName.length > 255) return false // Android limit
        return PACKAGE_NAME_PATTERN.matches(packageName)
    }
    
    /**
     * Checks if a package name contains characters that could be used for shell injection.
     * 
     * @param packageName The package name to check
     * @return true if injection attempt is detected (contains dangerous characters)
     */
    fun checkInjectionAttempt(packageName: String): Boolean {
        return packageName.any { it in INJECTION_CHARACTERS }
    }
    
    /**
     * Performs complete validation of a package name including format and injection checks.
     * 
     * @param packageName The package name to validate
     * @return Result.success(Unit) if valid, Result.failure with appropriate exception if invalid
     */
    fun validatePackageName(packageName: String): Result<Unit> {
        return when {
            checkInjectionAttempt(packageName) -> 
                Result.failure(SecurityException("Injection attempt detected in package name: $packageName"))
            !validatePackageNameFormat(packageName) -> 
                Result.failure(IllegalArgumentException("Invalid package name format: $packageName"))
            else -> Result.success(Unit)
        }
    }
}

/**
 * Result of validating a list of packages.
 */
data class ValidationResult(
    val blocked: List<String>,
    val warnings: List<String>,
    val safe: List<String>,
    val canProceed: Boolean
)
