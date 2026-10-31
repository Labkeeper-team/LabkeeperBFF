package io.labkeeper.web.frontend.internal.blog

import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.beans.factory.annotation.Value
import org.springframework.core.io.support.PathMatchingResourcePatternResolver
import org.springframework.stereotype.Component
import org.yaml.snakeyaml.Yaml
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

    val lastModified: String? = articles.maxOfOrNull { it.dateModified }

    val sliderJson = scriptJson(
        articles.map { mapOf("url" to "/blog/${it.slug}", "title" to it.sliderTitle, "text" to it.sliderText) },
    )

    fun find(slug: String) = articles.firstOrNull { it.slug == slug }

    fun exampleProgram(slug: String) = find(slug)?.exampleSegments

    /** Links listed in the article YAML under `related`, in that order. */
    fun relatedTo(page: BlogArticle): List<ArticleLink> {
        val bySlug = articles.associateBy { it.slug }
        return page.relatedSlugs.map { slug ->
            val target = bySlug[slug] ?: error("Blog article '${page.slug}' links to unknown article '$slug'")
            ArticleLink("/blog/${target.slug}", plain(target.sliderTitle), plain(target.sliderText))
        }
    }

    init {
        articles.forEach(::relatedTo)
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
    val relatedSlugs = slugList(yaml, "related", slug)
    val categoryAttr = categoryIds.joinToString(" ")
    val categoryNames = categoryIds.map { CATEGORY_NAMES[it] ?: it }
    val tags = categoryIds.zip(categoryNames, ::ArticleTag)
    val dateModified = yaml["dateModified"] as String
    val dateDisplay: String = LocalDate.parse(dateModified).format(DATE_FORMAT)
    val exampleSegments = exampleSegments(yaml["editorExample"], slug)
    val editorExampleUrl = "/project/default?example=$slug"
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

data class BlogExampleSegment(
    val type: String,
    val text: String,
    val parameters: BlogExampleParameters = BlogExampleParameters(),
)

data class BlogExampleParameters(
    val visible: Boolean = true,
    val hideAssignment: Boolean = false,
    val hideAssignmentWithValues: Boolean = false,
    val hideValue: Boolean = false,
    val hideGeneralFormula: Boolean = false,
    val hideInflAssignment: Boolean = false,
    val hideInflAssignmentWithValues: Boolean = false,
    val hideInfl: Boolean = false,
)

private const val PUBLIC_URL_TOKEN = "{{publicUrl}}"
private const val PUBLIC_HOST_TOKEN = "{{publicHost}}"
private val JSON = ObjectMapper()
private val WHITESPACE = Regex("\\s+")
private val DATE_FORMAT = DateTimeFormatter.ofPattern("d MMMM yyyy", Locale.forLanguageTag("ru"))
private val EXAMPLE_TYPES = listOf("compute", "latex", "markdown")
private val SEGMENT_TYPES = mapOf(
    "compute" to "computational",
    "computational" to "computational",
    "latex" to "latex",
    "markdown" to "md",
    "md" to "md",
    "asciimath" to "asciimath",
    "mermaid" to "mermaid",
    "plantuml" to "plantuml",
    "drawio" to "drawio",
)
private val PARAMETER_NAMES = setOf(
    "visible",
    "hideAssignment",
    "hideAssignmentWithValues",
    "hideValue",
    "hideGeneralFormula",
    "hideInflAssignment",
    "hideInflAssignmentWithValues",
    "hideInfl",
)
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

private fun slugList(yaml: Map<String, Any?>, key: String, owner: String): List<String> {
    val raw = yaml[key] as? List<*> ?: error("Blog article '$owner' is missing '$key'")
    return raw.map { item ->
        val slug = item as? String ?: error("Blog article '$owner' has a non-text $key entry")
        if (slug.isBlank() || slug == owner) error("Blog article '$owner' has an invalid $key entry '$slug'")
        slug
    }
}

internal fun exampleSegments(example: Any?, slug: String): List<BlogExampleSegment> = when (example) {
    is Map<*, *> -> exampleSegmentsFromMap(example, slug)
    is List<*> -> {
        if (example.isEmpty()) error("Blog article '$slug' has an empty editor example")
        example.mapIndexed { index, item -> exampleSegment(item, slug, index) }
    }
    null -> error("Blog article '$slug' is missing an editor example")
    else -> error("Blog article '$slug' has an invalid editor example")
}

private fun exampleSegmentsFromMap(example: Map<*, *>, slug: String): List<BlogExampleSegment> {
    if (example.isEmpty()) error("Blog article '$slug' has an empty editor example")
    val unknown = example.keys.filter { it !in EXAMPLE_TYPES }
    if (unknown.isNotEmpty()) error("Blog article '$slug' has unknown editor example keys $unknown")
    return EXAMPLE_TYPES.mapNotNull { type ->
        if (type !in example) return@mapNotNull null
        val text = example[type] as? String ?: error("Blog article '$slug' has an empty $type example")
        if (text.isBlank()) error("Blog article '$slug' has an empty $type example")
        BlogExampleSegment(type = SEGMENT_TYPES.getValue(type), text = text)
    }
}

private fun exampleSegment(item: Any?, slug: String, index: Int): BlogExampleSegment {
    val segment = item as? Map<*, *> ?: error("Blog article '$slug' example segment ${index + 1} must be an object")
    val unknown = segment.keys.filter { it != "type" && it != "text" && it != "parameters" }
    if (unknown.isNotEmpty()) error("Blog article '$slug' example segment ${index + 1} has unknown fields $unknown")
    val typeName = segment["type"] as? String ?: error("Blog article '$slug' example segment ${index + 1} is missing type")
    val type = SEGMENT_TYPES[typeName] ?: error("Blog article '$slug' example segment ${index + 1} has unknown type '$typeName'")
    val text = segment["text"] as? String ?: error("Blog article '$slug' example segment ${index + 1} is missing text")
    if (text.isBlank()) error("Blog article '$slug' example segment ${index + 1} has empty text")
    return BlogExampleSegment(type, text, exampleParameters(segment["parameters"], slug, index))
}

private fun exampleParameters(raw: Any?, slug: String, index: Int): BlogExampleParameters {
    if (raw == null) return BlogExampleParameters()
    val parameters = raw as? Map<*, *>
        ?: error("Blog article '$slug' example segment ${index + 1} parameters must be an object")
    val unknown = parameters.keys.filter { it !in PARAMETER_NAMES }
    if (unknown.isNotEmpty()) {
        error("Blog article '$slug' example segment ${index + 1} has unknown parameters $unknown")
    }
    fun flag(name: String, default: Boolean): Boolean {
        if (name !in parameters) return default
        return parameters[name] as? Boolean
            ?: error("Blog article '$slug' example segment ${index + 1} parameter '$name' must be true or false")
    }
    return BlogExampleParameters(
        visible = flag("visible", true),
        hideAssignment = flag("hideAssignment", false),
        hideAssignmentWithValues = flag("hideAssignmentWithValues", false),
        hideValue = flag("hideValue", false),
        hideGeneralFormula = flag("hideGeneralFormula", false),
        hideInflAssignment = flag("hideInflAssignment", false),
        hideInflAssignmentWithValues = flag("hideInflAssignmentWithValues", false),
        hideInfl = flag("hideInfl", false),
    )
}
