package io.labkeeper.web.frontend.internal

import io.netty.handler.codec.http.HttpHeaderNames
import org.springframework.boot.web.embedded.netty.NettyServerCustomizer
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.util.MimeTypeUtils

/**
 * Pages and text files of the site go out compressed: the editor alone is about 10 MB as is and about 2.8 MB
 * with gzip. The API is left as it was. Netty skips clients without Accept-Encoding, and nginx in front
 * does not compress a response twice.
 *
 * The customizer runs after `server.compression`, so with that property on the API would stop being compressed:
 * the property and this bean are not meant to be used together.
 */
@Configuration
class StaticFileCompressionConfig {
    @Bean
    fun staticFileCompression() = NettyServerCustomizer { server ->
        server.compress(MIN_COMPRESSED_SIZE).compress { request, response ->
            !request.uri().startsWith(API_PREFIX) &&
                isCompressible(response.responseHeaders().get(HttpHeaderNames.CONTENT_TYPE))
        }
    }

    private companion object {
        const val API_PREFIX = "/api/"

        /** Smaller responses gain less than the gzip header costs. */
        const val MIN_COMPRESSED_SIZE = 1024

        /** Fonts in woff2 and images other than svg are compressed already. */
        val COMPRESSIBLE_TYPES = listOf(
            "text/html",
            "text/css",
            "text/javascript",
            "application/javascript",
            "application/json",
            "text/plain",
            "text/xml",
            "application/xml",
            "image/svg+xml",
            "font/ttf",
            "font/otf",
        ).map(MimeTypeUtils::parseMimeType)

        fun isCompressible(contentType: String?): Boolean {
            val type = contentType?.let { runCatching { MimeTypeUtils.parseMimeType(it) }.getOrNull() } ?: return false
            return COMPRESSIBLE_TYPES.any { it.equalsTypeAndSubtype(type) }
        }
    }
}
