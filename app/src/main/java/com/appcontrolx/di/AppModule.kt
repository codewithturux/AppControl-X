package com.appcontrolx.di

import android.content.Context
import com.appcontrolx.data.repository.AppRepository
import com.appcontrolx.data.repository.AppRepositoryImpl
import com.appcontrolx.executor.CommandExecutor
import com.appcontrolx.executor.RootExecutor
import com.appcontrolx.model.ExecutionMode
import com.appcontrolx.service.AppFetcher
import com.appcontrolx.service.BatteryPolicyManager
import com.appcontrolx.service.PermissionBridge
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {
    
    @Provides
    @Singleton
    fun providePermissionBridge(
        @ApplicationContext context: Context
    ): PermissionBridge = PermissionBridge(context)
    
    @Provides
    @Singleton
    fun provideCommandExecutor(
        permissionBridge: PermissionBridge
    ): CommandExecutor? {
        return when (permissionBridge.detectMode()) {
            is ExecutionMode.Root -> RootExecutor()
            else -> null
        }
    }
    
    @Provides
    @Singleton
    fun provideBatteryPolicyManager(
        executor: CommandExecutor?
    ): BatteryPolicyManager? {
        return executor?.let { BatteryPolicyManager(it) }
    }
    
    @Provides
    @Singleton
    fun provideAppFetcher(
        @ApplicationContext context: Context
    ): AppFetcher = AppFetcher(context)
    
    @Provides
    @Singleton
    fun provideAppRepository(
        appFetcher: AppFetcher,
        policyManager: BatteryPolicyManager?
    ): AppRepository = AppRepositoryImpl(appFetcher, policyManager)
}
