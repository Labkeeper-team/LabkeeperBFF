package io.labkeeper.web.frontend.internal.blog

import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.beans.factory.annotation.Value
import org.springframework.core.io.support.PathMatchingResourcePatternResolver
import org.springframework.stereotype.Component
import org.yaml.snakeyaml.Yaml
import java.net.URLEncoder
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

@Component
class BlogCatalog(@Value($$"${io.labkeeper.host}") host: String) {
    val articles = PathMatchingResourcePatternResolver().getResources("classpath:blog/articles/**/*.yaml")
        .map { resource ->
            val text = resource.inputStream.bufferedReader().use { it.readText() }
                .replace(PUBLIC_URL_TOKEN, host)
                .replace(PUBLIC_HOST_TOKEN, host)
            BlogArticle(Yaml().load(text), host)
        }
        .sortedBy { it.catalogIndex }

    val sliderJson = scriptJson(
        articles.map { mapOf("url" to "/blog/${it.slug}", "title" to it.sliderTitle, "text" to it.sliderText) },
    )

    private val categorySize = articles.flatMap { it.categoryIds }.groupingBy { it }.eachCount()

    fun find(slug: String) = articles.firstOrNull { it.slug == slug }

    /**
     * Neighbors in the smallest shared category, in catalog order.
     * A short window keeps a large category one connected chain instead of a random mix.
     */
    fun relatedTo(page: BlogArticle): List<ArticleLink> {
        val primary = page.categoryIds.minByOrNull { categorySize[it] ?: Int.MAX_VALUE } ?: return emptyList()
        val group = articles.filter { primary in it.categoryIds }.sortedBy { it.catalogIndex }
        val self = group.indexOfFirst { it.slug == page.slug }
        if (self < 0 || group.size <= 1) return emptyList()
        val limit = minOf(RELATED_LIMIT, group.size - 1)
        val before = limit / 2
        val after = limit - before
        return buildList {
            for (step in 1..before) add(group[(self - step + group.size) % group.size])
            for (step in 1..after) add(group[(self + step) % group.size])
        }.sortedBy { it.catalogIndex }
            .map { ArticleLink("/blog/${it.slug}", plain(it.sliderTitle), plain(it.sliderText)) }
    }
}

data class ArticleLink(val href: String, val title: String, val text: String)

@Suppress("UNCHECKED_CAST")
class BlogArticle(yaml: Map<String, Any?>, siteUrl: String) {
    val slug: String by yaml
    val catalogIndex: Int by yaml
    val pageTitle: String by yaml
    val metaDescription: String by yaml
    val h1: String by yaml
    val breadcrumbTitle: String by yaml
    val readingTime: String by yaml
    val sliderTitle: String by yaml
    val sliderText: String by yaml
    val sidebarText: String by yaml
    val ctaTitle: String by yaml
    val ctaText: String by yaml
    val toc: List<Map<String, String>> by yaml
    val sections: List<Map<String, String>> by yaml
    val tips = (yaml["tips"] as List<Map<String, String>>?).orEmpty()
    val sidebarImageAlt = yaml["sidebarImageAlt"] as String? ?: "Интерфейс онлайн-редактора LaTeX Labkeeper"
    val ogTitle = yaml["ogTitle"] as String? ?: pageTitle
    val ogDescription = yaml["ogDescription"] as String? ?: metaDescription
    val twitterTitle = yaml["twitterTitle"] as String? ?: pageTitle
    val twitterDescription = yaml["twitterDescription"] as String? ?: metaDescription
    val cardTitle = collapseWhitespace(yaml["cardTitle"] as String)
    val cardDescription = collapseWhitespace(yaml["cardDescription"] as String)
    val keywordsText = (yaml["keywords"] as List<String>).joinToString(", ")
    val categoryIds = yaml["categories"] as List<String>
    val categoryAttr = categoryIds.joinToString(" ")
    val categoryNames = categoryIds.map { CATEGORY_NAMES[it] ?: it }
    val tags = categoryIds.zip(categoryNames, ::ArticleTag)
    private val dateModified = yaml["dateModified"] as String
    val dateDisplay: String = LocalDate.parse(dateModified).format(DATE_FORMAT)
    val editorExampleUrl = exampleUrl(yaml["editorExample"] as Map<String, String>)
    val openUrl = editorExampleUrl
    val opensProject = false
    val canonicalPath = "/blog/$slug"
    val catalogHref = "/blog"
    val catalogLabel = "Блог"
    val relatedTitle = "Другие статьи"
    val relatedHref = "/blog"
    val relatedLabel = "Все статьи"
    private val url = "$siteUrl/blog/$slug"

