package io.labkeeper.web.frontend.internal.controller

import io.labkeeper.web.frontend.FrontendProperties
import io.labkeeper.web.frontend.internal.blog.BlogCatalog
import io.labkeeper.web.frontend.internal.project.LandingPreviewProject
import io.labkeeper.web.frontend.internal.project.ProjectSource
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.security.authentication.AuthenticationTrustResolverImpl
import org.springframework.security.core.context.ReactiveSecurityContextHolder
import org.springframework.stereotype.Controller
import org.springframework.ui.Model
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.ModelAttribute
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.server.ResponseStatusException

@Controller
class LandingPagesController(
    private val blogCatalog: BlogCatalog,
    private val projectSource: ProjectSource?,
    private val properties: FrontendProperties,
    @Value($$"${io.labkeeper.version.major}") private val versionMajor: String,
) {
    @ModelAttribute
    fun commonAttributes(model: Model) {
        model.addAttribute("apiVersion", "v$versionMajor")
        model.addAttribute("yandexMetrikaKey", properties.yandexMetrikaKey.ifBlank { "undefined" })
        model.addAttribute("openpanelClientId", properties.openpanelClientId)
        model.addAttribute("openpanelApiUrl", properties.openpanelApiUrl)
        model.addAttribute("publicUrl", properties.siteUrl)
        model.addAttribute("contactEmail", properties.contactEmail)
        model.addAttribute("contactPhone", properties.contactPhone)
        model.addAttribute("articles", blogCatalog.articles)
        model.addAttribute("blogSliderJson", blogCatalog.sliderJson)
        model.addAttribute(
            "viewerAuthenticated",
            ReactiveSecurityContextHolder.getContext()
                .map { TRUST_RESOLVER.isAuthenticated(it.authentication) }
                .defaultIfEmpty(false),
        )
    }

    @GetMapping("/")
    suspend fun index(model: Model): String {
        val projects = projectSource?.let { runCatching { it.previewProjects() }.getOrNull() }
        model.addAttribute("previewProjects", projects ?: LandingPreviewProject.FALLBACK)
        return "index"
    }

    @GetMapping("/{page:about|blog|privacy|soglas|sogl_yam|sogl_adv|sogl_ds|oferta}")
    fun page(@PathVariable page: String) = page

    @GetMapping("/blog/{slug}")
    fun article(@PathVariable slug: String, model: Model): String {
        model.addAttribute("article", blogCatalog.find(slug) ?: throw ResponseStatusException(HttpStatus.NOT_FOUND))
        return "article"
    }

    @GetMapping("/sitemap.xml", produces = [MediaType.APPLICATION_XML_VALUE])
    fun sitemap() = "sitemap"

    private companion object {
        val TRUST_RESOLVER = AuthenticationTrustResolverImpl()
    }
}
