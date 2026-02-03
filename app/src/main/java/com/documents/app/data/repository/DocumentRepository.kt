package com.documents.app.data.repository

import com.documents.app.data.api.NetworkResult
import com.documents.app.data.model.DocumentDetail
import com.documents.app.data.model.DocumentSummary
import com.documents.app.data.model.DocumentVersion
import com.documents.app.data.model.MetadataListItem
import com.documents.app.data.model.SearchResult
import com.documents.app.data.model.V2Meta

interface DocumentRepository {
    suspend fun listDocuments(page: Int = 1, perPage: Int = 20, category: String? = null): NetworkResult<Pair<List<DocumentSummary>, V2Meta?>>
    suspend fun getDocument(id: String): NetworkResult<DocumentDetail>
    suspend fun downloadDocument(id: String): NetworkResult<ByteArray>
    suspend fun uploadDocument(fileBytes: ByteArray, filename: String, mimeType: String, metadata: Map<String, Any>): NetworkResult<DocumentDetail>
    suspend fun search(
        query: String,
        page: Int = 1,
        size: Int = 20,
        filters: Map<String, String> = emptyMap(),
    ): NetworkResult<Pair<List<SearchResult>, V2Meta?>>
    suspend fun updateDocument(id: String, metadata: Map<String, Any?>): NetworkResult<DocumentDetail>
    suspend fun deleteDocument(id: String): NetworkResult<Unit>
    suspend fun getVersions(id: String): NetworkResult<List<DocumentVersion>>
    suspend fun checkStatus(): NetworkResult<Unit>
    suspend fun listCompanies(): NetworkResult<List<MetadataListItem>>
    suspend fun listHolders(): NetworkResult<List<MetadataListItem>>
    suspend fun listDocumentTypes(): NetworkResult<List<MetadataListItem>>
}
