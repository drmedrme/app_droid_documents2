package com.documents.app.ui.detail

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.documents.app.data.model.MetadataListItem
import com.documents.app.ui.components.ErrorState
import com.documents.app.ui.components.LoadingState
import com.documents.app.ui.components.buildThumbnailUrl
import com.documents.app.ui.components.formatDate
import com.documents.app.ui.components.formatFileSize
import com.documents.app.viewmodel.DocumentDetailViewModel
import com.documents.app.viewmodel.TAX_TYPES

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DocumentDetailScreen(
    documentId: String,
    onNavigateBack: () -> Unit,
    viewModel: DocumentDetailViewModel,
) {
    val state by viewModel.uiState.collectAsState()
    val apiBaseUrl by viewModel.apiBaseUrl.collectAsState()
    val context = LocalContext.current
    var showDeleteDialog by remember { mutableStateOf(false) }

    LaunchedEffect(documentId) {
        viewModel.loadDocument(documentId)
    }

    LaunchedEffect(state.deleted) {
        if (state.deleted) onNavigateBack()
    }

    // Snackbar for success messages
    state.actionSuccess?.let { msg ->
        LaunchedEffect(msg) {
            viewModel.clearActionResult()
        }
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Delete Document") },
            text = { Text("Are you sure you want to delete this document?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteDialog = false
                        viewModel.deleteDocument()
                    },
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) { Text("Delete") }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) { Text("Cancel") }
            }
        )
    }

    Column(modifier = Modifier.fillMaxSize()) {
        TopAppBar(
            title = { Text("Document Detail") },
            navigationIcon = {
                IconButton(onClick = onNavigateBack) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                }
            },
            actions = {
                if (!state.isEditing) {
                    IconButton(onClick = { viewModel.startEditing() }) {
                        Icon(Icons.Default.Edit, contentDescription = "Edit metadata")
                    }
                }
                IconButton(
                    onClick = { viewModel.openDocument(context) },
                    enabled = !state.isOpening
                ) {
                    if (state.isOpening) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                    } else {
                        Icon(Icons.Default.OpenInNew, contentDescription = "Open")
                    }
                }
                IconButton(
                    onClick = { viewModel.shareDocument(context) },
                    enabled = !state.isSharing
                ) {
                    if (state.isSharing) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                    } else {
                        Icon(Icons.Default.Share, contentDescription = "Share")
                    }
                }
                IconButton(onClick = { showDeleteDialog = true }) {
                    Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error)
                }
            }
        )

        when {
            state.isLoading -> LoadingState()
            state.error != null && state.document == null -> {
                ErrorState(
                    message = state.error ?: "Failed to load document",
                    onRetry = { viewModel.loadDocument(documentId) }
                )
            }
            state.document != null -> {
                val doc = state.document!!

                if (state.isEditing) {
                    // --- Edit mode ---
                    EditMetadataContent(
                        state = state,
                        viewModel = viewModel,
                        originalFilename = doc.originalFilename ?: doc.filename,
                    )
                } else {
                    // --- View mode ---
                    ViewDocumentContent(
                        state = state,
                        viewModel = viewModel,
                        apiBaseUrl = apiBaseUrl,
                    )
                }
            }
        }
    }
}

