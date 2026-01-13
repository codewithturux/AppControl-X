package com.appcontrolx.di

import android.content.Context
import com.appcontrolx.domain.monitor.SystemMonitor
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Hilt module providing app-level dependencies.
 * 
 * This module provides core application dependencies that are shared
 * across the entire application lifecycle.
 */
@Module
@InstallIn(SingletonComponent::class)
object AppModule {
    
    /**
     * Provides the application context for dependencies that need it.
     */
    @Provides
    @Singleton
    fun provideApplicationContext(
        @ApplicationContext context: Context
    ): Context = context
    
    /**
     * Provides the SystemMonitor for real-time device monitoring.
     * Requirements: 0.1.1-0.1.9, 0.2.1-0.2.7
     */
    @Provides
    @Singleton
    fun provideSystemMonitor(
        @ApplicationContext context: Context
    ): SystemMonitor = SystemMonitor(context)
}
