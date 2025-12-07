package com.appcontrolx.service

import com.appcontrolx.executor.CommandExecutor

class BatteryPolicyManager(private val executor: CommandExecutor) {
    
    fun restrictBackground(packageName: String): Result<Unit> {
        val commands = listOf(
            "appops set $packageName RUN_IN_BACKGROUND ignore",
            "appops set $packageName RUN_ANY_IN_BACKGROUND ignore",
            "appops set $packageName WAKE_LOCK ignore"
        )
        return executor.executeBatch(commands)
    }
    
    fun allowBackground(packageName: String): Result<Unit> {
        val commands = listOf(
            "appops set $packageName RUN_IN_BACKGROUND allow",
            "appops set $packageName RUN_ANY_IN_BACKGROUND allow",
            "appops set $packageName WAKE_LOCK allow"
        )
        return executor.executeBatch(commands)
    }
    
    fun getBackgroundStatus(packageName: String): BackgroundStatus {
        val result = executor.execute("appops get $packageName RUN_IN_BACKGROUND")
        val output = result.getOrDefault("")
        return when {
            output.contains("ignore") -> BackgroundStatus.RESTRICTED
            output.contains("allow") -> BackgroundStatus.ALLOWED
            else -> BackgroundStatus.DEFAULT
        }
    }
    
    fun forceStop(packageName: String): Result<Unit> {
        return executor.execute("am force-stop $packageName").map { }
    }
    
    fun freezeApp(packageName: String): Result<Unit> {
        return executor.execute("pm disable-user --user 0 $packageName").map { }
    }
    
    fun unfreezeApp(packageName: String): Result<Unit> {
        return executor.execute("pm enable $packageName").map { }
    }
    
    fun uninstallApp(packageName: String): Result<Unit> {
        return executor.execute("pm uninstall -k --user 0 $packageName").map { }
    }
}
