package io.labkeeper.web.frontend.internal.controller

import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.http.server.reactive.ServerHttpRequest
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController
import java.net.URI

@RestController
class FrontendRedirectController(
    @Value($$"${io.labkeeper.host}") private val host: String,
) {
    @GetMapping("/qr/{version}")
    fun qr(): ResponseEntity<Unit> = ResponseEntity.status(HttpStatus.FOUND).location(URI.create(host)).build()

    /** One address per page: `/blog/` and `/blog` would otherwise be two URLs, and the slashed one is a 404. */
    @GetMapping(
        "/{page:about|blog|templates|privacy|soglas|sogl_yam|sogl_adv|sogl_ds|oferta}/",
        "/blog/{slug}/",
        "/templates/{slug}/",
    )
    fun withoutTrailingSlash(request: ServerHttpRequest): ResponseEntity<Unit> {
        val path = request.path.value().trimEnd('/')
        val query = request.uri.rawQuery?.let { "?$it" }.orEmpty()
        return ResponseEntity.status(HttpStatus.MOVED_PERMANENTLY).location(URI.create(path + query)).build()
    }
}
