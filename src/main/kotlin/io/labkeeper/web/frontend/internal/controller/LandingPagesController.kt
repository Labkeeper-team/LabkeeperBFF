package io.labkeeper.web.frontend.internal.controller

import io.labkeeper.web.frontend.FrontendProperties
import io.labkeeper.web.frontend.internal.blog.BlogCatalog
import io.labkeeper.web.frontend.internal.template.TemplateCatalog
import io.labkeeper.web.frontend.internal.wiki.WikiPages
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
    private val templateCatalog: TemplateCatalog,
    private val wikiPages: WikiPages,
    private val properties: FrontendProperties,
    @Value($$"${io.labkeeper.host}") private val host: String,
    @Value($$"${io.labkeeper.version.major}") private val versionMajor: String,
) {
    @ModelAttribute
    fun commonAttributes(model: Model) {
        model.addAttribute("apiVersion", "v$versionMajor")
        model.addAttribute("yandexMetrikaKey", properties.yandexMetrikaKey.ifBlank { "undefined" })
        model.addAttribute("openpanelClientId", properties.openpanelClientId)
        model.addAttribute("openpanelApiUrl", properties.openpanelApiUrl)
        model.addAttribute("publicUrl", host)
        model.addAttribute("contactEmail", properties.contactEmail)
        model.addAttribute("contactPhone", properties.contactPhone)
        model.addAttribute("articles", blogCatalog.articles)
        model.addAttribute("blogSliderJson", blogCatalog.sliderJson)
        model.addAttribute("templates", templateCatalog.templates)
        model.addAttribute(
            "viewerAuthenticated",
            ReactiveSecurityContextHolder.getContext()
                .map { TRUST_RESOLVER.isAuthenticated(it.authentication) }
                .defaultIfEmpty(false),
        )
    }

    @GetMapping("/")
    fun index(model: Model): String {
        model.addAttribute("previewProjects", properties.previewProjects)
        model.addAttribute("homeArticles", blogCatalog.articles.take(HOME_CATALOG_LIMIT))
        model.addAttribute("homeTemplates", templateCatalog.templates.take(HOME_CATALOG_LIMIT))
        return "index"
    }

    @GetMapping("/{page:about|blog|templates|privacy|soglas|sogl_yam|sogl_adv|sogl_ds|oferta}")
    fun page(@PathVariable page: String) = page

    @GetMapping("/blog/{slug}")
    fun article(@PathVariable slug: String, model: Model): String {
        val page = blogCatalog.find(slug) ?: throw ResponseStatusException(HttpStatus.NOT_FOUND)
        model.addAttribute("article", page)
        model.addAttribute("relatedLinks", blogCatalog.relatedTo(page))
        return "article"
    }

    @GetMapping("/templates/{slug}")
    fun template(@PathVariable slug: String, model: Model): String {
        val page = templateCatalog.find(slug) ?: throw ResponseStatusException(HttpStatus.NOT_FOUND)
        model.addAttribute("article", page)
        model.addAttribute("relatedLinks", templateCatalog.relatedTo(page))
        model.addAttribute("blogSliderJson", templateCatalog.sliderJson)
        return "article"
    }

    @GetMapping("/sitemap.xml", produces = [MediaType.APPLICATION_XML_VALUE])
    fun sitemap(model: Model): String {
        model.addAttribute("blogLastModified", blogCatalog.lastModified)
        model.addAttribute("templatesLastModified", templateCatalog.lastModified)
        model.addAttribute(
            "homeLastModified",
            listOfNotNull(blogCatalog.lastModified, templateCatalog.lastModified).maxOrNull(),
        )
        model.addAttribute("wikiPages", wikiPages.paths)
        return "sitemap"
    }

    private companion object {
        const val HOME_CATALOG_LIMIT = 100
        val TRUST_RESOLVER = AuthenticationTrustResolverImpl()
    }
}
