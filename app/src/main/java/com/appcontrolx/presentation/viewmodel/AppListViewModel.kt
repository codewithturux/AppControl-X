package com.appcontrolx.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.appcontrolx.data.repository.AppRepository
import com.appcontrolx.model.AppInfo
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import android.util.Log
import javax.inject.Inject

sealed class AppListUiState {
    object Loading : AppListUiState()
    data class Success(val apps: List<AppInfo>) : AppListUiState()
    data class Error(val message: String) : AppListUiState()
}

@HiltViewModel
class AppListViewModel @Inject constructor(
    private val repository: AppRepository
) : ViewModel() {
    
    private val _uiState = MutableStateFlow<AppListUiState>(AppListUiState.Loading)
    val uiState: StateFlow<AppListUiState> = _uiState.asStateFlow()
    
    private val _selectedApps = MutableStateFlow<Set<String>>(emptySet())
    val selectedApps: StateFlow<Set<String>> = _selectedApps.asStateFlow()
    
    fun loadUserApps() {
        viewModelScope.launch {
            _uiState.value = AppListUiState.Loading
            repository.getUserApps().collect { result ->
                result.fold(
                    onSuccess = { apps ->
                        _uiState.value = AppListUiState.Success(apps)
                        Log.d(TAG, "Loaded ${apps.size} user apps")
                    },
                    onFailure = { error ->
                        _uiState.value = AppListUiState.Error(error.message ?: "Unknown error")
                        Log.e(TAG, "Failed to load user apps", error)
                    }
                )
            }
        }
    }
    
    fun loadSystemApps() {
        viewModelScope.launch {
            _uiState.value = AppListUiState.Loading
            repository.getSystemApps().collect { result ->
                result.fold(
                    onSuccess = { apps ->
                        _uiState.value = AppListUiState.Success(apps)
                        Log.d(TAG, "Loaded ${apps.size} system apps")
                    },
                    onFailure = { error ->
                        _uiState.value = AppListUiState.Error(error.message ?: "Unknown error")
                        Log.e(TAG, "Failed to load system apps", error)
                    }
                )
            }
        }
    }
    
    fun toggleSelection(packageName: String) {
        val current = _selectedApps.value.toMutableSet()
        if (current.contains(packageName)) {
            current.remove(packageName)
        } else {
            current.add(packageName)
        }
        _selectedApps.value = current
    }
    
    fun selectAll(apps: List<AppInfo>) {
        _selectedApps.value = apps.map { it.packageName }.toSet()
    }
    
    fun deselectAll() {
        _selectedApps.value = emptySet()
    }
    
    fun freezeApp(packageName: String, onComplete: (Result<Unit>) -> Unit) {
        viewModelScope.launch {
            val result = repository.freezeApp(packageName)
            onComplete(result)
            if (result.isSuccess) {
                Log.d(TAG, "App frozen: $packageName")
            } else {
                Log.e(TAG, "Failed to freeze app: $packageName")
            }
        }
    }
    
    fun unfreezeApp(packageName: String, onComplete: (Result<Unit>) -> Unit) {
        viewModelScope.launch {
            val result = repository.unfreezeApp(packageName)
            onComplete(result)
            if (result.isSuccess) {
                Log.d(TAG, "App unfrozen: $packageName")
            } else {
                Log.e(TAG, "Failed to unfreeze app: $packageName")
            }
        }
    }
    
    companion object {
        private const val TAG = "AppListViewModel"
    }
}
