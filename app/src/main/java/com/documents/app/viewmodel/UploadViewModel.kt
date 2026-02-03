package com.documents.app.viewmodel

import android.content.ContentResolver
import android.net.Uri
import android.provider.OpenableColumns
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.documents.app.data.api.NetworkResult
import com.documents.app.data.model.MetadataListItem
import com.documents.app.data.repository.DocumentRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class UploadUiState(
    val selectedUri: Uri? = null,
    val selectedFilename: String = "",
    val selectedMimeType: String = "application/octet-stream",
    // Metadata fields matching web frontend
    val year: String = "",
    val month: String = "",
    val day: String = "",
    val company: String = "",
    val holder: String = "",
    val documentType: String = "",
    val purpose: String = "",
    val accountNumber: String = "",
    val taxType: String = "",
    val wintonDisclosure: Boolean = false,
    val ustaxAccountClosing: Boolean = false,
    val ustaxOpening: Boolean = false,
    val notes: String = "",
    // Metadata options from API
    val companies: List<MetadataListItem> = emptyList(),
    val holders: List<MetadataListItem> = emptyList(),
    val documentTypes: List<MetadataListItem> = emptyList(),
    // Upload state
    val isUploading: Boolean = false,
    val uploadSuccess: Boolean = false,
    val error: String? = null,
)

val TAX_TYPES = listOf("", "USTax", "Apr", "K1", "1040")

