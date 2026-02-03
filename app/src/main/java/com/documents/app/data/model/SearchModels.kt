package com.documents.app.data.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class SearchResult(
    @Json(name = "id") val id: String,
    @Json(name = "filename") val filename: String,
    @Json(name = "originalFilename") val originalFilename: String? = null,
    @Json(name = "mimeType") val mimeType: String? = null,
    @Json(name = "size") val size: Long? = null,
    @Json(name = "storagePath") val storagePath: String? = null,
    @Json(name = "metadata") val metadata: DocumentMetadata? = null,
    @Json(name = "score") val score: Double? = null,
    @Json(name = "highlight") val highlight: Map<String, List<String>>? = null,
    @Json(name = "createdAt") val createdAt: String? = null,
    @Json(name = "uploadedAt") val uploadedAt: String? = null,
) {
    val category: String? get() = metadata?.category

    /** First highlight snippet with HTML tags stripped. */
    val highlightText: String? get() {
        val snippets = highlight?.values?.firstOrNull() ?: return null
        val first = snippets.firstOrNull() ?: return null
        return first.replace(Regex("</?mark>"), "").trim()
    }
}