@Composable
private fun ViewDocumentContent(
    state: com.documents.app.viewmodel.DocumentDetailUiState,
    viewModel: DocumentDetailViewModel,
    apiBaseUrl: String,
) {
    val doc = state.document ?: return
    val context = LocalContext.current
    val thumbnailUrl = buildThumbnailUrl(apiBaseUrl, doc.storagePath)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        // Thumbnail
        if (thumbnailUrl != null) {
            Card(modifier = Modifier.fillMaxWidth()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp),
                    contentAlignment = Alignment.Center
                ) {
                    AsyncImage(
                        model = ImageRequest.Builder(LocalContext.current)
                            .data(thumbnailUrl).crossfade(true).build(),
                        contentDescription = "Document preview",
                        modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(12.dp)),
                        contentScale = ContentScale.Fit,
                    )
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
        }

        // Filename
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(doc.originalFilename ?: doc.filename, style = MaterialTheme.typography.headlineSmall)
                if (doc.originalFilename != null && doc.originalFilename != doc.filename) {
                    Text(
                        "Stored as: ${doc.filename}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Open button
        Button(
            onClick = { viewModel.openDocument(context) },
            modifier = Modifier.fillMaxWidth(),
            enabled = !state.isOpening
        ) {
            if (state.isOpening) {
                CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp, color = MaterialTheme.colorScheme.onPrimary)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Opening...")
            } else {
                Icon(Icons.Default.OpenInNew, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Open Document")
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Edit Metadata button
        OutlinedButton(
            onClick = { viewModel.startEditing() },
            modifier = Modifier.fillMaxWidth(),
        ) {
            Icon(Icons.Default.Edit, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Edit Metadata & Rename")
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Details
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Details", style = MaterialTheme.typography.titleMedium)
                Spacer(modifier = Modifier.height(12.dp))
                doc.company?.let { MetadataRow("Company", it) }
                doc.holder?.let { MetadataRow("Holder", it) }
                doc.documentType?.let { MetadataRow("Document Type", it) }
                val meta = doc.metadata
                meta?.year?.let { year ->
                    val dateStr = buildString {
                        append(year)
                        meta.month?.let { append("-${it.padStart(2, '0')}") }
                        meta.day?.let { append("-${it.padStart(2, '0')}") }
                    }
                    MetadataRow("Document Date", dateStr)
                }
                meta?.purpose?.let { MetadataRow("Purpose", it) }
                meta?.accountNumber?.let { MetadataRow("Account", it) }
                meta?.taxType?.let { MetadataRow("Tax Type", it) }
                MetadataRow("Category", doc.category ?: "None")
                MetadataRow("Confidentiality", doc.confidentialityLevel ?: "STANDARD")
                MetadataRow("Size", doc.size?.let { formatFileSize(it) } ?: "Unknown")
                MetadataRow("Type", doc.mimeType ?: "Unknown")
                MetadataRow("Created", doc.createdAt?.let { formatDate(it) } ?: "Unknown")
                MetadataRow("Updated", doc.modifiedAt?.let { formatDate(it) } ?: "Unknown")
                if (meta?.wintonDisclosure == true) MetadataRow("Winton Disclosure", "Yes")
                if (meta?.ustaxAccountClosing == true) MetadataRow("USTax Account Closing", "Yes")
                if (meta?.ustaxOpening == true) MetadataRow("USTax Opening", "Yes")
            }
        }

        // Notes
        val notesText = doc.notes
        if (!notesText.isNullOrBlank()) {
            Spacer(modifier = Modifier.height(16.dp))
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Notes", style = MaterialTheme.typography.titleMedium)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(notesText, style = MaterialTheme.typography.bodyMedium)
                }
            }
        }

        // Text content
        val extractedText = doc.textContent
        if (!extractedText.isNullOrBlank()) {
            Spacer(modifier = Modifier.height(16.dp))
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Extracted Text", style = MaterialTheme.typography.titleMedium)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = extractedText.take(500) + if (extractedText.length > 500) "..." else "",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        // Versions
        if (state.versions.isNotEmpty()) {
            Spacer(modifier = Modifier.height(16.dp))
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Versions", style = MaterialTheme.typography.titleMedium)
                    Spacer(modifier = Modifier.height(8.dp))
                    state.versions.forEach { version ->
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("v${version.versionNumber}", style = MaterialTheme.typography.bodyMedium)
                            Text(
                                version.createdAt?.let { formatDate(it) } ?: "",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(32.dp))
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EditMetadataContent(
    state: com.documents.app.viewmodel.DocumentDetailUiState,
    viewModel: DocumentDetailViewModel,
    originalFilename: String,
) {
    val previewFilename = remember(
        state.editYear, state.editMonth, state.editDay,
        state.editCompany, state.editHolder, state.editDocumentType,
        state.editPurpose, state.editAccountNumber, state.editTaxType,
        state.editWintonDisclosure
    ) {
        viewModel.generatePreviewFilename()
    }

    Column(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("Document Metadata", style = MaterialTheme.typography.titleMedium)

            // Document Date
            Text("Document Date", style = MaterialTheme.typography.labelLarge)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = state.editYear,
                    onValueChange = { viewModel.setEditYear(it.filter { c -> c.isDigit() }) },
                    label = { Text("YYYY") },
                    modifier = Modifier.weight(1f),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                )
                OutlinedTextField(
                    value = state.editMonth,
                    onValueChange = { viewModel.setEditMonth(it.filter { c -> c.isDigit() }) },
                    label = { Text("MM") },
                    modifier = Modifier.weight(1f),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                )
                OutlinedTextField(
                    value = state.editDay,
                    onValueChange = { viewModel.setEditDay(it.filter { c -> c.isDigit() }) },
                    label = { Text("DD") },
                    modifier = Modifier.weight(1f),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                )
            }

            // Company
            EditMetadataDropdown(
                label = "Company",
                value = state.editCompany,
                options = state.companies,
                onValueChange = { viewModel.setEditCompany(it) },
            )

            // Holder
            EditMetadataDropdown(
                label = "Holder",
                value = state.editHolder,
                options = state.holders,
                onValueChange = { viewModel.setEditHolder(it) },
            )

            // Document Type
            EditMetadataDropdown(
                label = "Document Type",
                value = state.editDocumentType,
                options = state.documentTypes,
                onValueChange = { viewModel.setEditDocumentType(it) },
            )

            // Purpose
            OutlinedTextField(
                value = state.editPurpose,
                onValueChange = { viewModel.setEditPurpose(it) },
                label = { Text("Purpose") },
                placeholder = { Text("Format_Like_This") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
            )

            // Account Number
            OutlinedTextField(
                value = state.editAccountNumber,
                onValueChange = { viewModel.setEditAccountNumber(it) },
                label = { Text("Account Number") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
            )

            // Tax Type
            EditTaxTypeDropdown(
                value = state.editTaxType,
                onValueChange = { viewModel.setEditTaxType(it) },
            )

            // Checkboxes
            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(checked = state.editWintonDisclosure, onCheckedChange = { viewModel.setEditWintonDisclosure(it) })
                Text("Winton Disclosure", modifier = Modifier.padding(start = 4.dp))
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(checked = state.editUstaxAccountClosing, onCheckedChange = { viewModel.setEditUstaxAccountClosing(it) })
                Text("USTax Account Closing", modifier = Modifier.padding(start = 4.dp))
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(checked = state.editUstaxOpening, onCheckedChange = { viewModel.setEditUstaxOpening(it) })
                Text("USTax Opening", modifier = Modifier.padding(start = 4.dp))
            }

            // Notes
            OutlinedTextField(
                value = state.editNotes,
                onValueChange = { viewModel.setEditNotes(it) },
                label = { Text("Notes") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 2,
            )

            // Filename preview
            Text("Preview Filename", style = MaterialTheme.typography.labelLarge)
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Text(
                    text = previewFilename,
                    modifier = Modifier.padding(12.dp),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
            Text(
                "Preview updates as you type above",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            // Error
            state.error?.let { error ->
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)) {
                    Text(error, modifier = Modifier.padding(16.dp), color = MaterialTheme.colorScheme.onErrorContainer)
                }
            }

            Spacer(modifier = Modifier.height(80.dp))
        }

        // Bottom buttons
        Surface(modifier = Modifier.fillMaxWidth(), shadowElevation = 8.dp) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = { viewModel.cancelEditing() },
                    modifier = Modifier.weight(1f),
                ) {
                    Text("Cancel")
                }
                Button(
                    onClick = { viewModel.saveAndRename() },
                    modifier = Modifier.weight(2f),
                    enabled = !state.isSaving,
                ) {
                    if (state.isSaving) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Saving...")
                    } else {
                        Icon(Icons.Default.Save, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Save & Rename")
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EditMetadataDropdown(
    label: String,
    value: String,
    options: List<MetadataListItem>,
    onValueChange: (String) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    var searchText by remember { mutableStateOf("") }

    val filtered = remember(options, searchText) {
        if (searchText.isBlank()) options
        else options.filter { it.name.contains(searchText, ignoreCase = true) }
    }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = !expanded }
    ) {
        OutlinedTextField(
            value = if (expanded) searchText else value,
            onValueChange = { text ->
                searchText = text
                if (!expanded) expanded = true
            },
            label = { Text(label) },
            placeholder = { Text("Type to search...") },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier.fillMaxWidth().menuAnchor(),
            singleLine = true,
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false; searchText = "" }
        ) {
            if (value.isNotBlank()) {
                DropdownMenuItem(
                    text = { Text("Clear", color = MaterialTheme.colorScheme.error) },
                    onClick = { onValueChange(""); searchText = ""; expanded = false }
                )
            }
            filtered.forEach { item ->
                DropdownMenuItem(
                    text = {
                        val countStr = item.count?.documents?.let { " ($it)" } ?: ""
                        Text("${item.name}$countStr")
                    },
                    onClick = { onValueChange(item.name); searchText = ""; expanded = false }
                )
            }
            if (filtered.isEmpty() && searchText.isNotBlank()) {
                DropdownMenuItem(
                    text = { Text("Use \"$searchText\"") },
                    onClick = { onValueChange(searchText); searchText = ""; expanded = false }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EditTaxTypeDropdown(
    value: String,
    onValueChange: (String) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = !expanded }
    ) {
        OutlinedTextField(
            value = value.ifBlank { "None" },
            onValueChange = {},
            readOnly = true,
            label = { Text("Tax Type") },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier.fillMaxWidth().menuAnchor(),
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            TAX_TYPES.forEach { type ->
                DropdownMenuItem(
                    text = { Text(type.ifBlank { "None" }) },
                    onClick = { onValueChange(type); expanded = false }
                )
            }
        }
    }
}

@Composable
private fun MetadataRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.bodyMedium)
    }
}
