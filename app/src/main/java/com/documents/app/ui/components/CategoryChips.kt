package com.documents.app.ui.components

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

data class FacetItem(
    val key: String,
    val label: String,
    val count: Int = 0,
)

data class FacetGroup(
    val key: String,
    val label: String,
    val items: List<FacetItem>,
    val selectedKey: String?,
)

data class ActiveFilter(
    val groupKey: String,
    val groupLabel: String,
    val value: String,
)

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun FilterBar(
    activeFilters: List<ActiveFilter>,
    facetGroups: List<FacetGroup>,
    onFilterChanged: (groupKey: String, itemKey: String?) -> Unit,
    onClearAll: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var showSheet by remember { mutableStateOf(false) }

    if (facetGroups.isEmpty()) return

    Row(
        modifier = modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        FilterChip(
            selected = activeFilters.isNotEmpty(),
            onClick = { showSheet = true },
            label = {
                Text(
                    if (activeFilters.isEmpty()) "Filters"
                    else "Filters (${activeFilters.size})"
                )
            },
            leadingIcon = {
                Icon(
                    Icons.Default.FilterList,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
            },
        )

        activeFilters.forEach { filter ->
            InputChip(
                selected = true,
                onClick = { onFilterChanged(filter.groupKey, null) },
                label = { Text(filter.value, maxLines = 1) },
                trailingIcon = {
                    Icon(
                        Icons.Default.Close,
                        contentDescription = "Remove",
                        modifier = Modifier.size(16.dp)
                    )
                },
            )
        }
    }

    if (showSheet) {
        ModalBottomSheet(
            onDismissRequest = { showSheet = false },
        ) {
            FilterSheetContent(
                facetGroups = facetGroups,
                onFilterChanged = onFilterChanged,
                onClearAll = {
                    onClearAll()
                    showSheet = false
                },
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun FilterSheetContent(
    facetGroups: List<FacetGroup>,
    onFilterChanged: (groupKey: String, itemKey: String?) -> Unit,
    onClearAll: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 20.dp, end = 20.dp, bottom = 32.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("Filters", style = MaterialTheme.typography.titleLarge)
            val hasActive = facetGroups.any { it.selectedKey != null }
            if (hasActive) {
                TextButton(onClick = onClearAll) {
                    Text("Clear all")
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        facetGroups.forEach { group ->
            if (group.items.isNotEmpty()) {
                Text(
                    text = group.label,
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    group.items.forEach { item ->
                        FilterChip(
                            selected = group.selectedKey == item.key,
                            onClick = {
                                onFilterChanged(
                                    group.key,
                                    if (group.selectedKey == item.key) null else item.key
                                )
                            },
                            label = {
                                Text(
                                    if (item.count > 0) "${item.label} (${item.count})"
                                    else item.label
                                )
                            },
                        )
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}
