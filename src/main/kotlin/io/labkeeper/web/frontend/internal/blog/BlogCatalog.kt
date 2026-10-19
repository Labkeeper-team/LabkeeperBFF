package io.labkeeper.web.frontend.internal.blog

import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.core.io.support.PathMatchingResourcePatternResolver
import org.springframework.stereotype.Component
import org.yaml.snakeyaml.Yaml
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

@Component
class BlogCatalog {
    val articles: List<BlogArticle> = loadArticles()
    val blogSliderJson: String = JSON.writeValueAsString(
        articles.map {
            mapOf(
                "url" to "/blog/${it.slug}",
                "title" to it.sliderTitle,
                "text" to it.sliderText,
            )
        },
    ).replace("<", "\\u003c")

    fun find(slug: String): BlogArticle? = articles.firstOrNull { it.slug == slug }

    private fun loadArticles(): List<BlogArticle> {
        val yaml = Yaml()
        val resources = PathMatchingResourcePatternResolver().getResources("classpath*:blog/articles/*.yaml")
        return resources.map { resource ->
            resource.inputStream.reader(StandardCharsets.UTF_8).use { reader ->
                @Suppress("UNCHECKED_CAST")
                val document = yaml.load<Map<String, Any?>>(reader)
                BlogArticle.from(document)
            }
        }.sortedBy { it.catalogIndex }
    }

    companion object {
        private val JSON = ObjectMapper()
    }
}

