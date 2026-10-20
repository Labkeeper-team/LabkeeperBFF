package io.labkeeper.web.frontend.internal.controller

import io.labkeeper.web.frontend.FrontendProperties
import io.labkeeper.web.frontend.spi.ProjectSource
import org.springframework.core.io.ClassPathResource
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

    @GetMapping("/projects", "/project/default", "/project/default/agent", "/oauth2/code", "/tokens", "/pay")
    fun spa() = indexHtml

    /** Link previews of a shared project show its title instead of the generic one. */
    @GetMapping("/project/{id}", "/project/{id}/agent")
    suspend fun project(@PathVariable id: String): String {
        val title = runCatching { projectSource?.title(id) }.getOrNull() ?: return indexHtml
        return indexHtml
            .replace(properties.defaultTitle, HtmlUtils.htmlEscape(title, Charsets.UTF_8.name()))
            .replace(properties.defaultDescription, "")
    }
}
