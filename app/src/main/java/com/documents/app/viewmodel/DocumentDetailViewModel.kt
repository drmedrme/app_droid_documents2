package com.documents.app.viewmodel

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.documents.app.data.api.NetworkResult
import com.documents.app.data.model.DocumentDetail
import com.documents.app.data.model.DocumentVersion
import com.documents.app.data.model.MetadataListItem
import com.documents.app.data.preferences.SettingsPreferences
import com.documents.app.data.repository.DocumentRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject

data class DocumentDetailUiState(
    val document: DocumentDetail? = null,
    val versions: List<DocumentVersion> = emptyList(),
    val isLoading: Boolean = false,
    val isSharing: Boolean = false,
    val isOpening: Boolean = false,
    val isDeleting: Boolean = false,
    val isSaving: Boolean = false,
    val error: String? = null,
    val actionSuccess: String? = null,
    val deleted: Boolean = false,
    // Edit mode
    val isEditing: Boolean = false,
    val editYear: String = "",
    val editMonth: String = "",
    val editDay: String = "",
    val editCompany: String = "",
    val editHolder: String = "",
    val editDocumentType: String = "",
    val editPurpose: String = "",
    val editAccountNumber: String = "",
    val editTaxType: String = "",
    val editWintonDisclosure: Boolean = false,
    val editUstaxAccountClosing: Boolean = false,
    val editUstaxOpening: Boolean = false,
    val editNotes: String = "",
    // Metadata options
    val companies: List<MetadataListItem> = emptyList(),
    val holders: List<MetadataListItem> = emptyList(),
    val documentTypes: List<MetadataListItem> = emptyList(),
)

