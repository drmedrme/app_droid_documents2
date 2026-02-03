package com.documents.app.data.api

import com.documents.app.data.model.DocumentDetail
import com.documents.app.data.model.DocumentSummary
import com.documents.app.data.model.DocumentVersion
import com.documents.app.data.model.MetadataListItem
import com.documents.app.data.model.SearchResult
import com.documents.app.data.model.V2Response
import okhttp3.MultipartBody
import okhttp3.RequestBody
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.*

interface DocumentsApiService {

    @GET("/api/v2/documents/")
    suspend fun listDocuments(
        @Query("page") page: Int = 1,
        @Query("per_page") perPage: Int = 20,
        @Query("category") category: String? = null,
    ): Response<V2Response<List<DocumentSummary>>>

    @GET("/api/v2/documents/{id}")
    suspend fun getDocument(@Path("id") id: String): Response<V2Response<DocumentDetail>>

    @Streaming
    @GET("/api/v2/documents/{id}/download")
    suspend fun downloadDocument(@Path("id") id: String): Response<ResponseBody>

    @Multipart
    @POST("/api/v2/documents/upload")
    suspend fun uploadDocument(
        @Part file: MultipartBody.Part,
        @Part("metadata") metadata: RequestBody? = null,
    ): Response<V2Response<DocumentDetail>>

    @GET("/api/v2/search/")
    suspend fun search(
        @Query("q") query: String,
        @Query("page") page: Int = 1,
        @Query("size") size: Int = 20,
        @Query("searchMode") searchMode: String = "hybrid",
        @Query("companies") companies: String? = null,
        @Query("holders") holders: String? = null,
        @Query("documentTypes") documentTypes: String? = null,
        @Query("taxTypes") taxTypes: String? = null,
        @Query("years") years: String? = null,
        @Query("fileTypes") fileTypes: String? = null,
    ): Response<V2Response<List<SearchResult>>>

    @PATCH("/api/v2/documents/{id}")
    suspend fun updateDocument(
        @Path("id") id: String,
        @Body metadata: Map<String, @JvmSuppressWildcards Any?>,
    ): Response<V2Response<DocumentDetail>>

    @DELETE("/api/v2/documents/{id}")
    suspend fun deleteDocument(@Path("id") id: String): Response<V2Response<Any>>

    @GET("/api/v2/documents/{id}/versions")
    suspend fun getVersions(@Path("id") id: String): Response<V2Response<List<DocumentVersion>>>

    @GET("/api/v2/documents/status")
    suspend fun status(): Response<V2Response<Any>>

    // Metadata lists (v1 endpoints)
    @GET("/api/v1/metadata/companies")
    suspend fun listCompanies(): Response<List<MetadataListItem>>

    @GET("/api/v1/metadata/holders")
    suspend fun listHolders(): Response<List<MetadataListItem>>

    @GET("/api/v1/metadata/document-types")
    suspend fun listDocumentTypes(): Response<List<MetadataListItem>>
}