class BlogArticle(
    val slug: String,
    val catalogIndex: Int,
    val pageTitle: String,
    val metaDescription: String,
    val keywordsText: String,
    val h1: String,
    val breadcrumbTitle: String,
    val tags: List<ArticleTag>,
    val categoryNames: List<String>,
    val categoryAttr: String,
    val dateDisplay: String,
    val readingTime: String,
    val cardTitle: String,
    val cardDescription: String,
    val sliderTitle: String,
    val sliderText: String,
    val sidebarText: String,
    val sidebarImageAlt: String,
    val ctaTitle: String,
    val ctaText: String,
    val ogTitle: String,
    val ogDescription: String,
    val twitterTitle: String,
    val twitterDescription: String,
    val editorExampleUrl: String,
    val toc: List<ArticleHeading>,
    val sections: List<ArticleSection>,
    val tips: List<ArticleTip>,
    val articleJsonLd: String,
    val breadcrumbJsonLd: String,
) {
    companion object {
        private val months = listOf(
            "января", "февраля", "марта", "апреля", "мая", "июня",
            "июля", "августа", "сентября", "октября", "ноября", "декабря",
        )
        private val categoryNames = mapOf(
            "latex" to "Работа с LaTeX",
            "markdown" to "Работа с Markdown",
            "labs" to "Лабораторные работы",
            "diploma" to "Дипломные работы",
            "cv" to "Оформление резюме / CV",
            "articles" to "Оформление научных статей",
        )
        private val exampleTypes = listOf("compute", "latex", "markdown")
        private val json = ObjectMapper()

        fun from(document: Map<String, Any?>): BlogArticle {
            val slug = text(document, "slug")
            val title = text(document, "title")
            val pageTitle = text(document, "pageTitle").ifBlank { "$title — Labkeeper" }
            val metaDescription = text(document, "metaDescription").ifBlank { text(document, "description") }
            val categories = stringList(document["categories"]).ifEmpty { listOf("latex") }
            val names = categories.map { categoryNames[it] ?: it }
            val keywords = stringList(document["keywords"])
            val dateModified = text(document, "dateModified").ifBlank { text(document, "datePublished") }
            val sections = blocks(document["sections"])
            val tips = if (sections.any { it.id == "tips" }) emptyList() else tips(document["tips"])
            val breadcrumbTitle = text(document, "breadcrumbTitle").ifBlank { title }
            val cardTitle = collapse(text(document, "cardTitle").ifBlank { title })
            val cardDescription = collapse(text(document, "cardDescription").ifBlank { metaDescription })
            val sliderTitle = text(document, "sliderTitle").ifBlank { cardTitle }
            val sliderText = text(document, "sliderText").ifBlank { cardDescription }
            val schemaDescription = text(document, "schemaDescription").ifBlank { metaDescription }
            val schemaBreadcrumb = text(document, "schemaBreadcrumbTitle").ifBlank { breadcrumbTitle }
            return BlogArticle(
                slug = slug,
                catalogIndex = number(document["catalogIndex"]),
                pageTitle = pageTitle,
                metaDescription = metaDescription,
                keywordsText = keywords.joinToString(", "),
                h1 = text(document, "h1").ifBlank { title },
                breadcrumbTitle = breadcrumbTitle,
                tags = categories.zip(names).map { pair -> ArticleTag(pair.first, pair.second) },
                categoryNames = names,
                categoryAttr = categories.joinToString(" "),
                dateDisplay = displayDate(dateModified),
                readingTime = text(document, "readingTime").ifBlank { "7 мин" },
                cardTitle = cardTitle,
                cardDescription = cardDescription,
                sliderTitle = sliderTitle,
                sliderText = sliderText,
                sidebarText = text(document, "sidebarText").ifBlank {
                    "Онлайн-редактор LaTeX с компиляцией в PDF — сразу в браузере."
                },
                sidebarImageAlt = text(document, "sidebarImageAlt").ifBlank {
                    "Интерфейс онлайн-редактора LaTeX Labkeeper"
                },
                ctaTitle = text(document, "ctaTitle").ifBlank { "Набирайте сложные работы в Labkeeper" },
                ctaText = text(document, "ctaText").ifBlank {
                    "Онлайн-редактор LaTeX с компиляцией в PDF — сразу в браузере."
                },
                ogTitle = text(document, "ogTitle").ifBlank { pageTitle },
                ogDescription = text(document, "ogDescription").ifBlank { metaDescription },
                twitterTitle = text(document, "twitterTitle").ifBlank { pageTitle },
                twitterDescription = text(document, "twitterDescription").ifBlank { metaDescription },
                editorExampleUrl = exampleUrl(mapValue(document["editorExample"])),
                toc = headings(document["toc"], sections),
                sections = sections,
                tips = tips,
                articleJsonLd = jsonLd(
                    mapOf(
                        "@context" to "https://schema.org",
                        "@type" to "Article",
                        "headline" to title,
                        "description" to schemaDescription,
                        "author" to mapOf("@type" to "Organization", "name" to "Labkeeper", "url" to "https://labkeeper.io"),
                        "publisher" to mapOf(
                            "@type" to "Organization",
                            "name" to "Labkeeper",
                            "logo" to mapOf(
                                "@type" to "ImageObject",
                                "url" to "https://labkeeper.io/assets/img/logo.svg",
                            ),
                        ),
                        "mainEntityOfPage" to mapOf(
                            "@type" to "WebPage",
                            "@id" to "https://labkeeper.io/blog/$slug",
                        ),
                        "datePublished" to text(document, "datePublished"),
                        "dateModified" to dateModified,
                        "inLanguage" to "ru",
                        "articleSection" to names,
                    ),
                ),
                breadcrumbJsonLd = jsonLd(
                    mapOf(
                        "@context" to "https://schema.org",
                        "@type" to "BreadcrumbList",
                        "itemListElement" to listOf(
                            crumb(1, "Главная", "https://labkeeper.io/"),
                            crumb(2, "Блог", "https://labkeeper.io/blog"),
                            crumb(3, schemaBreadcrumb, "https://labkeeper.io/blog/$slug"),
                        ),
                    ),
                ),
            )
        }

        private fun crumb(position: Int, name: String, item: String): Map<String, Any> = mapOf(
            "@type" to "ListItem",
            "position" to position,
            "name" to name,
            "item" to item,
        )

        private fun jsonLd(value: Any): String = json.writeValueAsString(value).replace("<", "\\u003c")

        private fun headings(value: Any?, sections: List<ArticleSection>): List<ArticleHeading> {
            val items = maps(value)
            if (items.isEmpty()) {
                return sections.map { ArticleHeading(it.id, it.title) }
            }
            return items.map { ArticleHeading(text(it, "id"), text(it, "title")) }
        }

        private fun blocks(value: Any?): List<ArticleSection> =
            maps(value).map { ArticleSection(text(it, "id"), text(it, "title"), text(it, "html")) }

        private fun tips(value: Any?): List<ArticleTip> =
            maps(value).map { ArticleTip(text(it, "title"), text(it, "html")) }

        @Suppress("UNCHECKED_CAST")
        private fun maps(value: Any?): List<Map<String, Any?>> =
            (value as? List<*>)?.mapNotNull { it as? Map<String, Any?> }.orEmpty()

        @Suppress("UNCHECKED_CAST")
        private fun mapValue(value: Any?): Map<String, Any?> = value as? Map<String, Any?> ?: emptyMap()

        private fun stringList(value: Any?): List<String> = when (value) {
            is List<*> -> value.mapNotNull { it?.toString() }.filter { it.isNotBlank() }
            is String -> listOf(value)
            else -> emptyList()
        }

        private fun text(map: Map<String, Any?>, key: String): String = map[key]?.toString().orEmpty()

        private fun number(value: Any?): Int = when (value) {
            is Number -> value.toInt()
            is String -> value.toIntOrNull() ?: Int.MAX_VALUE
            else -> Int.MAX_VALUE
        }

        private fun collapse(value: String): String = value.replace(Regex("\\s+"), " ").trim()

        private fun displayDate(iso: String): String {
            val parts = iso.split("-")
            if (parts.size != 3) {
                return iso
            }
            val month = parts[1].toIntOrNull()?.minus(1) ?: return iso
            val day = parts[2].toIntOrNull() ?: return iso
            if (month !in months.indices) {
                return iso
            }
            return "$day ${months[month]} ${parts[0]}"
        }

        private fun exampleUrl(example: Map<String, Any?>): String {
            if (example.isEmpty()) {
                return "/project/default"
            }
            val open = if (example["latex"] != null || example["compute"] != null) "latex" else "markdown"
            val query = buildString {
                append("open=")
                append(encode(open))
                for (type in exampleTypes) {
                    val value = example[type] as? String ?: continue
                    if (value.isBlank()) {
                        continue
                    }
                    append('&')
                    append(type)
                    append('=')
                    append(encode(value))
                }
            }
            return "/project/default?$query"
        }

        private fun encode(value: String): String = URLEncoder.encode(value, StandardCharsets.UTF_8)
    }
}

data class ArticleTag(val id: String, val name: String)

data class ArticleHeading(val id: String, val title: String)

data class ArticleSection(val id: String, val title: String, val html: String)

data class ArticleTip(val title: String, val html: String)
