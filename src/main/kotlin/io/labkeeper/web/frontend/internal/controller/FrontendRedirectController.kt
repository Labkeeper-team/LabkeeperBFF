package io.labkeeper.web.frontend.internal.controller

import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController
import java.net.URI

@RestController
class FrontendRedirectController(
    @Value($$"${io.labkeeper.host}") private val host: String,
) {
    @GetMapping("/qr/{version}")
    fun qr() = found(host)

    @GetMapping("/wiki")
    fun wiki() = found("/wiki/")

    private fun found(location: String) =
        ResponseEntity.status(HttpStatus.FOUND).location(URI.create(location)).build<Void>()
}
