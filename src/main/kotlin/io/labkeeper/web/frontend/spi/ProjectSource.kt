package io.labkeeper.web.frontend.spi

/** Projects the public pages show. The application that hosts the frontend provides the implementation. */
interface ProjectSource {
    suspend fun previewProjects(): List<PreviewProject>

    suspend fun title(projectId: String): String
}

data class PreviewProject(
    val id: String,
    val nameRu: String,
    val nameEn: String,
    val category: String,
    val logoUrl: String?,
)
