package com.documents.app.data.repository

import com.documents.app.data.api.DocumentsApiService
import com.documents.app.data.api.NetworkResult
import com.documents.app.data.model.DocumentDetail
import com.documents.app.data.model.DocumentSummary
import com.documents.app.data.model.DocumentVersion
import com.documents.app.data.model.MetadataListItem
import com.documents.app.data.model.SearchResult
import com.documents.app.data.model.V2Meta
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DocumentRepositoryImpl @Inject constructor(
    private val api: DocumentsApiService
) : DocumentRepository {

    override suspend fun listDocuments(page: Int, perPage: Int, category: String?): NetworkResult<Pair<List<DocumentSummary>, V2Meta?>> {
        return safeApiCall {
            val response = api.listDocuments(page, perPage, category)
            if (response.isSuccessful) {
                val body = response.body()
                if (body?.success == true && body.data != null) {
                    NetworkResult.Success(Pair(body.data, body.meta))
                } else {
                    NetworkResult.Error("Failed to load documents")
                }
            } else {
                NetworkResult.Error("Error ${response.code()}: ${response.message()}", response.code())
            }
        }
    }

    override suspend fun getDocument(id: String): NetworkResult<DocumentDetail> {
        return safeApiCall {
            val response = api.getDocument(id)
            if (response.isSuccessful) {
                val body = response.body()
                if (body?.success == true && body.data != null) {
                    NetworkResult.Success(body.data)
                } else {
                    NetworkResult.Error("Document not found")
                }
            } else {
                NetworkResult.Error("Error ${response.code()}: ${response.message()}", response.code())
            }
        }
    }

    override suspend fun downloadDocument(id: String): NetworkResult<ByteArray> {
        return safeApiCall {
            val response = api.downloadDocument(id)
            if (response.isSuccessful) {
                val bytes = response.body()?.bytes()
                if (bytes != null) {
                    NetworkResult.Success(bytes)
                } else {
                    NetworkResult.Error("Empty response body")
                }
            } else {
                NetworkResult.Error("Download failed: ${response.code()}", response.code())
            }
        }
    }

    override suspend fun uploadDocument(
        fileBytes: ByteArray,
        filename: String,
        mimeType: String,
        metadata: Map<String, Any>,
    ): NetworkResult<DocumentDetail> {
        return safeApiCall {
            val requestFile = fileBytes.toRequestBody(mimeType.toMediaTypeOrNull())
            val filePart = MultipartBody.Part.createFormData("file", filename, requestFile)

            val metadataPart = if (metadata.isNotEmpty()) {
                JSONObject(metadata).toString()
                    .toRequestBody("text/plain".toMediaTypeOrNull())
            } else null

            val response = api.uploadDocument(filePart, metadataPart)
            if (response.isSuccessful) {
                val body = response.body()
                if (body?.success == true && body.data != null) {
                    NetworkResult.Success(body.data)
                } else {
                    NetworkResult.Error("Upload failed")
                }
            } else {
                NetworkResult.Error("Upload error ${response.code()}: ${response.message()}", response.code())
            }
        }
    }

    override suspend fun search(
        query: String,
        page: Int,
        size: Int,
        filters: Map<String, String>,
    ): NetworkResult<Pair<List<SearchResult>, V2Meta?>> {
        return safeApiCall {
            val response = api.search(
                query = query,
                page = page,
                size = size,
                companies = filters["companies"],
                holders = filters["holders"],
                documentTypes = filters["documentTypes"],
                taxTypes = filters["taxTypes"],
                years = filters["years"],
                fileTypes = filters["fileTypes"],
            )
            if (response.isSuccessful) {
                val body = response.body()
                if (body?.success == true && body.data != null) {
                    NetworkResult.Success(Pair(body.data, body.meta))
                } else {
                    NetworkResult.Error("Search failed")
                }
            } else {
                NetworkResult.Error("Search error ${response.code()}: ${response.message()}", response.code())
            }
        }
    }

    override suspend fun updateDocument(id: String, metadata: Map<String, Any?>): NetworkResult<DocumentDetail> {
        return safeApiCall {
            val response = api.updateDocument(id, metadata)
            if (response.isSuccessful) {
                val body = response.body()
                if (body?.success == true && body.data != null) {
                    NetworkResult.Success(body.data)
                } else {
                    NetworkResult.Error("Update failed")
                }
            } else {
                NetworkResult.Error("Update error ${response.code()}: ${response.message()}", response.code())
            }
        }
    }

    override suspend fun deleteDocument(id: String): NetworkResult<Unit> {
        return safeApiCall {
            val response = api.deleteDocument(id)
            if (response.isSuccessful) {
                NetworkResult.Success(Unit)
            } else {
                NetworkResult.Error("Delete failed: ${response.code()}", response.code())
            }
        }
    }

    override suspend fun getVersions(id: String): NetworkResult<List<DocumentVersion>> {
        return safeApiCall {
            val response = api.getVersions(id)
            if (response.isSuccessful) {
                val body = response.body()
                if (body?.success == true && body.data != null) {
                    NetworkResult.Success(body.data)
                } else {
                    NetworkResult.Error("Failed to load versions")
                }
            } else {
                NetworkResult.Error("Error ${response.code()}: ${response.message()}", response.code())
            }
        }
    }

    override suspend fun checkStatus(): NetworkResult<Unit> {
        return safeApiCall {
            val response = api.status()
            if (response.isSuccessful) {
                NetworkResult.Success(Unit)
            } else {
                NetworkResult.Error("Service unhealthy: ${response.code()}", response.code())
            }
        }
    }

    override suspend fun listCompanies(): NetworkResult<List<MetadataListItem>> {
        return safeApiCall {
            val response = api.listCompanies()
            if (response.isSuccessful) {
                NetworkResult.Success(response.body() ?: emptyList())
            } else {
                NetworkResult.Error("Failed to load companies: ${response.code()}", response.code())
            }
        }
    }

    override suspend fun listHolders(): NetworkResult<List<MetadataListItem>> {
        return safeApiCall {
            val response = api.listHolders()
            if (response.isSuccessful) {
                NetworkResult.Success(response.body() ?: emptyList())
            } else {
                NetworkResult.Error("Failed to load holders: ${response.code()}", response.code())
            }
        }
    }

    override suspend fun listDocumentTypes(): NetworkResult<List<MetadataListItem>> {
        return safeApiCall {
            val response = api.listDocumentTypes()
            if (response.isSuccessful) {
                NetworkResult.Success(response.body() ?: emptyList())
            } else {
                NetworkResult.Error("Failed to load document types: ${response.code()}", response.code())
            }
        }
    }

    private inline fun <T> safeApiCall(call: () -> NetworkResult<T>): NetworkResult<T> {
        return try {
            call()
        } catch (e: Exception) {
            NetworkResult.Error(e.message ?: "Network error")
        }
    }
}
