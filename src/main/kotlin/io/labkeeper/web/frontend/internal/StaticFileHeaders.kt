package io.labkeeper.web.frontend.internal

import org.springframework.core.io.ClassPathResource
import org.springframework.core.io.Resource
import org.springframework.http.CacheControl
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.util.DigestUtils
import java.time.Duration
import java.util.concurrent.ConcurrentHashMap

/**
 * Cache headers for files served as they are. Without them Spring Security writes `no-store`,
 * and every full load of the editor downloads about 10 MB again. Its header writer leaves a response
 * alone once it has a Cache-Control of its own. HTML pages are not files and keep `no-store`.
 */
object StaticFileHeaders {
    /** Files of the editor location: build output in `assets/`, the rest copied from the editor's public folder. */
    fun editorFile(resource: Resource, headers: HttpHeaders) {
        if (isHashedBuildFile(resource)) {
            hashedFile(resource, headers)
        } else {
            revalidatedFile(resource, headers)
        }
    }

    /**
     * Landing scripts, styles and images, MathJax and the wiki keep their names across releases.
     * The browser checks them on every use and gets an empty 304 while the content is the same.
     * Jar entries carry no real modification time (Gradle builds reproducible archives), so the check uses a hash.
     */
    fun revalidatedFile(resource: Resource, headers: HttpHeaders) {
        commonHeaders(resource, headers)
        headers.cacheControl = CacheControl.noCache().headerValue
        etagOf(resource)?.let { headers.eTag = it }
    }

    /** A release gives the file a new name and new pages link to that name, so the old one may stay for good. */
    private fun hashedFile(resource: Resource, headers: HttpHeaders) {
        commonHeaders(resource, headers)
        headers.cacheControl = CacheControl.maxAge(Duration.ofDays(DAYS_IN_YEAR)).cachePublic().immutable().headerValue
    }

    private fun commonHeaders(resource: Resource, headers: HttpHeaders) {
        headers.vary = listOf(HttpHeaders.ACCEPT_ENCODING)
        // Spell check dictionaries are text, but the default types say text/x-c and application/octet-stream.
        val name = resource.filename.orEmpty()
        if (DICTIONARY_EXTENSIONS.any { name.endsWith(it) }) {
            headers.contentType = MediaType.TEXT_PLAIN
        }
    }

    private fun isHashedBuildFile(resource: Resource): Boolean {
        val path = (resource as? ClassPathResource)?.path ?: return false
        return path.startsWith(EDITOR_BUILD_DIR) && HASHED_NAME.matches(path.removePrefix(EDITOR_BUILD_DIR))
    }

    /** Keyed by time and size as well, so a file edited under bootRun gets a new tag. */
    private fun etagOf(resource: Resource): String? = runCatching {
        val key = "${resource.url}|${resource.lastModified()}|${resource.contentLength()}"
        etags.getOrPut(key) {
            // Weak: compression changes the bytes on the wire but not the content.
            resource.inputStream.use { "W/\"${DigestUtils.md5DigestAsHex(it)}\"" }
        }
    }.getOrNull()

    private val etags = ConcurrentHashMap<String, String>()

    private const val DAYS_IN_YEAR = 365L
    private const val EDITOR_BUILD_DIR = "editor/assets/"

    /** Vite names build files `name-HASH.ext` with an eight character hash, right in `assets/`. */
    private val HASHED_NAME = Regex("[^/]+-[A-Za-z0-9_-]{8}\\.[A-Za-z0-9]+")
    private val DICTIONARY_EXTENSIONS = listOf(".dic", ".aff")
}
