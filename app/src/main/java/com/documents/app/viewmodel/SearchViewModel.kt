package com.documents.app.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.documents.app.data.api.NetworkResult
import com.documents.app.data.model.AggregationResult
import com.documents.app.data.model.SearchResult
import com.documents.app.data.preferences.SettingsPreferences
import com.documents.app.data.repository.DocumentRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SearchUiState(
    val query: String = "",
    val results: List<SearchResult> = emptyList(),
    val totalResults: Int = 0,
    val isSearching: Boolean = false,
    val isListening: Boolean = false,
    val hasSearched: Boolean = false,
    val voiceError: String? = null,
    val error: String? = null,
    // Aggregations from search response
    val aggregations: Map<String, AggregationResult> = emptyMap(),
    // Selected filters
    val selectedCompany: String? = null,
    val selectedHolder: String? = null,
    val selectedDocumentType: String? = null,
    val selectedTaxType: String? = null,
    val selectedYear: String? = null,
    val selectedFileType: String? = null,
)

@HiltViewModel
class SearchViewModel @Inject constructor(
    private val repository: DocumentRepository,
    private val settingsPreferences: SettingsPreferences,
) : ViewModel() {

    private val _uiState = MutableStateFlow(SearchUiState())
    val uiState: StateFlow<SearchUiState> = _uiState.asStateFlow()

    val speechPauseDuration = settingsPreferences.speechPauseDuration.stateIn(
        viewModelScope, SharingStarted.Eagerly, 3000L
    )

    private var searchJob: Job? = null

    fun updateQuery(query: String) {
        _uiState.value = _uiState.value.copy(query = query)
        // Debounce typed search
        searchJob?.cancel()
        if (query.isNotBlank()) {
            searchJob = viewModelScope.launch {
                delay(500)
                search()
            }
        }
    }

    fun setFilter(groupKey: String, value: String?) {
        _uiState.value = when (groupKey) {
            "companies" -> _uiState.value.copy(selectedCompany = value)
            "holders" -> _uiState.value.copy(selectedHolder = value)
            "documentTypes" -> _uiState.value.copy(selectedDocumentType = value)
            "taxTypes" -> _uiState.value.copy(selectedTaxType = value)
            "years" -> _uiState.value.copy(selectedYear = value)
            "fileTypes" -> _uiState.value.copy(selectedFileType = value)
            else -> _uiState.value
        }
        // Re-search with new filters
        if (_uiState.value.query.isNotBlank()) {
            searchJob?.cancel()
            search()
        }
    }

    fun clearAllFilters() {
        _uiState.value = _uiState.value.copy(
            selectedCompany = null,
            selectedHolder = null,
            selectedDocumentType = null,
            selectedTaxType = null,
            selectedYear = null,
            selectedFileType = null,
        )
        if (_uiState.value.query.isNotBlank()) {
            searchJob?.cancel()
            search()
        }
    }

    private fun buildFilters(): Map<String, String> {
        val s = _uiState.value
        val map = mutableMapOf<String, String>()
        s.selectedCompany?.let { map["companies"] = it }
        s.selectedHolder?.let { map["holders"] = it }
        s.selectedDocumentType?.let { map["documentTypes"] = it }
        s.selectedTaxType?.let { map["taxTypes"] = it }
        s.selectedYear?.let { map["years"] = it }
        s.selectedFileType?.let { map["fileTypes"] = it }
        return map
    }

    fun search() {
        val query = _uiState.value.query.trim()
        if (query.isBlank()) return

        _uiState.value = _uiState.value.copy(isSearching = true, error = null, hasSearched = true)

        viewModelScope.launch {
            when (val result = repository.search(query, filters = buildFilters())) {
                is NetworkResult.Success -> {
                    val (results, meta) = result.data
                    _uiState.value = _uiState.value.copy(
                        results = results,
                        totalResults = meta?.total ?: results.size,
                        aggregations = meta?.aggregations ?: emptyMap(),
                        isSearching = false
                    )
                }
                is NetworkResult.Error -> {
                    _uiState.value = _uiState.value.copy(
                        isSearching = false,
                        error = result.message
                    )
                }
                is NetworkResult.Loading -> {}
            }
        }
    }

    fun onVoiceResult(text: String) {
        _uiState.value = _uiState.value.copy(query = text, isListening = false)
        // Immediate search on voice input (no debounce)
        searchJob?.cancel()
        search()
    }

    fun setListening(listening: Boolean) {
        _uiState.value = _uiState.value.copy(isListening = listening)
    }

    fun setVoiceError(error: String?) {
        _uiState.value = _uiState.value.copy(voiceError = error, isListening = false)
    }

    fun clearVoiceError() {
        _uiState.value = _uiState.value.copy(voiceError = null)
    }
}
