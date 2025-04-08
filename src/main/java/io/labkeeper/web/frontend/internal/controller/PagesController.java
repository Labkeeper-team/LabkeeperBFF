package io.labkeeper.web.frontend.internal.controller;

import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import static io.labkeeper.web.common.api.EndpointsList.DEFAULT_PROJECT_PAGE;
import static io.labkeeper.web.common.api.EndpointsList.HOME_PAGE;
import static io.labkeeper.web.common.api.EndpointsList.PROJECTS_PAGE;
import static io.labkeeper.web.common.api.EndpointsList.PROJECT_PAGE;

@RestController
public class PagesController {
    final Mono<Resource> indexHtmlMono = Mono.just(new ClassPathResource("frontend/index.html"));

    @GetMapping({HOME_PAGE, PROJECT_PAGE, PROJECTS_PAGE, DEFAULT_PROJECT_PAGE})
    public Mono<Resource> spa() {
        return indexHtmlMono;
    }
}
