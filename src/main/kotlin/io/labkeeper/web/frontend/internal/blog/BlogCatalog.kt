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

    fun find(slug: String) = articles.firstOrNull { it.slug == slug }
}

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
    private val categories = yaml["categories"] as List<String>
    val categoryAttr = categories.joinToString(" ")
    val categoryNames = categories.map { CATEGORY_NAMES[it] ?: it }
    val tags = categories.zip(categoryNames, ::ArticleTag)
    private val dateModified = yaml["dateModified"] as String
    val dateDisplay: String = LocalDate.parse(dateModified).format(DATE_FORMAT)
    val editorExampleUrl = exampleUrl(yaml["editorExample"] as Map<String, String>)
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

private fun exampleUrl(example: Map<String, String>): String {
    val open = if ("latex" in example || "compute" in example) "latex" else "markdown"
    val params = listOf("open" to open) + EXAMPLE_TYPES.mapNotNull { type -> example[type]?.let { type to it } }
    return params.joinToString("&", prefix = "/project/default?") { (name, value) ->
        "$name=${URLEncoder.encode(value, Charsets.UTF_8)}"
    }
}
