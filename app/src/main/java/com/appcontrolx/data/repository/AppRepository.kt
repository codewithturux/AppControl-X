package com.appcontrolx.data.repository

import com.appcontrolx.model.AppInfo
import com.appcontrolx.service.AppFetcher
import com.appcontrolx.service.BatteryPolicyManager
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject
import javax.inject.Singleton

interface AppRepository {
    fun getAllApps(): Flow<Result<List<AppInfo>>>
    fun getUserApps(): Flow<Result<List<AppInfo>>>
    fun getSystemApps(): Flow<Result<List<AppInfo>>>
    suspend fun freezeApp(packageName: String): Result<Unit>
    suspend fun unfreezeApp(packageName: String): Result<Unit>
    suspend fun uninstallApp(packageName: String): Result<Unit>
    suspend fun forceStopApp(packageName: String): Result<Unit>
    suspend fun restrictBackground(packageName: String): Result<Unit>
    suspend fun allowBackground(packageName: String): Result<Unit>
    suspend fun clearCache(packageName: String): Result<Unit>
    suspend fun clearData(packageName: String): Result<Unit>
}

@Singleton
class AppRepositoryImpl @Inject constructor(
    private val appFetcher: AppFetcher,
    private val policyManager: BatteryPolicyManager?
) : AppRepository {
    
    override fun getAllApps(): Flow<Result<List<AppInfo>>> = flow {
        try {
            val apps = appFetcher.getAllApps()
            emit(Result.success(apps))
        } catch (e: Exception) {
            emit(Result.failure(e))
        }
    }
    
    override fun getUserApps(): Flow<Result<List<AppInfo>>> = flow {
        try {
            val apps = appFetcher.getUserApps()
            emit(Result.success(apps))
        } catch (e: Exception) {
            emit(Result.failure(e))
        }
    }
    
    override fun getSystemApps(): Flow<Result<List<AppInfo>>> = flow {
        try {
            val apps = appFetcher.getSystemApps()
            emit(Result.success(apps))
        } catch (e: Exception) {
            emit(Result.failure(e))
        }
    }
    
    override suspend fun freezeApp(packageName: String): Result<Unit> {
        return policyManager?.freezeApp(packageName) 
            ?: Result.failure(Exception("Policy manager not available"))
    }
    
    override suspend fun unfreezeApp(packageName: String): Result<Unit> {
        return policyManager?.unfreezeApp(packageName)
            ?: Result.failure(Exception("Policy manager not available"))
    }
    
    override suspend fun uninstallApp(packageName: String): Result<Unit> {
        return policyManager?.uninstallApp(packageName)
            ?: Result.failure(Exception("Policy manager not available"))
    }
    
    override suspend fun forceStopApp(packageName: String): Result<Unit> {
        return policyManager?.forceStop(packageName)
            ?: Result.failure(Exception("Policy manager not available"))
    }
    
    override suspend fun restrictBackground(packageName: String): Result<Unit> {
        return policyManager?.restrictBackground(packageName)
            ?: Result.failure(Exception("Policy manager not available"))
    }
    
    override suspend fun allowBackground(packageName: String): Result<Unit> {
        return policyManager?.allowBackground(packageName)
            ?: Result.failure(Exception("Policy manager not available"))
    }
    
    override suspend fun clearCache(packageName: String): Result<Unit> {
        return policyManager?.clearCache(packageName)
            ?: Result.failure(Exception("Policy manager not available"))
    }
    
    override suspend fun clearData(packageName: String): Result<Unit> {
        return policyManager?.clearData(packageName)
            ?: Result.failure(Exception("Policy manager not available"))
    }
}
