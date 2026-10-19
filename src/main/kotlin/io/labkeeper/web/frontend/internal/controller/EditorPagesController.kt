package io.labkeeper.web.frontend.internal.controller

import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController
import reactor.core.publisher.Mono

@RestController
class EditorPagesController(
    private val editorIndexPage: EditorIndexPage,
) {
    @GetMapping(
        "/projects",
        "/project/default",
        "/project/default/agent",
        "/oauth2/code",
        "/tokens",
        "/pay",
    )
    fun spa(): Mono<String> = editorIndexPage.content()
}
