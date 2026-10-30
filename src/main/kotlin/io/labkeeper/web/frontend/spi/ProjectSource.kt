package io.labkeeper.web.frontend.spi

/** Project data the public pages cannot load themselves. The host application provides the implementation. */
interface ProjectSource {
    suspend fun title(projectId: String): String
}
