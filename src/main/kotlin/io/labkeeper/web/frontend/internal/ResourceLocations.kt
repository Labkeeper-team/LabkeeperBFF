package io.labkeeper.web.frontend.internal

import io.labkeeper.web.frontend.FrontendProperties
import org.springframework.core.io.ClassPathResource
import org.springframework.core.io.FileSystemResource
import org.springframework.core.io.Resource
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.server.ServerRequest
import reactor.core.publisher.Mono
import java.nio.file.Files
import java.nio.file.Path

@Component
class ResourceLocations(
    properties: FrontendProperties,
) {
    private val sourceStatic: Resource? = resourcesDirectory(properties.staticDirectory)
        ?.resolve("static")
        ?.takeIf { Files.isDirectory(it) }
        ?.let { FileSystemResource(withTrailingSlash(it)) }

    fun findStatic(request: ServerRequest): Mono<Resource> {
        val relative = relativePath(request.path())
        if (!isSafeRelativePath(relative)) {
            return Mono.empty()
        }
        return Mono.justOrEmpty(locate(relative))
    }

    fun templatesDirectory(): Path? = resourcesDirectory(configured)?.resolve("templates")?.takeIf {
        Files.isRegularFile(it.resolve("index.html"))
    }

    private val configured: String = properties.staticDirectory

    private fun locate(relativePath: String): Resource? {
        val fileResource = sourceStatic?.let { root ->
            try {
                root.createRelative(relativePath).takeIf { it.isReadable }
            } catch (_: Exception) {
                null
            }
        }
        if (fileResource != null) {
            return fileResource
        }
        return try {
            CLASSPATH_STATIC.createRelative(relativePath).takeIf { it.isReadable }
        } catch (_: Exception) {
            null
        }
    }

    companion object {
        private val CLASSPATH_STATIC = ClassPathResource("static/")

        fun resourcesDirectory(configured: String): Path? {
            val value = configured.trim()
            if (value.equals("off", ignoreCase = true)) {
                return null
            }
            if (value.isNotEmpty() && !value.equals("auto", ignoreCase = true)) {
                val path = Path.of(value)
                val resolved = if (path.isAbsolute) path else Path.of("").toAbsolutePath().resolve(path)
                return resolved.takeIf { Files.isDirectory(it) }
            }
            val workingDirectory = Path.of("").toAbsolutePath()
            return listOf(
                workingDirectory.resolve("src").resolve("main").resolve("resources"),
                workingDirectory.resolve("Web").resolve("Frontend").resolve("src").resolve("main").resolve("resources"),
            ).firstOrNull { Files.isRegularFile(it.resolve("templates").resolve("index.html")) }
        }

        private fun relativePath(path: String): String {
            if (path.isBlank() || path == "/") {
                return ""
            }
            return if (path.startsWith("/")) path.substring(1) else path
        }

        private fun isSafeRelativePath(path: String): Boolean {
            if (path.isBlank()) {
                return false
            }
            val normalized = path.replace('\\', '/')
            return !normalized.startsWith("/") && !normalized.contains("..") && !normalized.contains("//")
        }

        private fun withTrailingSlash(directory: Path): String {
            val location = directory.toAbsolutePath().toString().replace('\\', '/')
            return if (location.endsWith("/")) location else "$location/"
        }
    }
}
