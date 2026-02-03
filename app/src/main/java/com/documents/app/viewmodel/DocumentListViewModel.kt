package com.documents.app.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.documents.app.data.api.NetworkResult
import com.documents.app.data.model.DocumentSummary
import com.documents.app.data.model.V2Meta
import com.documents.app.data.preferences.SettingsPreferences
import com.documents.app.data.repository.DocumentRepository
import com.documents.app.ui.components.FacetItem
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class DocumentListUiState(
    val documents: List<DocumentSummary> = emptyList(),
    val meta: V2Meta? = null,
    val currentPage: Int = 1,
    val selectedCompany: String? = null,
    val selectedHolder: String? = null,
    val selectedYear: String? = null,
    val selectedDocType: String? = null,
    val companyFacets: List<FacetItem> = emptyList(),
    val holderFacets: List<FacetItem> = emptyList(),
    val yearFacets: List<FacetItem> = emptyList(),
    val docTypeFacets: List<FacetItem> = emptyList(),
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val isLoadingMore: Boolean = false,
    val error: String? = null,
)

@HiltViewModel
class DocumentListViewModel @Inject constructor(
    private val repository: DocumentRepository,
    settingsPreferences: SettingsPreferences,
) : ViewModel() {

    private val _uiState = MutableStateFlow(DocumentListUiState())
    val uiState: StateFlow<DocumentListUiState> = _uiState.asStateFlow()

    val apiBaseUrl = settingsPreferences.apiBaseUrl.stateIn(
        viewModelScope, SharingStarted.Eagerly, ""
    )

    // Keep all loaded docs to derive facets from
    private var allDocuments: List<DocumentSummary> = emptyList()

    init {
        loadDocuments()
    }

    fun loadDocuments(page: Int = 1) {
        if (page == 1) {
            _uiState.value = _uiState.value.copy(
                isLoading = !_uiState.value.isRefreshing,
                error = null
            )
        } else {
            _uiState.value = _uiState.value.copy(isLoadingMore = true)
        }

        viewModelScope.launch {
            when (val result = repository.listDocuments(page = page)) {
                is NetworkResult.Success -> {
                    val (docs, meta) = result.data
                    allDocuments = if (page == 1) docs else allDocuments + docs
                    val facets = buildFacets(allDocuments)
                    val filtered = applyFilters(allDocuments)
                    _uiState.value = _uiState.value.copy(
                        documents = filtered,
                        meta = meta,
                        currentPage = page,
                        companyFacets = facets.companies,
                        holderFacets = facets.holders,
                        yearFacets = facets.years,
                        docTypeFacets = facets.docTypes,
                        isLoading = false,
                        isRefreshing = false,
                        isLoadingMore = false
                    )
                }
                is NetworkResult.Error -> {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        isRefreshing = false,
                        isLoadingMore = false,
                        error = result.message
                    )
                }
                is NetworkResult.Loading -> {}
            }
        }
    }

    fun refresh() {
        _uiState.value = _uiState.value.copy(isRefreshing = true, error = null)
        loadDocuments(page = 1)
    }

    fun loadMore() {
        val meta = _uiState.value.meta ?: return
        val currentPage = _uiState.value.currentPage
        val totalPages = meta.pages ?: return
        if (currentPage < totalPages && !_uiState.value.isLoadingMore) {
            loadDocuments(page = currentPage + 1)
        }
    }

    fun setCompanyFilter(company: String?) {
        _uiState.value = _uiState.value.copy(selectedCompany = company)
        _uiState.value = _uiState.value.copy(documents = applyFilters(allDocuments))
    }

    fun setHolderFilter(holder: String?) {
        _uiState.value = _uiState.value.copy(selectedHolder = holder)
        _uiState.value = _uiState.value.copy(documents = applyFilters(allDocuments))
    }

    fun setYearFilter(year: String?) {
        _uiState.value = _uiState.value.copy(selectedYear = year)
        _uiState.value = _uiState.value.copy(documents = applyFilters(allDocuments))
    }

    fun setDocTypeFilter(docType: String?) {
        _uiState.value = _uiState.value.copy(selectedDocType = docType)
        _uiState.value = _uiState.value.copy(documents = applyFilters(allDocuments))
    }

    fun clearAllFilters() {
        _uiState.value = _uiState.value.copy(
            selectedCompany = null,
            selectedHolder = null,
            selectedYear = null,
            selectedDocType = null,
            documents = allDocuments,
        )
    }

    private fun applyFilters(docs: List<DocumentSummary>): List<DocumentSummary> {
        val state = _uiState.value
        return docs.filter { doc ->
            val meta = doc.metadata
            (state.selectedCompany == null || meta?.company == state.selectedCompany) &&
            (state.selectedHolder == null || meta?.holder == state.selectedHolder) &&
            (state.selectedYear == null || meta?.year == state.selectedYear) &&
            (state.selectedDocType == null || meta?.documentType == state.selectedDocType)
        }
    }

    private data class Facets(
        val companies: List<FacetItem>,
        val holders: List<FacetItem>,
        val years: List<FacetItem>,
        val docTypes: List<FacetItem>,
    )

    private fun buildFacets(docs: List<DocumentSummary>): Facets {
        val companies = docs.mapNotNull { it.metadata?.company }
            .filter { it.isNotBlank() }
            .groupBy { it }
            .map { FacetItem(it.key, it.key, it.value.size) }
            .sortedByDescending { it.count }

        val holders = docs.mapNotNull { it.metadata?.holder }
            .filter { it.isNotBlank() }
            .groupBy { it }
            .map { FacetItem(it.key, it.key, it.value.size) }
            .sortedByDescending { it.count }

        val years = docs.mapNotNull { it.metadata?.year }
            .filter { it.isNotBlank() }
            .groupBy { it }
            .map { FacetItem(it.key, it.key, it.value.size) }
            .sortedByDescending { it.key }

        val docTypes = docs.mapNotNull { it.metadata?.documentType }
            .filter { it.isNotBlank() }
            .groupBy { it }
            .map { FacetItem(it.key, it.key, it.value.size) }
            .sortedByDescending { it.count }

        return Facets(companies, holders, years, docTypes)
    }
}
