package io.labkeeper.web.frontend.internal.controller

import io.labkeeper.web.frontend.FrontendProperties
import io.labkeeper.web.frontend.spi.ProjectSource
import org.springframework.core.io.ClassPathResource
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.util.HtmlUtils

@RestController
class EditorPagesController(
    private val properties: FrontendProperties,
    private val projectSource: ProjectSource?,
) {
    private val indexHtml by lazy {
        ClassPathResource("editor/index.html").getContentAsString(Charsets.UTF_8)
            .replace("IO_LABKEEPER_FRONTEND_SENTRY_DSN", properties.sentryDsn)
            .replace("IO_LABKEEPER_FRONTEND_YANDEX_METRIKA_KEY", properties.yandexMetrikaKey)
            .replace("IO_LABKEEPER_FRONTEND_YANDEX_CAPTCHA_SITE_KEY", properties.yandexCaptchaSiteKey)
            .replace("IO_LABKEEPER_FRONTEND_OPENPANEL_CLIENT_ID", properties.openpanelClientId)
            .replace("IO_LABKEEPER_FRONTEND_OPENPANEL_API_URL", properties.openpanelApiUrl)
    }

    @GetMapping(
        "/projects",
        "/projects/new",
        "/project/default",
        "/project/default/agent",
        "/oauth2/code",
        "/tokens",
        "/pay"
    )
    fun spa() = page(indexHtml)

    /** Link previews of a shared project show its title instead of the generic one. */
    @GetMapping("/project/{id}", "/project/{id}/agent")
    suspend fun project(@PathVariable id: String): ResponseEntity<String> {
        val title = runCatching { projectSource?.title(id) }.getOrNull() ?: return page(indexHtml)
        return page(
            indexHtml
                .replace(properties.defaultTitle, HtmlUtils.htmlEscape(title, Charsets.UTF_8.name()))
                .replace(properties.defaultDescription, ""),
        )
    }

    /**
     * Editor pages are HTML, not text/plain, and stay out of search results. The header works where the
     * `<meta name="robots">` of index.html is not read, and it covers user projects, which are not site content.
     */
    private fun page(html: String) = ResponseEntity.ok()
        .contentType(MediaType(MediaType.TEXT_HTML, Charsets.UTF_8))
        .header(ROBOTS_HEADER, NOINDEX)
        .body(html)

    private companion object {
        const val ROBOTS_HEADER = "X-Robots-Tag"
        const val NOINDEX = "noindex, follow"
    }
}
