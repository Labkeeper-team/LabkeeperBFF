package io.labkeeper.web.frontend.internal.controller

import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController
import reactor.core.publisher.Mono
import java.net.URI

@RestController
class FrontendRedirectController(
    @Value($$"${io.labkeeper.host:http://localhost:8080}") private val host: String,
) {
    @GetMapping("/qr/{version}")
    fun qr(): Mono<ResponseEntity<Void>> = Mono.just(
        ResponseEntity
            .status(HttpStatus.FOUND)
            .location(URI.create(host))
            .build(),
    )

    @GetMapping("/wiki")
    fun wiki(): Mono<ResponseEntity<Void>> = Mono.just(
        ResponseEntity
            .status(HttpStatus.FOUND)
            .location(URI.create("/wiki/"))
            .build(),
    )
}
