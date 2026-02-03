package com.documents.app.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.documents.app.data.api.NetworkResult
import com.documents.app.data.preferences.SettingsPreferences
import com.documents.app.data.preferences.ThemeMode
import com.documents.app.data.repository.DocumentRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SettingsUiState(
    val apiBaseUrl: String = "",
    val tenantId: String = "",
    val apiKey: String = "",
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val speechPauseDuration: Long = 3000L,
    val statusMessage: String? = null,
    val isCheckingStatus: Boolean = false,
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settingsPreferences: SettingsPreferences,
    private val repository: DocumentRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            settingsPreferences.apiBaseUrl.collect { url ->
                _uiState.value = _uiState.value.copy(apiBaseUrl = url)
            }
        }
        viewModelScope.launch {
            settingsPreferences.tenantId.collect { id ->
                _uiState.value = _uiState.value.copy(tenantId = id)
            }
        }
        viewModelScope.launch {
            settingsPreferences.apiKey.collect { key ->
                _uiState.value = _uiState.value.copy(apiKey = key)
            }
        }
        viewModelScope.launch {
            settingsPreferences.themeMode.collect { mode ->
                _uiState.value = _uiState.value.copy(themeMode = mode)
            }
        }
        viewModelScope.launch {
            settingsPreferences.speechPauseDuration.collect { duration ->
                _uiState.value = _uiState.value.copy(speechPauseDuration = duration)
            }
        }
    }

    fun setApiBaseUrl(url: String) {
        viewModelScope.launch {
            settingsPreferences.setApiBaseUrl(url)
        }
    }

    fun setTenantId(id: String) {
        viewModelScope.launch {
            settingsPreferences.setTenantId(id)
        }
    }

    fun setApiKey(key: String) {
        viewModelScope.launch {
            settingsPreferences.setApiKey(key)
        }
    }

    fun setThemeMode(mode: ThemeMode) {
        viewModelScope.launch {
            settingsPreferences.setThemeMode(mode)
        }
    }

    fun setSpeechPauseDuration(ms: Long) {
        viewModelScope.launch {
            settingsPreferences.setSpeechPauseDuration(ms)
        }
    }

    fun checkStatus() {
        _uiState.value = _uiState.value.copy(isCheckingStatus = true, statusMessage = null)

        viewModelScope.launch {
            when (val result = repository.checkStatus()) {
                is NetworkResult.Success -> {
                    _uiState.value = _uiState.value.copy(
                        isCheckingStatus = false,
                        statusMessage = "Connected successfully"
                    )
                }
                is NetworkResult.Error -> {
                    _uiState.value = _uiState.value.copy(
                        isCheckingStatus = false,
                        statusMessage = "Connection failed: ${result.message}"
                    )
                }
                is NetworkResult.Loading -> {}
            }
        }
    }
}