@HiltViewModel
class DocumentDetailViewModel @Inject constructor(
    private val repository: DocumentRepository,
    settingsPreferences: SettingsPreferences,
) : ViewModel() {

    private val _uiState = MutableStateFlow(DocumentDetailUiState())
    val uiState: StateFlow<DocumentDetailUiState> = _uiState.asStateFlow()

    val apiBaseUrl = settingsPreferences.apiBaseUrl.stateIn(
        viewModelScope, SharingStarted.Eagerly, ""
    )

    init {
        loadMetadataOptions()
    }

    private fun loadMetadataOptions() {
        viewModelScope.launch {
            launch {
                when (val r = repository.listCompanies()) {
                    is NetworkResult.Success -> _uiState.value = _uiState.value.copy(companies = r.data)
                    else -> {}
                }
            }
            launch {
                when (val r = repository.listHolders()) {
                    is NetworkResult.Success -> _uiState.value = _uiState.value.copy(holders = r.data)
                    else -> {}
                }
            }
            launch {
                when (val r = repository.listDocumentTypes()) {
                    is NetworkResult.Success -> _uiState.value = _uiState.value.copy(documentTypes = r.data)
                    else -> {}
                }
            }
        }
    }

    fun loadDocument(documentId: String) {
        _uiState.value = _uiState.value.copy(isLoading = true, error = null)

        viewModelScope.launch {
            when (val result = repository.getDocument(documentId)) {
                is NetworkResult.Success -> {
                    _uiState.value = _uiState.value.copy(
                        document = result.data,
                        isLoading = false
                    )
                    loadVersions(documentId)
                }
                is NetworkResult.Error -> {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = result.message
                    )
                }
                is NetworkResult.Loading -> {}
            }
        }
    }

    private fun loadVersions(documentId: String) {
        viewModelScope.launch {
            when (val result = repository.getVersions(documentId)) {
                is NetworkResult.Success -> {
                    _uiState.value = _uiState.value.copy(versions = result.data)
                }
                else -> {}
            }
        }
    }

    // --- Edit mode ---

    fun startEditing() {
        val doc = _uiState.value.document ?: return
        val meta = doc.metadata
        _uiState.value = _uiState.value.copy(
            isEditing = true,
            editYear = meta?.year ?: "",
            editMonth = meta?.month ?: "",
            editDay = meta?.day ?: "",
            editCompany = meta?.company ?: "",
            editHolder = meta?.holder ?: "",
            editDocumentType = meta?.documentType ?: "",
            editPurpose = meta?.purpose ?: "",
            editAccountNumber = meta?.accountNumber ?: "",
            editTaxType = meta?.taxType ?: "",
            editWintonDisclosure = meta?.wintonDisclosure ?: false,
            editUstaxAccountClosing = meta?.ustaxAccountClosing ?: false,
            editUstaxOpening = meta?.ustaxOpening ?: false,
            editNotes = meta?.notes ?: "",
        )
    }

    fun cancelEditing() {
        _uiState.value = _uiState.value.copy(isEditing = false)
    }

    fun setEditYear(v: String) { _uiState.value = _uiState.value.copy(editYear = v.take(4)) }
    fun setEditMonth(v: String) { _uiState.value = _uiState.value.copy(editMonth = v.take(2)) }
    fun setEditDay(v: String) { _uiState.value = _uiState.value.copy(editDay = v.take(2)) }
    fun setEditCompany(v: String) { _uiState.value = _uiState.value.copy(editCompany = v) }
    fun setEditHolder(v: String) { _uiState.value = _uiState.value.copy(editHolder = v) }
    fun setEditDocumentType(v: String) { _uiState.value = _uiState.value.copy(editDocumentType = v) }
    fun setEditPurpose(v: String) { _uiState.value = _uiState.value.copy(editPurpose = v) }
    fun setEditAccountNumber(v: String) { _uiState.value = _uiState.value.copy(editAccountNumber = v) }
    fun setEditTaxType(v: String) { _uiState.value = _uiState.value.copy(editTaxType = v) }
    fun setEditWintonDisclosure(v: Boolean) { _uiState.value = _uiState.value.copy(editWintonDisclosure = v) }
    fun setEditUstaxAccountClosing(v: Boolean) { _uiState.value = _uiState.value.copy(editUstaxAccountClosing = v) }
    fun setEditUstaxOpening(v: Boolean) { _uiState.value = _uiState.value.copy(editUstaxOpening = v) }
    fun setEditNotes(v: String) { _uiState.value = _uiState.value.copy(editNotes = v) }

    fun generatePreviewFilename(): String {
        val s = _uiState.value
        val doc = s.document ?: return ""
        val ext = (doc.originalFilename ?: doc.filename).substringAfterLast('.', "pdf")

        val datePart = buildString {
            if (s.editYear.isNotBlank()) append(s.editYear)
            if (s.editMonth.isNotBlank()) append(s.editMonth.padStart(2, '0'))
            if (s.editDay.isNotBlank()) append(s.editDay.padStart(2, '0'))
        }

        val parts = when {
            s.editWintonDisclosure -> listOf(datePart, "WintonDisclosure", s.editHolder, s.editCompany, s.editAccountNumber)
            s.editTaxType.isNotBlank() -> listOf("${s.editTaxType}${datePart}", s.editHolder, s.editCompany, s.editDocumentType, s.editPurpose, s.editAccountNumber)
            else -> listOf(datePart, s.editHolder, s.editCompany, s.editDocumentType, s.editPurpose, s.editAccountNumber)
        }

        val name = parts
            .filter { it.isNotBlank() }
            .joinToString("_")
            .replace(Regex("_+"), "_")
            .trimStart('_')
            .trimEnd('_')

        return if (name.isNotBlank()) "$name.$ext" else (doc.originalFilename ?: doc.filename)
    }

    fun saveAndRename() {
        val doc = _uiState.value.document ?: return
        val s = _uiState.value
        _uiState.value = s.copy(isSaving = true, error = null)

        val metadata = mutableMapOf<String, Any?>()
        metadata["company"] = s.editCompany.ifBlank { null }
        metadata["holder"] = s.editHolder.ifBlank { null }
        metadata["documentType"] = s.editDocumentType.ifBlank { null }
        metadata["purpose"] = s.editPurpose.ifBlank { null }
        metadata["accountNumber"] = s.editAccountNumber.ifBlank { null }
        metadata["year"] = s.editYear.ifBlank { null }
        metadata["month"] = s.editMonth.ifBlank { null }
        metadata["day"] = s.editDay.ifBlank { null }
        metadata["taxType"] = s.editTaxType.ifBlank { null }
        metadata["notes"] = s.editNotes.ifBlank { null }
        metadata["wintonDisclosure"] = s.editWintonDisclosure
        metadata["ustaxAccountClosing"] = s.editUstaxAccountClosing
        metadata["ustaxOpening"] = s.editUstaxOpening
        metadata["originalFilename"] = generatePreviewFilename()

        viewModelScope.launch {
            when (val result = repository.updateDocument(doc.id, metadata)) {
                is NetworkResult.Success -> {
                    _uiState.value = _uiState.value.copy(
                        document = result.data,
                        isSaving = false,
                        isEditing = false,
                        actionSuccess = "Saved & renamed"
                    )
                }
                is NetworkResult.Error -> {
                    _uiState.value = _uiState.value.copy(
                        isSaving = false,
                        error = result.message
                    )
                }
                is NetworkResult.Loading -> {}
            }
        }
    }

    // --- Actions ---

    fun shareDocument(context: Context) {
        val doc = _uiState.value.document ?: return
        _uiState.value = _uiState.value.copy(isSharing = true)

        viewModelScope.launch {
            when (val result = repository.downloadDocument(doc.id)) {
                is NetworkResult.Success -> {
                    val cacheDir = File(context.cacheDir, "shared_documents")
                    cacheDir.mkdirs()
                    val file = File(cacheDir, doc.originalFilename ?: doc.filename)
                    file.writeBytes(result.data)

                    val uri = FileProvider.getUriForFile(
                        context,
                        "${context.packageName}.provider",
                        file
                    )
                    val intent = Intent(Intent.ACTION_SEND).apply {
                        type = doc.mimeType ?: "application/octet-stream"
                        putExtra(Intent.EXTRA_STREAM, uri)
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    }
                    context.startActivity(Intent.createChooser(intent, "Share Document"))
                    _uiState.value = _uiState.value.copy(isSharing = false)
                }
                is NetworkResult.Error -> {
                    _uiState.value = _uiState.value.copy(
                        isSharing = false,
                        error = result.message
                    )
                }
                is NetworkResult.Loading -> {}
            }
        }
    }

    fun openDocument(context: Context) {
        val doc = _uiState.value.document ?: return
        _uiState.value = _uiState.value.copy(isOpening = true)

        viewModelScope.launch {
            when (val result = repository.downloadDocument(doc.id)) {
                is NetworkResult.Success -> {
                    val cacheDir = File(context.cacheDir, "shared_documents")
                    cacheDir.mkdirs()
                    val file = File(cacheDir, doc.originalFilename ?: doc.filename)
                    file.writeBytes(result.data)

                    val uri = FileProvider.getUriForFile(
                        context,
                        "${context.packageName}.provider",
                        file
                    )
                    val intent = Intent(Intent.ACTION_VIEW).apply {
                        setDataAndType(uri, doc.mimeType ?: "application/octet-stream")
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    }
                    try {
                        context.startActivity(intent)
                    } catch (e: Exception) {
                        val chooser = Intent.createChooser(intent, "Open with")
                        context.startActivity(chooser)
                    }
                    _uiState.value = _uiState.value.copy(isOpening = false)
                }
                is NetworkResult.Error -> {
                    _uiState.value = _uiState.value.copy(
                        isOpening = false,
                        error = result.message
                    )
                }
                is NetworkResult.Loading -> {}
            }
        }
    }

    fun deleteDocument() {
        val doc = _uiState.value.document ?: return
        _uiState.value = _uiState.value.copy(isDeleting = true, error = null)

        viewModelScope.launch {
            when (val result = repository.deleteDocument(doc.id)) {
                is NetworkResult.Success -> {
                    _uiState.value = _uiState.value.copy(
                        isDeleting = false,
                        deleted = true,
                        actionSuccess = "Document deleted"
                    )
                }
                is NetworkResult.Error -> {
                    _uiState.value = _uiState.value.copy(
                        isDeleting = false,
                        error = result.message
                    )
                }
                is NetworkResult.Loading -> {}
            }
        }
    }

    fun clearActionResult() {
        _uiState.value = _uiState.value.copy(actionSuccess = null, error = null)
    }
}