    val articleJsonLd = scriptJson(
        mapOf(
            "@context" to "https://schema.org",
            "@type" to "Article",
            "headline" to yaml["title"],
            "description" to (yaml["schemaDescription"] ?: metaDescription),
            "author" to mapOf("@type" to "Organization", "name" to "Labkeeper", "url" to siteUrl),
            "publisher" to mapOf(
                "@type" to "Organization",
                "name" to "Labkeeper",
                "logo" to mapOf("@type" to "ImageObject", "url" to "$siteUrl/assets/img/logo.svg"),
            ),
            "mainEntityOfPage" to mapOf("@type" to "WebPage", "@id" to url),
            "datePublished" to yaml["datePublished"],
            "dateModified" to dateModified,
            "inLanguage" to "ru",
            "articleSection" to categoryNames,
        ),
    )

    val breadcrumbJsonLd = scriptJson(
        mapOf(
            "@context" to "https://schema.org",
            "@type" to "BreadcrumbList",
            "itemListElement" to listOf(
                "Главная" to "$siteUrl/",
                "Блог" to "$siteUrl/blog",
                (yaml["schemaBreadcrumbTitle"] ?: breadcrumbTitle) to url,
            ).mapIndexed { index, (name, item) ->
                mapOf("@type" to "ListItem", "position" to index + 1, "name" to name, "item" to item)
            },
        ),
    )
}

data class ArticleTag(val id: String, val name: String)

private const val RELATED_LIMIT = 12

private const val PUBLIC_URL_TOKEN = "{{publicUrl}}"
private const val PUBLIC_HOST_TOKEN = "{{publicHost}}"
private val JSON = ObjectMapper()
private val WHITESPACE = Regex("\\s+")
private val DATE_FORMAT = DateTimeFormatter.ofPattern("d MMMM yyyy", Locale.forLanguageTag("ru"))
private val EXAMPLE_TYPES = listOf("compute", "latex", "markdown")
private val CATEGORY_NAMES = mapOf(
    "latex" to "Работа с LaTeX",
    "markdown" to "Работа с Markdown",
    "labs" to "Лабораторные работы",
    "diploma" to "Дипломные работы",
    "cv" to "Оформление резюме / CV",
    "articles" to "Оформление научных статей",
)

/** The JSON goes inside `<script>`, so `<` is escaped to keep `</script>` in a string from closing the tag. */
private fun scriptJson(value: Any) = JSON.writeValueAsString(value).replace("<", "\\u003c")

private fun collapseWhitespace(text: String) = text.replace(WHITESPACE, " ").trim()

private fun plain(text: String) = collapseWhitespace(text.replace("&nbsp;", " ").replace("\u00a0", " "))

private fun exampleUrl(example: Map<String, String>): String {
    val open = if ("latex" in example || "compute" in example) "latex" else "markdown"
    val params = listOf("open" to open) + EXAMPLE_TYPES.mapNotNull { type -> example[type]?.let { type to it } }
    return params.joinToString("&", prefix = "/project/default?") { (name, value) ->
        "$name=${URLEncoder.encode(value, Charsets.UTF_8)}"
    }
}
