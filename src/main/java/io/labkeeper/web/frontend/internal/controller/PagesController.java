package io.labkeeper.web.frontend.internal.controller;

import io.labkeeper.web.frontend.FrontendProperties;
import org.springframework.core.io.ClassPathResource;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

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
    static final String YANDEX_CAPTCHA_SITE_KEY = "IO_LABKEEPER_FRONTEND_YANDEX_CAPTCHA_SITE_KEY";

    final FrontendProperties frontendProperties;
    final Mono<String> reactIndexHtmlMono;
    final Mono<String> landingPageHtmlMono;
    final Mono<String> aboutUsPageHtmlMono;

    public PagesController(FrontendProperties frontendProperties) {
        this.frontendProperties = frontendProperties;
        this.reactIndexHtmlMono = getClasspathResourcesAsMono("react/index.html")
                .map(str -> str
                        .replace(SENTRY_DSN_PARAM, frontendProperties.getSentryDsn())
                        .replace(YANDEX_METRIKA_PARAM, frontendProperties.getYandexMetrikaKey())
                        .replace(YANDEX_CAPTCHA_SITE_KEY, frontendProperties.getYandexCaptchaSiteKey()))
                .cache();
        this.landingPageHtmlMono = getClasspathResourcesAsMono("static/index.html")
                .map(this::replaceLandingPageParams)
                .cache();
        this.aboutUsPageHtmlMono = getClasspathResourcesAsMono("static/about.html")
                .map(this::replaceLandingPageParams)
                .cache();
    }

    @GetMapping({PROJECT_PAGE, PROJECTS_PAGE, DEFAULT_PROJECT_PAGE, OAUTH_CODE_PAGE, QR_PAGE})
    public Mono<String> spa() {
        return reactIndexHtmlMono;
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
                .<String>handle((f, sink) -> {
                    try {
                        sink.next(f.getContentAsString(StandardCharsets.UTF_8));
                    } catch (IOException e) {
                        sink.error(new IllegalStateException(e));
                    }
                });
    }

    private String replaceLandingPageParams(String html) {
        return html.replace(YANDEX_METRIKA_PARAM, frontendProperties.getYandexMetrikaKey());
    }
}
