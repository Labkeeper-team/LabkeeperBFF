package io.labkeeper.web.frontend.internal.controller;

import io.labkeeper.businesslogic.operation.api.service.ProjectService;
import io.labkeeper.web.frontend.FrontendProperties;
import org.apache.logging.log4j.util.Strings;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

@RestController
public class PagesController {
    static final String HOME_PAGE = "/";
    static final String ABOUT_PAGE = "/about";
    static final String PROJECT_PAGE = "/project/{id}";
    static final String PROJECTS_PAGE = "/projects";
    static final String DEFAULT_PROJECT_PAGE = "/project/default";
    static final String OAUTH_CODE_PAGE = "/oauth2/code";
    static final String QR_PAGE = "/qr/{version}";

    static final String SENTRY_DSN_PARAM = "IO_LABKEEPER_FRONTEND_SENTRY_DSN";
    static final String YANDEX_METRIKA_PARAM = "IO_LABKEEPER_FRONTEND_YANDEX_METRIKA_KEY";
    static final String VERSION_PARAM = "IO_LABKEEPER_FRONTEND_API_VERSION";
    static final String YANDEX_CAPTCHA_SITE_KEY = "IO_LABKEEPER_FRONTEND_YANDEX_CAPTCHA_SITE_KEY";

    static final String HTML_TITLE_TO_REPLACE = "Labkeeper — online pdf editor with a built-in scientific calculator";
    static final String HTML_DESCRIPTION_TO_REPLACE = "Compose documents, formulas, and plots, convert to pdf. Editor and calculations in one place";

    final FrontendProperties frontendProperties;
    final Mono<String> reactIndexHtmlMono;
    final Mono<String> landingPageHtmlMono;
    final Mono<String> aboutUsPageHtmlMono;
    final int majorVersion;
    final String host;
    final ProjectService projectService;

    public PagesController(
            FrontendProperties frontendProperties,
            @Value("${io.labkeeper.version.major}") int majorVersion,
            @Value("${io.labkeeper.host}") String host,
            ProjectService projectService
    ) {
        this.host = host;
        this.majorVersion = majorVersion;
        this.frontendProperties = frontendProperties;
        this.reactIndexHtmlMono = getClasspathResourcesAsMono("editor/index.html")
                .map(str -> str
                        .replace(SENTRY_DSN_PARAM, frontendProperties.getSentryDsn())
                        .replace(YANDEX_METRIKA_PARAM, frontendProperties.getYandexMetrikaKey())
                        .replace(YANDEX_CAPTCHA_SITE_KEY, frontendProperties.getYandexCaptchaSiteKey()))
                .cache();
        this.projectService = projectService;
        this.landingPageHtmlMono = getClasspathResourcesAsMono("pages/index.html")
                .map(this::replaceLandingPageParams)
                .cache();
        this.aboutUsPageHtmlMono = getClasspathResourcesAsMono("pages/about.html")
                .map(this::replaceLandingPageParams)
                .cache();
    }

    @GetMapping({PROJECTS_PAGE, DEFAULT_PROJECT_PAGE, OAUTH_CODE_PAGE})
    public Mono<String> spa() {
        return reactIndexHtmlMono;
    }

    @GetMapping(QR_PAGE)
    public Mono<ResponseEntity<?>> qr() {
        return Mono.just(ResponseEntity.status(HttpStatus.FOUND) // 302
                .location(URI.create(host))
                .build());
    }

    @GetMapping(PROJECT_PAGE)
    public Mono<String> projectPageWithCustomDescription(
            @PathVariable("id") String id
    ) {
        return reactIndexHtmlMono
                .flatMap(content -> Mono.defer(() -> {
                    var uuid = UUID.fromString(id);
                    return projectService
                            .getProjectUnauthorized(uuid)
                            .map(project -> content
                                    .replace(HTML_TITLE_TO_REPLACE, project.title())
                                    .replace(HTML_DESCRIPTION_TO_REPLACE, Strings.EMPTY)
                            );
                        })
                        .onErrorReturn(content)
                );
    }

    @GetMapping({ABOUT_PAGE})
    public Mono<String> landingPage() {
        return aboutUsPageHtmlMono;
    }

    @GetMapping({HOME_PAGE})
    public Mono<String> aboutPage() {
        return landingPageHtmlMono;
    }

    private Mono<String> getClasspathResourcesAsMono(String res) {
        return Mono
                .just(new ClassPathResource(res))
                .handle((f, sink) -> {
                    try {
                        sink.next(f.getContentAsString(StandardCharsets.UTF_8));
                    } catch (IOException e) {
                        sink.error(new IllegalStateException(e));
                    }
                });
    }

    private String replaceLandingPageParams(String html) {
        return html
                .replace(VERSION_PARAM, "v" + majorVersion)
                .replace(YANDEX_METRIKA_PARAM, frontendProperties.getYandexMetrikaKey());
    }
}
