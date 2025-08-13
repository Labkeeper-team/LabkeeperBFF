package io.labkeeper.web.frontend.internal.controller;

import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

@RestController
public class PagesController {
    static final String HOME_PAGE = "/";
    static final String PROJECT_PAGE = "/project/{id}";
    static final String PROJECTS_PAGE = "/projects";
    static final String DEFAULT_PROJECT_PAGE = "/project/default";
    static final String OAUTH_CODE_PAGE = "/oauth2/code";
    static final String QR_PAGE = "/qr/{version}";

    final Mono<Resource> indexHtmlMono = Mono.just(new ClassPathResource("frontend/index.html"));

    @GetMapping({HOME_PAGE, PROJECT_PAGE, PROJECTS_PAGE, DEFAULT_PROJECT_PAGE, OAUTH_CODE_PAGE, QR_PAGE})
    public Mono<Resource> spa() {
        return indexHtmlMono;
    }
}