@HiltViewModel
class UploadViewModel @Inject constructor(
    private val repository: DocumentRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(UploadUiState())
    val uiState: StateFlow<UploadUiState> = _uiState.asStateFlow()

    init {
        loadMetadataOptions()
    }

    private fun loadMetadataOptions() {
        viewModelScope.launch {
            // Load all metadata options in parallel
            launch {
                when (val result = repository.listCompanies()) {
                    is NetworkResult.Success ->
                        _uiState.value = _uiState.value.copy(companies = result.data)
                    else -> {}
                }
            }
            launch {
                when (val result = repository.listHolders()) {
                    is NetworkResult.Success ->
                        _uiState.value = _uiState.value.copy(holders = result.data)
                    else -> {}
                }
            }
            launch {
                when (val result = repository.listDocumentTypes()) {
                    is NetworkResult.Success ->
                        _uiState.value = _uiState.value.copy(documentTypes = result.data)
                    else -> {}
                }
            }
        }
    }

    fun setSelectedFile(uri: Uri, contentResolver: ContentResolver) {
        val cursor = contentResolver.query(uri, null, null, null, null)
        var filename = "file"
        cursor?.use {
            if (it.moveToFirst()) {
                val nameIndex = it.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (nameIndex >= 0) {
                    filename = it.getString(nameIndex)
                }
            }
        }
        val mimeType = contentResolver.getType(uri) ?: "application/octet-stream"

        _uiState.value = _uiState.value.copy(
            selectedUri = uri,
            selectedFilename = filename,
            selectedMimeType = mimeType,
            uploadSuccess = false,
            error = null
        )
    }

    fun setCameraFile(uri: Uri, mimeType: String) {
        val timestamp = java.text.SimpleDateFormat("yyyyMMdd_HHmmss", java.util.Locale.US)
            .format(java.util.Date())
        _uiState.value = _uiState.value.copy(
            selectedUri = uri,
            selectedFilename = "DOC_${timestamp}.jpg",
            selectedMimeType = mimeType,
            uploadSuccess = false,
            error = null
        )
    }

    fun setYear(v: String) { _uiState.value = _uiState.value.copy(year = v.take(4)) }
    fun setMonth(v: String) { _uiState.value = _uiState.value.copy(month = v.take(2)) }
    fun setDay(v: String) { _uiState.value = _uiState.value.copy(day = v.take(2)) }
    fun setCompany(v: String) { _uiState.value = _uiState.value.copy(company = v) }
    fun setHolder(v: String) { _uiState.value = _uiState.value.copy(holder = v) }
    fun setDocumentType(v: String) { _uiState.value = _uiState.value.copy(documentType = v) }
    fun setPurpose(v: String) { _uiState.value = _uiState.value.copy(purpose = v) }
    fun setAccountNumber(v: String) { _uiState.value = _uiState.value.copy(accountNumber = v) }
    fun setTaxType(v: String) { _uiState.value = _uiState.value.copy(taxType = v) }
    fun setWintonDisclosure(v: Boolean) { _uiState.value = _uiState.value.copy(wintonDisclosure = v) }
    fun setUstaxAccountClosing(v: Boolean) { _uiState.value = _uiState.value.copy(ustaxAccountClosing = v) }
    fun setUstaxOpening(v: Boolean) { _uiState.value = _uiState.value.copy(ustaxOpening = v) }
    fun setNotes(v: String) { _uiState.value = _uiState.value.copy(notes = v) }

    fun generatePreviewFilename(): String {
        val s = _uiState.value
        val ext = s.selectedFilename.substringAfterLast('.', "pdf")

        val datePart = buildString {
            if (s.year.isNotBlank()) append(s.year)
            if (s.month.isNotBlank()) append(s.month.padStart(2, '0'))
            if (s.day.isNotBlank()) append(s.day.padStart(2, '0'))
        }

        val parts = when {
            s.wintonDisclosure -> listOf(datePart, "WintonDisclosure", s.holder, s.company, s.accountNumber)
            s.taxType.isNotBlank() -> listOf("${s.taxType}${datePart}", s.holder, s.company, s.documentType, s.purpose, s.accountNumber)
            else -> listOf(datePart, s.holder, s.company, s.documentType, s.purpose, s.accountNumber)
        }

        val name = parts
            .filter { it.isNotBlank() }
            .joinToString("_")
            .replace(Regex("_+"), "_")
            .trimStart('_')
            .trimEnd('_')

        return if (name.isNotBlank()) "$name.$ext" else s.selectedFilename
    }

    fun upload(contentResolver: ContentResolver) {
        val state = _uiState.value
        val uri = state.selectedUri ?: return

        _uiState.value = state.copy(isUploading = true, error = null)

        viewModelScope.launch {
            try {
                val inputStream = contentResolver.openInputStream(uri)
                val bytes = inputStream?.readBytes() ?: throw Exception("Cannot read file")
                inputStream.close()

                val metadata = buildMetadataMap()

                when (val result = repository.uploadDocument(
                    fileBytes = bytes,
                    filename = generatePreviewFilename(),
                    mimeType = state.selectedMimeType,
                    metadata = metadata,
                )) {
                    is NetworkResult.Success -> {
                        _uiState.value = _uiState.value.copy(
                            isUploading = false,
                            uploadSuccess = true
                        )
                    }
                    is NetworkResult.Error -> {
                        _uiState.value = _uiState.value.copy(
                            isUploading = false,
                            error = result.message
                        )
                    }
                    is NetworkResult.Loading -> {}
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isUploading = false,
                    error = e.message ?: "Upload failed"
                )
            }
        }
    }

    private fun buildMetadataMap(): Map<String, Any> {
        val s = _uiState.value
        val map = mutableMapOf<String, Any>()

        if (s.company.isNotBlank()) map["company"] = s.company
        if (s.holder.isNotBlank()) map["holder"] = s.holder
        if (s.documentType.isNotBlank()) map["documentType"] = s.documentType
        if (s.purpose.isNotBlank()) map["purpose"] = s.purpose
        if (s.accountNumber.isNotBlank()) map["accountNumber"] = s.accountNumber
        if (s.year.isNotBlank()) map["year"] = s.year
        if (s.month.isNotBlank()) map["month"] = s.month
        if (s.day.isNotBlank()) map["day"] = s.day
        if (s.taxType.isNotBlank()) map["taxType"] = s.taxType
        if (s.notes.isNotBlank()) map["notes"] = s.notes
        if (s.wintonDisclosure) map["wintonDisclosure"] = true
        if (s.ustaxAccountClosing) map["ustaxAccountClosing"] = true
        if (s.ustaxOpening) map["ustaxOpening"] = true

        return map
    }

    fun reset() {
        _uiState.value = UploadUiState(
            companies = _uiState.value.companies,
            holders = _uiState.value.holders,
            documentTypes = _uiState.value.documentTypes,
        )
    }
}
