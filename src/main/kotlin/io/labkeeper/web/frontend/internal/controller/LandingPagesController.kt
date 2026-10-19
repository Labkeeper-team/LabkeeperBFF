package io.labkeeper.web.frontend.internal.controller

import io.labkeeper.web.frontend.internal.blog.BlogCatalog
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.stereotype.Controller
import org.springframework.ui.Model
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.ResponseBody
import org.springframework.web.server.ResponseStatusException
import org.springframework.web.server.ServerWebExchange

@Controller
class LandingPagesController(
    private val blogCatalog: BlogCatalog,
) {
    @GetMapping("/", "/about", "/soglas", "/privacy", "/sogl_yam", "/sogl_adv", "/sogl_ds", "/oferta", "/blog")
    fun page(exchange: ServerWebExchange): String =
        PAGES[exchange.request.path.value()] ?: throw ResponseStatusException(HttpStatus.NOT_FOUND)

    @GetMapping("/blog/{slug}")
    fun article(@PathVariable("slug") slug: String, model: Model): String {
        if (!BLOG_SLUG.matches(slug)) {
            throw ResponseStatusException(HttpStatus.NOT_FOUND)
        }
        val article = blogCatalog.find(slug) ?: throw ResponseStatusException(HttpStatus.NOT_FOUND)
        model.addAttribute("article", article)
        return "article"
    }

    @GetMapping("/sitemap.xml", produces = [MediaType.APPLICATION_XML_VALUE])
    @ResponseBody
    fun sitemap(): String {
        val urls = buildList {
            add("https://labkeeper.io/")
            add("https://labkeeper.io/about")
            add("https://labkeeper.io/blog")
            add("https://labkeeper.io/privacy")
            add("https://labkeeper.io/soglas")
            add("https://labkeeper.io/oferta")
            blogCatalog.articles.forEach { add("https://labkeeper.io/blog/${it.slug}") }
        }
        val body = urls.joinToString("\n") { url ->
            "  <url><loc>${url.xmlEscape()}</loc></url>"
        }
        return """
            <?xml version="1.0" encoding="UTF-8"?>
            <urlset xmlns="http://www.sitemaps.org/schemas/sitemap/0.9">
            $body
            </urlset>
        """.trimIndent()
    }

    private fun String.xmlEscape(): String = this
        .replace("&", "&amp;")
        .replace("<", "&lt;")
        .replace(">", "&gt;")

    private companion object {
        private val BLOG_SLUG = Regex("[a-z0-9-]+")
        private val PAGES = mapOf(
            "/" to "index",
            "/about" to "about",
            "/soglas" to "soglas",
            "/privacy" to "privacy",
            "/sogl_yam" to "sogl_yam",
            "/sogl_adv" to "sogl_adv",
            "/sogl_ds" to "sogl_ds",
            "/oferta" to "oferta",
            "/blog" to "blog",
        )
    }
}
