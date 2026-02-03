package com.documents.app.ui.home

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.pullrefresh.PullRefreshIndicator
import androidx.compose.material.pullrefresh.pullRefresh
import androidx.compose.material.pullrefresh.rememberPullRefreshState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.documents.app.ui.components.ActiveFilter
import com.documents.app.ui.components.DocumentListItem
import com.documents.app.ui.components.ErrorState
import com.documents.app.ui.components.FacetGroup
import com.documents.app.ui.components.FilterBar
import com.documents.app.ui.components.LoadingState
import com.documents.app.ui.components.buildThumbnailUrl
import com.documents.app.viewmodel.DocumentListViewModel

@OptIn(ExperimentalMaterialApi::class, ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onDocumentClick: (String) -> Unit,
    viewModel: DocumentListViewModel,
) {
    val state by viewModel.uiState.collectAsState()
    val baseUrl by viewModel.apiBaseUrl.collectAsState()
    val listState = rememberLazyListState()

    val pullRefreshState = rememberPullRefreshState(
        refreshing = state.isRefreshing,
        onRefresh = { viewModel.refresh() }
    )

    // Load more when near bottom
    LaunchedEffect(listState) {
        snapshotFlow {
            val layoutInfo = listState.layoutInfo
            val lastVisible = layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
            val totalItems = layoutInfo.totalItemsCount
            lastVisible >= totalItems - 3
        }.collect { nearEnd ->
            if (nearEnd && !state.isLoadingMore) {
                viewModel.loadMore()
            }
        }
    }

    // Build facet groups and active filters for FilterBar
    val facetGroups = remember(
        state.companyFacets, state.holderFacets, state.yearFacets, state.docTypeFacets,
        state.selectedCompany, state.selectedHolder, state.selectedYear, state.selectedDocType
    ) {
        listOf(
            FacetGroup("company", "Company", state.companyFacets, state.selectedCompany),
            FacetGroup("holder", "Holder", state.holderFacets, state.selectedHolder),
            FacetGroup("year", "Year", state.yearFacets, state.selectedYear),
            FacetGroup("docType", "Type", state.docTypeFacets, state.selectedDocType),
        ).filter { it.items.isNotEmpty() }
    }

    val activeFilters = remember(
        state.selectedCompany, state.selectedHolder, state.selectedYear, state.selectedDocType
    ) {
        listOfNotNull(
            state.selectedCompany?.let { ActiveFilter("company", "Company", it) },
            state.selectedHolder?.let { ActiveFilter("holder", "Holder", it) },
            state.selectedYear?.let { ActiveFilter("year", "Year", it) },
            state.selectedDocType?.let { ActiveFilter("docType", "Type", it) },
        )
    }

    Column(modifier = Modifier.fillMaxSize()) {
        TopAppBar(
            title = { Text("Documents") }
        )

        // Compact filter bar
        FilterBar(
            activeFilters = activeFilters,
            facetGroups = facetGroups,
            onFilterChanged = { groupKey, value ->
                when (groupKey) {
                    "company" -> viewModel.setCompanyFilter(value)
                    "holder" -> viewModel.setHolderFilter(value)
                    "year" -> viewModel.setYearFilter(value)
                    "docType" -> viewModel.setDocTypeFilter(value)
                }
            },
            onClearAll = { viewModel.clearAllFilters() },
        )

        Box(
            modifier = Modifier
                .fillMaxSize()
                .pullRefresh(pullRefreshState)
        ) {
            when {
                state.isLoading -> LoadingState()
                state.error != null && state.documents.isEmpty() -> {
                    ErrorState(
                        message = state.error ?: "Unknown error",
                        onRetry = { viewModel.loadDocuments() }
                    )
                }
                state.documents.isEmpty() -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No documents found",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                else -> {
                    LazyColumn(
                        state = listState,
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(state.documents, key = { it.id }) { doc ->
                            DocumentListItem(
                                filename = doc.originalFilename ?: doc.filename,
                                category = doc.category,
                                size = doc.size,
                                mimeType = doc.mimeType,
                                createdAt = doc.createdAt,
                                storagePath = doc.storagePath,
                                thumbnailUrl = buildThumbnailUrl(baseUrl, doc.storagePath),
                                onClick = { onDocumentClick(doc.id) }
                            )
                        }

                        if (state.isLoadingMore) {
                            item {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    CircularProgressIndicator(modifier = Modifier.size(24.dp))
                                }
                            }
                        }
                    }
                }
            }

            PullRefreshIndicator(
                refreshing = state.isRefreshing,
                state = pullRefreshState,
                modifier = Modifier.align(Alignment.TopCenter)
            )
        }
    }
}
