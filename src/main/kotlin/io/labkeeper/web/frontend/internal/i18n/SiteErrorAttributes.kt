package io.labkeeper.web.frontend.internal.i18n

import org.springframework.boot.web.error.ErrorAttributeOptions
import org.springframework.boot.web.reactive.error.DefaultErrorAttributes
import org.springframework.http.InvalidMediaTypeException
import org.springframework.http.MediaType
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.server.ServerRequest

/**
 * The error pages (templates/error) are rendered by Spring Boot, not by LandingPagesController, so they would miss the
 * language and the texts the landing pages get. Only HTML requests get them: a JSON error stays small.
 */
@Component
class SiteErrorAttributes(private val siteTexts: SiteTexts) : DefaultErrorAttributes() {
    override fun getErrorAttributes(request: ServerRequest, options: ErrorAttributeOptions): MutableMap<String, Any?> {
        val attributes = super.getErrorAttributes(request, options)
        if (acceptsHtml(request)) {
            val language = SiteLanguage.of(request.cookies().getFirst(SiteLanguage.COOKIE)?.value)
            attributes["lang"] = language.code
            attributes["i18n"] = siteTexts[language]
        }
        return attributes
    }

    /**
     * The same test Spring Boot uses to pick the HTML error page: a client that accepts anything (axios, curl) gets
     * JSON, and a malformed Accept header is not an error of its own
     */
    private fun acceptsHtml(request: ServerRequest): Boolean = try {
        request.headers().accept()
            .filterNot { MediaType.ALL.equalsTypeAndSubtype(it) }
            .any { MediaType.TEXT_HTML.isCompatibleWith(it) }
    } catch (ignored: InvalidMediaTypeException) {
        false
    }
}
