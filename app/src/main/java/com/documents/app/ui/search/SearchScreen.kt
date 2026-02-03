package com.documents.app.ui.search

import android.Manifest
import android.speech.SpeechRecognizer
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.documents.app.ui.components.ActiveFilter
import com.documents.app.ui.components.ErrorState
import com.documents.app.ui.components.FacetGroup
import com.documents.app.ui.components.FacetItem
import com.documents.app.ui.components.FilterBar
import com.documents.app.ui.components.VoiceSearchButton
import com.documents.app.ui.components.createSpeechRecognizer
import com.documents.app.ui.components.formatFileSize
import com.documents.app.ui.components.startListening
import com.documents.app.viewmodel.SearchViewModel

private val FACET_LABELS = mapOf(
    "companies" to "Company",
    "holders" to "Holder",
    "documentTypes" to "Document Type",
    "taxTypes" to "Tax Type",
    "years" to "Year",
    "fileTypes" to "File Type",
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(
    onDocumentClick: (String) -> Unit,
    viewModel: SearchViewModel,
) {
    val state by viewModel.uiState.collectAsState()
    val speechPauseMs by viewModel.speechPauseDuration.collectAsState()
    val context = LocalContext.current

    var hasAudioPermission by remember { mutableStateOf(false) }
    val audioPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasAudioPermission = granted
    }

    var speechRecognizer by remember { mutableStateOf<SpeechRecognizer?>(null) }

    DisposableEffect(Unit) {
        speechRecognizer = createSpeechRecognizer(
            context = context,
            onResult = { text -> viewModel.onVoiceResult(text) },
            onError = { error -> viewModel.setVoiceError(error) },
            onListeningStateChanged = { listening -> viewModel.setListening(listening) }
        )
        onDispose {
            speechRecognizer?.destroy()
        }
    }

    // Build facet groups from aggregations
    val facetGroups = remember(state.aggregations, state.selectedCompany, state.selectedHolder,
        state.selectedDocumentType, state.selectedTaxType, state.selectedYear, state.selectedFileType) {
        state.aggregations.mapNotNull { (key, agg) ->
            val label = FACET_LABELS[key] ?: return@mapNotNull null
            if (agg.buckets.isEmpty()) return@mapNotNull null
            val selectedKey = when (key) {
                "companies" -> state.selectedCompany
                "holders" -> state.selectedHolder
                "documentTypes" -> state.selectedDocumentType
                "taxTypes" -> state.selectedTaxType
                "years" -> state.selectedYear
                "fileTypes" -> state.selectedFileType
                else -> null
            }
            FacetGroup(
                key = key,
                label = label,
                items = agg.buckets.map { bucket ->
                    FacetItem(key = bucket.key, label = bucket.key, count = bucket.docCount)
                },
                selectedKey = selectedKey,
            )
        }
    }

    val activeFilters = remember(state.selectedCompany, state.selectedHolder,
        state.selectedDocumentType, state.selectedTaxType, state.selectedYear, state.selectedFileType) {
        buildList {
            state.selectedCompany?.let { add(ActiveFilter("companies", "Company", it)) }
            state.selectedHolder?.let { add(ActiveFilter("holders", "Holder", it)) }
            state.selectedDocumentType?.let { add(ActiveFilter("documentTypes", "Document Type", it)) }
            state.selectedTaxType?.let { add(ActiveFilter("taxTypes", "Tax Type", it)) }
            state.selectedYear?.let { add(ActiveFilter("years", "Year", it)) }
            state.selectedFileType?.let { add(ActiveFilter("fileTypes", "File Type", it)) }
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        TopAppBar(
            title = { Text("Search Documents") }
        )

        OutlinedTextField(
            value = state.query,
            onValueChange = { viewModel.updateQuery(it) },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            placeholder = { Text("Search documents...") },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
            trailingIcon = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (state.query.isNotBlank()) {
                        IconButton(onClick = { viewModel.updateQuery("") }) {
                            Icon(Icons.Default.Clear, contentDescription = "Clear")
                        }
                    }
                    VoiceSearchButton(
                        isListening = state.isListening,
                        onStartListening = {
                            if (hasAudioPermission) {
                                speechRecognizer?.let { startListening(it, speechPauseMs) }
                            } else {
                                audioPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                            }
                        },
                        onStopListening = {
                            speechRecognizer?.stopListening()
                            viewModel.setListening(false)
                        }
                    )
                }
            },
            singleLine = true,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(onSearch = { viewModel.search() })
        )

        // Facet filters (shown when aggregations are available)
        if (facetGroups.isNotEmpty()) {
            FilterBar(
                activeFilters = activeFilters,
                facetGroups = facetGroups,
                onFilterChanged = { groupKey, itemKey -> viewModel.setFilter(groupKey, itemKey) },
                onClearAll = { viewModel.clearAllFilters() },
            )
        }

        // Voice error snackbar
        state.voiceError?.let { error ->
            Snackbar(
                modifier = Modifier.padding(16.dp),
                action = {
                    TextButton(onClick = { viewModel.clearVoiceError() }) {
                        Text("Dismiss")
                    }
                }
            ) {
                Text(error)
            }
        }

        // Listening indicator
        if (state.isListening) {
            LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            Text(
                text = "Listening...",
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                color = MaterialTheme.colorScheme.primary
            )
        }

        when {
            state.isSearching -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }
            state.error != null -> {
                ErrorState(
                    message = state.error ?: "Search failed",
                    onRetry = { viewModel.search() }
                )
            }
            state.hasSearched && state.results.isEmpty() -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Default.SearchOff,
                            contentDescription = null,
                            modifier = Modifier.size(48.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            "No results found",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
            state.results.isNotEmpty() -> {
                Text(
                    text = "${state.totalResults} result${if (state.totalResults != 1) "s" else ""}",
                    style = MaterialTheme.typography.labelMedium,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(state.results, key = { it.id }) { result ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onDocumentClick(result.id) }
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(
                                    text = result.filename,
                                    style = MaterialTheme.typography.bodyLarge,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(top = 4.dp)
                                ) {
                                    result.category?.let { cat ->
                                        SuggestionChip(
                                            onClick = {},
                                            label = { Text(cat, style = MaterialTheme.typography.labelSmall) },
                                            modifier = Modifier.height(24.dp)
                                        )
                                    }
                                    result.score?.let { score ->
                                        Text(
                                            text = "Score: ${String.format("%.1f%%", score * 100)}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    result.size?.let { size ->
                                        Text(
                                            text = formatFileSize(size),
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                                result.highlightText?.let { hl ->
                                    Text(
                                        text = hl,
                                        style = MaterialTheme.typography.bodySmall,
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.padding(top = 4.dp),
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
            }
            else -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Default.Search,
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            "Search documents by keyword or voice",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}
