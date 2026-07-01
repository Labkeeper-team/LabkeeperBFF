package io.labkeeper.web.frontend.internal.controller

import io.labkeeper.businesslogic.operation.api.service.ProjectService
import kotlinx.coroutines.reactor.mono
import org.apache.logging.log4j.util.Strings
import org.springframework.beans.factory.annotation.Value
import org.springframework.core.io.ClassPathResource
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RestController
import reactor.core.publisher.Mono
import java.io.IOException
import java.net.URI
import java.nio.charset.StandardCharsets
import java.util.UUID

@RestController
class PagesController(
    @Value($$"${io.labkeeper.version.major}") private val majorVersion: Int,
    @Value($$"${io.labkeeper.host}") private val host: String,
    @Value($$"${io.labkeeper.frontend.sentry-dsn}") private val sentryDsn: String,
    @Value($$"${io.labkeeper.frontend.yandex-metrika-key}") private val yandexMetrikaKey: String,
    @Value($$"${io.labkeeper.frontend.yandex-captcha-site-key}") private val yandexCaptchaSiteKey: String,
    private val projectService: ProjectService,
) {
    private val reactIndexHtmlMono: Mono<String> = getClasspathResourcesAsMono("editor/index.html")
        .map { str ->
            str
                .replace(SENTRY_DSN_PARAM, sentryDsn)
                .replace(YANDEX_METRIKA_PARAM, yandexMetrikaKey)
                .replace(YANDEX_CAPTCHA_SITE_KEY, yandexCaptchaSiteKey)
        }
        .cache()

    private val landingPageHtmlMono: Mono<String> = getClasspathResourcesAsMono("pages/index.html")
        .map(::replaceLandingPageParams)
        .cache()

    private val aboutUsPageHtmlMono: Mono<String> = getClasspathResourcesAsMono("pages/about.html")
        .map(::replaceLandingPageParams)
        .cache()

    @GetMapping(PROJECTS_PAGE, DEFAULT_PROJECT_PAGE, OAUTH_CODE_PAGE)
    fun spa(): Mono<String> = reactIndexHtmlMono

    @GetMapping(QR_PAGE)
    fun qr(): Mono<ResponseEntity<Void>> = Mono.just(
        ResponseEntity
            .status(HttpStatus.FOUND)
            .location(URI.create(host))
            .build(),
    )

    @GetMapping(PROJECT_PAGE)
    fun projectPageWithCustomDescription(
        @PathVariable("id") id: String,
    ): Mono<String> {
        return reactIndexHtmlMono.flatMap { content ->
            mono {
                val uuid = UUID.fromString(id)
                val project = projectService.getProjectUnauthorizedWithProgramResult(uuid)
                return@mono content
                    .replace(HTML_TITLE_TO_REPLACE, project.title())
                    .replace(HTML_DESCRIPTION_TO_REPLACE, Strings.EMPTY)
            }.onErrorReturn(content)
        }
    }

    @GetMapping(ABOUT_PAGE)
    fun landingPage(): Mono<String> = aboutUsPageHtmlMono

    @GetMapping(HOME_PAGE)
    fun aboutPage(): Mono<String> = landingPageHtmlMono

    private fun getClasspathResourcesAsMono(res: String): Mono<String> {
        return Mono
            .just(ClassPathResource(res))
            .handle { resource, sink ->
                try {
                    sink.next(resource.getContentAsString(StandardCharsets.UTF_8))
                } catch (e: IOException) {
                    sink.error(IllegalStateException(e))
                }
            }
    }

    private fun replaceLandingPageParams(html: String): String {
        return html
            .replace(VERSION_PARAM, "v$majorVersion")
            .replace(YANDEX_METRIKA_PARAM, yandexMetrikaKey)
    }

    companion object {
        private const val HOME_PAGE = "/"
        private const val ABOUT_PAGE = "/about"
        private const val PROJECT_PAGE = "/project/{id}"
        private const val PROJECTS_PAGE = "/projects"
        private const val DEFAULT_PROJECT_PAGE = "/project/default"
        private const val OAUTH_CODE_PAGE = "/oauth2/code"
        private const val QR_PAGE = "/qr/{version}"

        private const val SENTRY_DSN_PARAM = "IO_LABKEEPER_FRONTEND_SENTRY_DSN"
        private const val YANDEX_METRIKA_PARAM = "IO_LABKEEPER_FRONTEND_YANDEX_METRIKA_KEY"
        private const val VERSION_PARAM = "IO_LABKEEPER_FRONTEND_API_VERSION"
        private const val YANDEX_CAPTCHA_SITE_KEY = "IO_LABKEEPER_FRONTEND_YANDEX_CAPTCHA_SITE_KEY"

        private const val HTML_TITLE_TO_REPLACE = "Labkeeper — online pdf editor with a built-in scientific calculator"
        private const val HTML_DESCRIPTION_TO_REPLACE = "Compose documents, formulas, and plots, convert to pdf. Editor and calculations in one place"
    }
}
