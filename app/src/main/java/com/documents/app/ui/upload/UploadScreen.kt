package com.documents.app.ui.upload

import android.Manifest
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import com.documents.app.data.model.MetadataListItem
import com.documents.app.viewmodel.TAX_TYPES
import com.documents.app.viewmodel.UploadViewModel
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UploadScreen(
    onUploadComplete: () -> Unit,
    viewModel: UploadViewModel,
) {
    val state by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val contentResolver = context.contentResolver

    // File picker
    val filePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { viewModel.setSelectedFile(it, contentResolver) }
    }

    // Camera capture
    var cameraPhotoUri by remember { mutableStateOf<Uri?>(null) }

    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        if (success) {
            cameraPhotoUri?.let { uri ->
                viewModel.setCameraFile(uri, "image/jpeg")
            }
        }
    }

    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            val photoFile = createCameraFile(context)
            val uri = FileProvider.getUriForFile(context, "${context.packageName}.provider", photoFile)
            cameraPhotoUri = uri
            cameraLauncher.launch(uri)
        }
    }

    fun launchCamera() {
        cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
    }

    LaunchedEffect(state.uploadSuccess) {
        if (state.uploadSuccess) {
            onUploadComplete()
        }
    }

    val previewFilename = remember(
        state.selectedFilename, state.year, state.month, state.day,
        state.company, state.holder, state.documentType, state.purpose,
        state.accountNumber, state.taxType, state.wintonDisclosure
    ) {
        viewModel.generatePreviewFilename()
    }

    Column(modifier = Modifier.fillMaxSize()) {
        TopAppBar(title = { Text("Upload Document") })

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Source selection: file or camera
            if (state.selectedUri != null) {
                // File selected — show info + change option
                OutlinedCard(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            if (state.selectedMimeType.startsWith("image/")) Icons.Default.Image
                            else Icons.Default.InsertDriveFile,
                            contentDescription = null,
                            modifier = Modifier.size(32.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(state.selectedFilename, style = MaterialTheme.typography.bodyLarge)
                            Text(
                                state.selectedMimeType,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        IconButton(onClick = { viewModel.reset() }) {
                            Icon(Icons.Default.Close, contentDescription = "Clear")
                        }
                    }
                }
            } else {
                // Two options: Choose File or Take Photo
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    OutlinedCard(
                        modifier = Modifier.weight(1f),
                        onClick = { filePicker.launch("*/*") }
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            Icon(
                                Icons.Default.FolderOpen,
                                contentDescription = null,
                                modifier = Modifier.size(36.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                "Choose File",
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                    OutlinedCard(
                        modifier = Modifier.weight(1f),
                        onClick = { launchCamera() }
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            Icon(
                                Icons.Default.CameraAlt,
                                contentDescription = null,
                                modifier = Modifier.size(36.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                "Take Photo",
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }

            // --- Document Metadata Section ---
            if (state.selectedUri != null) {
                Text(
                    "Document Metadata",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(top = 4.dp)
                )

                // Document Date: Year / Month / Day
                Text("Document Date", style = MaterialTheme.typography.labelLarge)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = state.year,
                        onValueChange = { viewModel.setYear(it.filter { c -> c.isDigit() }) },
                        label = { Text("YYYY") },
                        modifier = Modifier.weight(1f),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                    )
                    OutlinedTextField(
                        value = state.month,
                        onValueChange = { viewModel.setMonth(it.filter { c -> c.isDigit() }) },
                        label = { Text("MM") },
                        modifier = Modifier.weight(1f),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                    )
                    OutlinedTextField(
                        value = state.day,
                        onValueChange = { viewModel.setDay(it.filter { c -> c.isDigit() }) },
                        label = { Text("DD") },
                        modifier = Modifier.weight(1f),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                    )
                }

                // Company dropdown
                MetadataDropdown(
                    label = "Company",
                    value = state.company,
                    options = state.companies,
                    onValueChange = { viewModel.setCompany(it) },
                )

                // Holder dropdown
                MetadataDropdown(
                    label = "Holder",
                    value = state.holder,
                    options = state.holders,
                    onValueChange = { viewModel.setHolder(it) },
                )

                // Document Type dropdown
                MetadataDropdown(
                    label = "Document Type",
                    value = state.documentType,
                    options = state.documentTypes,
                    onValueChange = { viewModel.setDocumentType(it) },
                )

                // Purpose
                OutlinedTextField(
                    value = state.purpose,
                    onValueChange = { viewModel.setPurpose(it) },
                    label = { Text("Purpose") },
                    placeholder = { Text("Format_Like_This") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                )

                // Account Number
                OutlinedTextField(
                    value = state.accountNumber,
                    onValueChange = { viewModel.setAccountNumber(it) },
                    label = { Text("Account Number") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                )

                // Tax Type dropdown
                TaxTypeDropdown(
                    value = state.taxType,
                    onValueChange = { viewModel.setTaxType(it) },
                )

                // Checkboxes
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(
                        checked = state.wintonDisclosure,
                        onCheckedChange = { viewModel.setWintonDisclosure(it) }
                    )
                    Text("Winton Disclosure", modifier = Modifier.padding(start = 4.dp))
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(
                        checked = state.ustaxAccountClosing,
                        onCheckedChange = { viewModel.setUstaxAccountClosing(it) }
                    )
                    Text("USTax Account Closing", modifier = Modifier.padding(start = 4.dp))
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(
                        checked = state.ustaxOpening,
                        onCheckedChange = { viewModel.setUstaxOpening(it) }
                    )
                    Text("USTax Opening", modifier = Modifier.padding(start = 4.dp))
                }

                // Notes
                OutlinedTextField(
                    value = state.notes,
                    onValueChange = { viewModel.setNotes(it) },
                    label = { Text("Notes") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 2,
                )

                // Filename preview
                Text("Preview Filename", style = MaterialTheme.typography.labelLarge)
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
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
            }

            // Error message
            state.error?.let { error ->
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    )
                ) {
                    Text(
                        text = error,
                        modifier = Modifier.padding(16.dp),
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                }
            }

            Spacer(modifier = Modifier.height(80.dp))
        }

        // Upload button pinned to bottom
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shadowElevation = 8.dp,
        ) {
            Button(
                onClick = { viewModel.upload(contentResolver) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                enabled = state.selectedUri != null && !state.isUploading
            ) {
                if (state.isUploading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Uploading...")
                } else {
                    Icon(Icons.Default.CloudUpload, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Upload & Rename")
                }
            }
        }
    }
}

private fun createCameraFile(context: android.content.Context): File {
    val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
    val dir = File(context.cacheDir, "camera_photos")
    dir.mkdirs()
    return File(dir, "DOC_${timestamp}.jpg")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MetadataDropdown(
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
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor(),
            singleLine = true,
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = {
                expanded = false
                searchText = ""
            }
        ) {
            if (value.isNotBlank()) {
                DropdownMenuItem(
                    text = {
                        Text("Clear", color = MaterialTheme.colorScheme.error)
                    },
                    onClick = {
                        onValueChange("")
                        searchText = ""
                        expanded = false
                    }
                )
            }
            filtered.forEach { item ->
                DropdownMenuItem(
                    text = {
                        val countStr = item.count?.documents?.let { " ($it)" } ?: ""
                        Text("${item.name}$countStr")
                    },
                    onClick = {
                        onValueChange(item.name)
                        searchText = ""
                        expanded = false
                    }
                )
            }
            if (filtered.isEmpty() && searchText.isNotBlank()) {
                DropdownMenuItem(
                    text = { Text("Use \"$searchText\"") },
                    onClick = {
                        onValueChange(searchText)
                        searchText = ""
                        expanded = false
                    }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TaxTypeDropdown(
    value: String,
    onValueChange: (String) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = !expanded }
    ) {
        OutlinedTextField(
            value = if (value.isBlank()) "None" else value,
            onValueChange = {},
            readOnly = true,
            label = { Text("Tax Type") },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor(),
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            TAX_TYPES.forEach { type ->
                DropdownMenuItem(
                    text = { Text(type.ifBlank { "None" }) },
                    onClick = {
                        onValueChange(type)
                        expanded = false
                    }
                )
            }
        }
    }
}
