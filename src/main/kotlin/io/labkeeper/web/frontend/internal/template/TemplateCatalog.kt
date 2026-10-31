package io.labkeeper.web.frontend.internal.template

import com.fasterxml.jackson.databind.ObjectMapper
import io.labkeeper.web.frontend.internal.blog.ArticleTag
import org.springframework.beans.factory.annotation.Value
import org.springframework.core.io.support.PathMatchingResourcePatternResolver
import org.springframework.stereotype.Component
import org.yaml.snakeyaml.Yaml
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale
import java.util.UUID

/** Public template pages loaded from `classpath:catalog/templates/**/*.yaml`. */
@Component
class TemplateCatalog(@Value($$"${io.labkeeper.host}") host: String) {
    val templates: List<TemplatePage> = PathMatchingResourcePatternResolver()
        .getResources("classpath*:catalog/templates/**/*.yaml")
        .map { resource ->
            val text = resource.inputStream.bufferedReader().use { it.readText() }
                .replace(PUBLIC_URL_TOKEN, host)
                .replace(PUBLIC_HOST_TOKEN, host)
            val name = resource.filename ?: "template.yaml"
            TemplatePage(Yaml().load(text), host, name)
        }
        .sortedBy { it.catalogIndex }

    val lastModified: String? = templates.mapNotNull { it.dateModified }.maxOrNull()

    val sliderJson = scriptJson(
        templates.map { mapOf("url" to it.canonicalPath, "title" to it.sliderTitle, "text" to it.sliderText) },
    )

    fun find(slug: String) = templates.firstOrNull { it.slug == slug }

    /** Journals link to other journals, CV templates link to other CV templates. The English ARC page stays next to the Russian AIT page. */
    fun relatedTo(page: TemplatePage): List<TemplateLink> {
        val cv = "cv" in page.categoryIds
        val companion = COMPANION_SLUG[page.slug]
        return templates
            .filter { it.slug != page.slug && ("cv" in it.categoryIds) == cv }
            .sortedWith(compareBy({ if (it.slug == companion) 0 else 1 }, { it.catalogIndex }))
            .map { TemplateLink(it.canonicalPath, it.sliderTitle, it.sliderText) }
    }
}

data class TemplateLink(val href: String, val title: String, val text: String)

@Suppress("UNCHECKED_CAST")
class TemplatePage(yaml: Map<String, Any?>, siteUrl: String, sourceName: String) {
    val slug = required(yaml, sourceName, "slug")
    val projectId = required(yaml, sourceName, "projectId").also { id ->
        runCatching { UUID.fromString(id) }.getOrElse {
            error("Template '$sourceName' has an invalid projectId '$id'")
        }
    }
    val catalogIndex = (yaml["catalogIndex"] as Number?)?.toInt() ?: 0
    val cardTitle = collapseWhitespace(required(yaml, sourceName, "cardTitle"))
    val cardDescription = collapseWhitespace(required(yaml, sourceName, "cardDescription"))
    val pageTitle = text(yaml, "pageTitle") ?: "$cardTitle — Labkeeper"
    val metaDescription = text(yaml, "metaDescription") ?: cardDescription
    val h1 = text(yaml, "h1") ?: cardTitle
    val breadcrumbTitle = text(yaml, "breadcrumbTitle") ?: cardTitle
    val readingTime = text(yaml, "readingTime").orEmpty()
    val sliderTitle = text(yaml, "sliderTitle") ?: cardTitle
    val sliderText = text(yaml, "sliderText") ?: cardDescription
    val sidebarText = text(yaml, "sidebarText") ?: cardDescription
    val ctaTitle = text(yaml, "ctaTitle") ?: "Откройте шаблон"
    val ctaText = text(yaml, "ctaText") ?: cardDescription
    val toc = (yaml["toc"] as List<Map<String, String>>?).orEmpty()
    val sections = (yaml["sections"] as List<Map<String, String>>?) ?: listOf(
        mapOf("id" to "about", "title" to cardTitle, "html" to "<p>${escapeHtml(cardDescription)}</p>"),
    )
    val tips = (yaml["tips"] as List<Map<String, String>>?).orEmpty()
    val sidebarImageAlt = text(yaml, "sidebarImageAlt") ?: "Интерфейс онлайн-редактора Labkeeper"
    val ogTitle = text(yaml, "ogTitle") ?: pageTitle
    val ogDescription = text(yaml, "ogDescription") ?: metaDescription
    val twitterTitle = text(yaml, "twitterTitle") ?: pageTitle
    val twitterDescription = text(yaml, "twitterDescription") ?: metaDescription
    val keywordsText = (yaml["keywords"] as List<String>?).orEmpty().joinToString(", ")
    val categoryIds = (yaml["categories"] as List<String>?).orEmpty()
    val categoryAttr = categoryIds.joinToString(" ")
    val categoryNames = categoryIds.map { CATEGORY_NAMES[it] ?: it }
    val tags = categoryIds.zip(categoryNames, ::ArticleTag)
    val dateModified = text(yaml, "dateModified")
    val dateDisplay = dateModified?.let { LocalDate.parse(it).format(DATE_FORMAT) }.orEmpty()
    val openUrl = "/project/$projectId"
    val opensProject = true
    val canonicalPath = "/templates/$slug"
    val catalogHref = "/templates"
    val catalogLabel = "Шаблоны"
    val relatedTitle = "Другие шаблоны"
    val relatedHref = "/templates"
    val relatedLabel = "Все шаблоны"
    private val url = "$siteUrl$canonicalPath"

    val articleJsonLd = scriptJson(
        buildMap {
            put("@context", "https://schema.org")
            put("@type", "Article")
            put("headline", text(yaml, "title") ?: cardTitle)
            put("description", text(yaml, "schemaDescription") ?: metaDescription)
            put("author", mapOf("@type" to "Organization", "name" to "Labkeeper", "url" to siteUrl))
            put(
                "publisher",
                mapOf(
                    "@type" to "Organization",
                    "name" to "Labkeeper",
                    "logo" to mapOf("@type" to "ImageObject", "url" to "$siteUrl/assets/img/logo.svg"),
                ),
            )
            put("mainEntityOfPage", mapOf("@type" to "WebPage", "@id" to url))
            text(yaml, "datePublished")?.let { put("datePublished", it) }
            dateModified?.let { put("dateModified", it) }
            put("inLanguage", "ru")
            put("articleSection", categoryNames)
        },
    )

    val breadcrumbJsonLd = scriptJson(
        mapOf(
            "@context" to "https://schema.org",
            "@type" to "BreadcrumbList",
            "itemListElement" to listOf(
                "Главная" to "$siteUrl/",
                catalogLabel to "$siteUrl$catalogHref",
                (text(yaml, "schemaBreadcrumbTitle") ?: breadcrumbTitle) to url,
            ).mapIndexed { index, (name, item) ->
                mapOf("@type" to "ListItem", "position" to index + 1, "name" to name, "item" to item)
            },
        ),
    )
}

private val COMPANION_SLUG = mapOf(
    "avtomatika-i-telemekhanika" to "automation-and-remote-control",
    "automation-and-remote-control" to "avtomatika-i-telemekhanika",
)

private const val PUBLIC_URL_TOKEN = "{{publicUrl}}"
private const val PUBLIC_HOST_TOKEN = "{{publicHost}}"
private val JSON = ObjectMapper()
private val WHITESPACE = Regex("\\s+")
private val DATE_FORMAT = DateTimeFormatter.ofPattern("d MMMM yyyy", Locale.forLanguageTag("ru"))
private val CATEGORY_NAMES = mapOf(
    "latex" to "Работа с LaTeX",
    "markdown" to "Работа с Markdown",
    "labs" to "Лабораторные работы",
    "diploma" to "Дипломные работы",
    "cv" to "Оформление резюме / CV",
    "articles" to "Оформление научных статей",
    "journal-ras" to "Оформление научных статей / Журналы РАН",
    "journal-vak" to "Оформление научных статей / Журналы ВАК",
    "msu-bulletin" to "Вестник Московского Университета",
    "msu-phys" to "Серия 3. Физика. Астрономия",
    "mupbsamp" to "MUPBsamp.tex",
    "revtex" to "revtex4-2",
    "vak" to "Статья ВАК",
    "wos-scopus" to "Оформление научных статей / Web of Science (WoS) / Scopus",
    "mdpi-template" to "MDPI Article Template",
    "mdpi" to "mdpi",
    "springer-nature" to "Springer Nature LaTeX Template",
    "sn-article" to "sn-article-template",
    "sn-jnl" to "sn-jnl",
    "ssu-izvestiya" to "Известия Саратовского университета",
    "izv-mmi" to "izv_mmi",
    "svmo-journal" to "Журнал Средневолжского математического общества",
    "arxiv-svmo" to "ArxivZhurnalSVMOru",
    "svmo" to "svmo",
    "arc" to "Automation and Remote Control",
    "ait" to "Автоматика и телемеханика",
)

private fun scriptJson(value: Any) = JSON.writeValueAsString(value).replace("<", "\\u003c")

private fun collapseWhitespace(text: String) = text.replace(WHITESPACE, " ").trim()

private fun text(yaml: Map<String, Any?>, key: String) = (yaml[key] as String?)?.let(::collapseWhitespace)

private fun required(yaml: Map<String, Any?>, sourceName: String, key: String): String {
    val value = yaml[key] as String?
    if (value.isNullOrBlank()) {
        error("Template '$sourceName' is missing '$key'")
    }
    return value
}

private fun escapeHtml(text: String) = text
    .replace("&", "&amp;")
    .replace("<", "&lt;")
    .replace(">", "&gt;")
