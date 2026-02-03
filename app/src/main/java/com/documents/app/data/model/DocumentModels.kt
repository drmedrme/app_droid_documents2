package com.documents.app.data.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class V2Response<T>(
    @Json(name = "success") val success: Boolean,
    @Json(name = "data") val data: T?,
    @Json(name = "meta") val meta: V2Meta? = null,
)

@JsonClass(generateAdapter = true)
data class V2Meta(
    @Json(name = "page") val page: Int? = null,
    @Json(name = "per_page") val perPage: Int? = null,
    @Json(name = "total") val total: Int? = null,
    @Json(name = "pages") val pages: Int? = null,
    @Json(name = "aggregations") val aggregations: Map<String, AggregationResult>? = null,
)

@JsonClass(generateAdapter = true)
data class AggregationResult(
    @Json(name = "buckets") val buckets: List<AggregationBucket> = emptyList(),
)

@JsonClass(generateAdapter = true)
data class AggregationBucket(
    @Json(name = "key") val key: String,
    @Json(name = "doc_count") val docCount: Int = 0,
)

@JsonClass(generateAdapter = true)
data class DocumentMetadata(
    @Json(name = "category") val category: String? = null,
    @Json(name = "confidentialityLevel") val confidentialityLevel: String? = null,
    @Json(name = "notes") val notes: String? = null,
    @Json(name = "company") val company: String? = null,
    @Json(name = "holder") val holder: String? = null,
    @Json(name = "documentType") val documentType: String? = null,
    @Json(name = "year") val year: String? = null,
    @Json(name = "month") val month: String? = null,
    @Json(name = "day") val day: String? = null,
    @Json(name = "purpose") val purpose: String? = null,
    @Json(name = "accountNumber") val accountNumber: String? = null,
    @Json(name = "taxType") val taxType: String? = null,
    @Json(name = "wintonDisclosure") val wintonDisclosure: Boolean? = null,
    @Json(name = "ustaxAccountClosing") val ustaxAccountClosing: Boolean? = null,
    @Json(name = "ustaxOpening") val ustaxOpening: Boolean? = null,
)

@JsonClass(generateAdapter = true)
data class DocumentSummary(
    @Json(name = "id") val id: String,
    @Json(name = "filename") val filename: String,
    @Json(name = "originalFilename") val originalFilename: String? = null,
    @Json(name = "size") val size: Long? = null,
    @Json(name = "mimeType") val mimeType: String? = null,
    @Json(name = "storagePath") val storagePath: String? = null,
    @Json(name = "uploadedAt") val uploadedAt: String? = null,
    @Json(name = "createdAt") val createdAt: String? = null,
    @Json(name = "modifiedAt") val modifiedAt: String? = null,
    @Json(name = "metadata") val metadata: DocumentMetadata? = null,
    @Json(name = "deleted") val deleted: Boolean? = null,
) {
    val category: String? get() = metadata?.category
    val confidentialityLevel: String? get() = metadata?.confidentialityLevel
    val notes: String? get() = metadata?.notes
}

@JsonClass(generateAdapter = true)
data class DocumentDetail(
    @Json(name = "id") val id: String,
    @Json(name = "filename") val filename: String,
    @Json(name = "originalFilename") val originalFilename: String? = null,
    @Json(name = "size") val size: Long? = null,
    @Json(name = "mimeType") val mimeType: String? = null,
    @Json(name = "storagePath") val storagePath: String? = null,
    @Json(name = "uploadedAt") val uploadedAt: String? = null,
    @Json(name = "createdAt") val createdAt: String? = null,
    @Json(name = "modifiedAt") val modifiedAt: String? = null,
    @Json(name = "metadata") val metadata: DocumentMetadata? = null,
    @Json(name = "currentVersion") val currentVersion: Int? = null,
    @Json(name = "deleted") val deleted: Boolean? = null,
    @Json(name = "indexed") val indexed: Boolean? = null,
    @Json(name = "textContent") val textContent: String? = null,
) {
    val category: String? get() = metadata?.category
    val confidentialityLevel: String? get() = metadata?.confidentialityLevel
    val notes: String? get() = metadata?.notes
    val company: String? get() = metadata?.company
    val holder: String? get() = metadata?.holder
    val documentType: String? get() = metadata?.documentType
}

@JsonClass(generateAdapter = true)
data class DocumentVersion(
    @Json(name = "id") val id: String,
    @Json(name = "versionNumber") val versionNumber: Int,
    @Json(name = "filename") val filename: String? = null,
    @Json(name = "size") val size: Long? = null,
    @Json(name = "createdAt") val createdAt: String? = null,
)

@JsonClass(generateAdapter = true)
data class MetadataListItem(
    @Json(name = "id") val id: String,
    @Json(name = "name") val name: String,
    @Json(name = "_count") val count: MetadataItemCount? = null,
)

@JsonClass(generateAdapter = true)
data class MetadataItemCount(
    @Json(name = "documents") val documents: Int = 0,
)
