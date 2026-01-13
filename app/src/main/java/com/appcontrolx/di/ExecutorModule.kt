package com.appcontrolx.di

import android.content.Context
import com.appcontrolx.data.model.ExecutionMode
import com.appcontrolx.domain.executor.CommandExecutor
import com.appcontrolx.domain.executor.PermissionBridge
import com.appcontrolx.domain.executor.RootExecutor
import com.appcontrolx.domain.executor.ShizukuExecutor
import com.appcontrolx.domain.manager.ActionLogger
import com.appcontrolx.domain.manager.AnimationScaleManager
import com.appcontrolx.domain.manager.AnimationScaleManagerImpl
import com.appcontrolx.domain.manager.DisplayManager
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Hilt module providing executor-related dependencies.
 * 
 * This module provides CommandExecutor implementations (Root, Shizuku)
 * and PermissionBridge for mode detection.
 * 
 * Requirements: 1.1-1.6, 11.1-11.4
 */
@Module
@InstallIn(SingletonComponent::class)
object ExecutorModule {
    
    /**
     * Provides PermissionBridge for execution mode detection and management.
     * Requirements: 1.1, 1.2, 1.3, 1.4, 1.5, 1.6
     */
    @Provides
    @Singleton
    fun providePermissionBridge(
        @ApplicationContext context: Context
    ): PermissionBridge = PermissionBridge(context)
    
    /**
     * Provides RootExecutor for root shell command execution.
     * Requirements: 11.1, 11.2, 11.3
     */
    @Provides
    @Singleton
    fun provideRootExecutor(): RootExecutor = RootExecutor()
    
    /**
     * Provides ShizukuExecutor for Shizuku-based command execution.
     * Requirements: 11.4
     */
    @Provides
    @Singleton
    fun provideShizukuExecutor(): ShizukuExecutor = ShizukuExecutor()
    
    /**
     * Provides CommandExecutor based on current execution mode.
     * Uses RootExecutor by default, falls back to ShizukuExecutor if root not available.
     */
    @Provides
    @Singleton
    fun provideCommandExecutor(
        rootExecutor: RootExecutor,
        shizukuExecutor: ShizukuExecutor,
        permissionBridge: PermissionBridge
    ): CommandExecutor {
        // Default to RootExecutor, the actual mode selection happens at runtime
        return if (rootExecutor.isAvailable()) {
            rootExecutor
        } else if (shizukuExecutor.isAvailable()) {
            shizukuExecutor
        } else {
            rootExecutor // Return root executor even if not available, will fail gracefully
        }
    }
    
    /**
     * Provides DisplayManager for refresh rate control.
     * Requirements: 10.5, 10.6, 10.7, 10.8, 10.9
     */
    @Provides
    @Singleton
    fun provideDisplayManager(
        @ApplicationContext context: Context,
        commandExecutor: CommandExecutor
    ): DisplayManager = DisplayManager(context, commandExecutor)
    
    /**
     * Provides ActionLogger for action history and rollback.
     * Requirements: 7.1, 7.2, 7.3, 7.4, 7.5, 7.6
     */
    @Provides
    @Singleton
    fun provideActionLogger(
        @ApplicationContext context: Context,
        commandExecutor: CommandExecutor
    ): ActionLogger = ActionLogger(context, commandExecutor)
    
    /**
     * Provides AnimationScaleManager for animation scale control.
     * Requirements: 7.4
     */
    @Provides
    @Singleton
    fun provideAnimationScaleManager(
        commandExecutor: CommandExecutor
    ): AnimationScaleManager = AnimationScaleManagerImpl(commandExecutor)
}
