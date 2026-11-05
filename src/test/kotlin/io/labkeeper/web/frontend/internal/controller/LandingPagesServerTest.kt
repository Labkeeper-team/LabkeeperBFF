package io.labkeeper.web.frontend.internal.controller

import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldEndWith
import io.kotest.matchers.string.shouldNotContain
import io.kotest.matchers.string.shouldStartWith
import io.labkeeper.web.frontend.local.LocalFrontendApplication
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.reactive.server.WebTestClient
import org.springframework.test.web.reactive.server.expectBody

@ActiveProfiles("frontend-local")
@SpringBootTest(
    classes = [LocalFrontendApplication::class],
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    properties = [
        "io.labkeeper.version.major=9",
        "io.labkeeper.frontend.yandex-metrika-key=123456",
        "io.labkeeper.frontend.openpanel-client-id=client",
        "io.labkeeper.frontend.openpanel-api-url=https://openpanel.example",
        "io.labkeeper.host=https://labkeeper.test",
        "io.labkeeper.frontend.contact-email=hello@labkeeper.test",
        "io.labkeeper.frontend.contact-phone=+1-555-0100",
        "io.labkeeper.frontend.proxy-host=",
    ],
)
class LandingPagesServerTest(
    @Autowired private val webTestClient: WebTestClient,
) {
    @Test
    fun homePageRendersSettings() {
        val body = get("/", MediaType.TEXT_HTML)

        body shouldContain "<!doctype html>"
        body shouldContain Regex("<title>\\s*Онлайн-редактор документов и LaTeX с ИИ-агентом — Labkeeper\\s*</title>")
        body shouldContain "window.__LABKEEPER_API_VERSION = 'v9';"
        body shouldContain "window.__LABKEEPER_OPENPANEL_CLIENT_ID = 'client';"
        body shouldContain "window.__LABKEEPER_OPENPANEL_API_URL = 'https://openpanel.example';"
        body shouldContain "ym(123456, \"init\""
        body shouldNotContain "IO_LABKEEPER_FRONTEND_"
        body shouldContain "href=\"assets/css/style.css\""
        body shouldNotContain "id=\"examples\""
        body shouldContain "href=\"/templates\""
        body shouldContain "href=\"/wiki/index.html\""
        body shouldNotContain "github.com/Labkeeper-team/Docs/wiki"
        body shouldContain "Шаблон статьи для журнала «Автоматика и телемеханика»"
        body shouldContain "/project/e7e8765e-3f85-4e3a-aac9-301c62099c5c"
        body shouldContain "header__projects"
        body shouldContain "display: none"
        body shouldContain "https://labkeeper.test/"
        body shouldContain "hello@labkeeper.test"
        body shouldContain "window.__LABKEEPER_PUBLIC_URL = 'https://labkeeper.test';"
        body shouldContain "connect-src &#39;self&#39; https://mc.yandex.ru " +
            "https://labkeeper.test https://openpanel.example;"
        body shouldNotContain "labkeeper.io"

        // The server renders the texts, lang.js no longer has them. Whitespace around them depends on the formatter.
        body shouldContain "<html lang=\"ru\">"
        body shouldContain Regex("class=\"hero__title h1\"\\s*>\\s*Создавайте документы")
        body shouldContain Regex("class=\"header__nav-link\"\\s*>\\s*Шаблоны\\s*</a")
        body shouldContain Regex("class=\"header__lang-btn header__lang-btn--active\"\\s+data-lang=\"ru\"")
        body shouldNotContain "data-i18n"
    }

    @Test
    fun languageCookieSwitchesTheTexts() {
        listOf("/", "/about", "/blog", "/templates", "/blog/latex-formulas", "/templates/mdpi").forEach { path ->
            val body = webTestClient.get().uri(path).cookie("labkeeper_lang", "en").exchange()
                .expectStatus().isOk
                .expectBody<String>().returnResult().responseBody.orEmpty()
            withClue(path) {
                body shouldContain "<html lang=\"en\">"
                body shouldContain Regex("class=\"header__nav-link[^\"]*\"\\s*>\\s*Templates\\s*</a")
                body shouldContain Regex("class=\"header__lang-btn header__lang-btn--active\"\\s+data-lang=\"en\"")
                body shouldContain Regex("class=\"header__lang-btn\"\\s+data-lang=\"ru\"")
            }
        }

        // Search engines come without the cookie. An unknown value is Russian too.
        val unknown = webTestClient.get().uri("/").cookie("labkeeper_lang", "de").exchange()
            .expectBody<String>().returnResult().responseBody.orEmpty()
        unknown shouldContain "<html lang=\"ru\">"
        unknown shouldContain Regex("class=\"hero__title h1\"\\s*>\\s*Создавайте документы")
    }

    @Test
    fun publicPagesHaveRenderedMeta() {
        listOf(
            "/", "/blog", "/templates", "/about", "/privacy", "/oferta", "/soglas", "/sogl_adv", "/sogl_ds",
            "/sogl_yam", "/blog/latex-formulas", "/templates/mdpi",
        ).forEach { path ->
            val body = get(path, MediaType.TEXT_HTML).replace(WHITESPACE, " ")
            withClue(path) {
                body shouldNotContain "[(\${"
                body shouldContain "<link rel=\"canonical\" href=\"https://labkeeper.test$path\""
                body shouldContain "property=\"og:url\" content=\"https://labkeeper.test$path\""
                OG_IMAGE.find(body)?.groupValues?.get(1).orEmpty() shouldEndWith ".png"
                body shouldNotContain "og-image.svg"
                body shouldContain "href=\"mailto:hello@labkeeper.test\""
            }
        }
    }

    @Test
    fun blogPagesRenderArticlesFromYaml() {
        get("/about", MediaType.TEXT_HTML).also {
            it shouldContain "<title>О нас — онлайн-редактор Labkeeper</title>"
            it shouldContain "connect-src &#39;self&#39; https://mc.yandex.ru https://openpanel.example;"
        }
        get("/blog", MediaType.TEXT_HTML) shouldContain "/blog/latex-formulas"
        get("/blog/ideal-lab-report-template-gost-online", MediaType.TEXT_HTML) shouldContain "<code>\${R}</code>"

        val article = get("/blog/latex-formulas", MediaType.TEXT_HTML)
        article shouldContain "Формулы и уравнения в"
        article shouldContain "amsmath"
        article shouldContain "window.__LABKEEPER_API_VERSION = \"v9\";"
        article shouldContain "https://labkeeper.test/blog/latex-formulas"
        article shouldContain "href=\"/project/default?example=latex-formulas\""
        article shouldNotContain "latex="
        article shouldNotContain "compute="
        article shouldNotContain "markdown="
        article shouldContain "href=\"/blog/latex-amsthm\""
        article shouldContain "href=\"/blog/rinc-vak-scientific-article-template-fonts-margins\""
        article shouldNotContain "href=\"/blog/markdown-footnotes\""
        article shouldContain "\"image\":\"https://labkeeper.test/assets/img/og-image.png\""
        article shouldContain "\"url\":\"https://labkeeper.test/assets/img/logo.png\""
        article shouldNotContain "IO_LABKEEPER_FRONTEND_"
        article shouldNotContain "labkeeper.io"

        val cited = get("/blog/latex-hyperref-pdf-bookmarks-navigation", MediaType.TEXT_HTML)
        cited shouldContain "https://labkeeper.test"
        cited shouldNotContain "{{publicUrl}}"
        cited shouldNotContain "labkeeper.io"

        val sitemap = get("/sitemap.xml", MediaType.APPLICATION_XML)
        sitemap shouldStartWith "<?xml"
        sitemap shouldContain "<loc>https://labkeeper.test/blog/latex-formulas</loc>"
        sitemap shouldContain "<lastmod>2026-10-18</lastmod>"
        sitemap shouldContain "<loc>https://labkeeper.test/templates</loc>"
        sitemap shouldContain "<loc>https://labkeeper.test/templates/avtomatika-i-telemekhanika</loc>"
        sitemap shouldContain "<loc>https://labkeeper.test/wiki/index.html</loc>"
        sitemap shouldContain "<loc>https://labkeeper.test/wiki/language/syntax.html</loc>"
        sitemap shouldNotContain "/wiki/_search"
        sitemap.replace("\r\n", "\n") shouldContain
            "<loc>https://labkeeper.test/templates/rezyume-1</loc>\n    <lastmod>2026-10-30</lastmod>"
        sitemap shouldNotContain "labkeeper.io"
    }

    @Test
    fun templatePagesOpenTheProject() {
        val catalog = get("/templates", MediaType.TEXT_HTML)
        catalog shouldContain "Шаблоны Labkeeper — готовые проекты для LaTeX и Markdown"
        catalog shouldContain "Шаблон статьи для журнала «Автоматика и телемеханика»"
        catalog shouldContain "href=\"/templates/avtomatika-i-telemekhanika\""
        catalog shouldContain "href=\"/project/e7e8765e-3f85-4e3a-aac9-301c62099c5c\""
        catalog shouldContain "Открыть шаблон"
        catalog shouldNotContain "Тестовый шаблон отчёта"
        catalog shouldNotContain "labkeeper.io"

        val template = get("/templates/avtomatika-i-telemekhanika", MediaType.TEXT_HTML)
        template shouldContain "Шаблон статьи «Автоматика и телемеханика» в LaTeX — Labkeeper"
        template shouldContain "Открыть шаблон"
        template shouldContain "href=\"/project/e7e8765e-3f85-4e3a-aac9-301c62099c5c\""
        template shouldContain "https://labkeeper.test/templates/avtomatika-i-telemekhanika"
        template shouldContain "href=\"/templates/automation-and-remote-control\""
        template shouldContain "href=\"/templates/vestnik-mgu-fizika\""
        template shouldNotContain "href=\"/templates/altacv\""
        template shouldNotContain "labkeeper.io"

        val cv = get("/templates/rezyume-1", MediaType.TEXT_HTML)
        cv shouldContain "href=\"/templates/altacv\""
        cv shouldContain "src=\"/assets/img/templates/rezyume-1.png\""
        cv shouldContain "href=\"/templates/simple-hipster-cv\""
        cv shouldNotContain "href=\"/templates/automation-and-remote-control\""

        webTestClient.get().uri("/templates/missing-template").exchange().expectStatus().isNotFound
    }

    @Test
    fun projectPagesAreNotIndexed() {
        listOf(
            "/project/default",
            "/project/default/agent",
            "/project/aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee",
            "/project/aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee/agent",
            "/projects",
            "/tokens",
        ).forEach { path ->
            val body = webTestClient.get().uri(path).exchange()
                .expectStatus().isOk
                // Crawlers skip the meta tag of a text/plain page, so the type and the header matter too.
                .expectHeader().contentTypeCompatibleWith(MediaType.TEXT_HTML)
                .expectHeader().valueEquals("X-Robots-Tag", "noindex, follow")
                .expectBody<String>().returnResult().responseBody.orEmpty()
            withClue(path) { body shouldContain "name=\"robots\" content=\"noindex, follow\"" }
        }
    }

    @Test
    fun staticAssetsAreServed() {
        get("/assets/css/style.css", MediaType.valueOf("text/css")) shouldContain "--c-primary"
        get("/assets/img/logo.svg") shouldContain "<svg"
        ok("/assets/img/logo.png", MediaType.IMAGE_PNG)
        ok("/assets/img/og-image.png", MediaType.IMAGE_PNG)
        val robots = get("/robots.txt", MediaType.TEXT_PLAIN)
        robots shouldBe """
            User-agent: *
            Disallow: /api/
            Disallow: /projects$
            Disallow: /projects?
            Disallow: /projects/
            Disallow: /tokens$
            Disallow: /tokens?
            Disallow: /pay$
            Disallow: /pay?
            Disallow: /oauth2/
            Disallow: /qr/

            Sitemap: https://labkeeper.test/sitemap.xml

        """.trimIndent()
        ok("/assets/js/swiper.js", MediaType.valueOf("text/javascript"))
        ok("/assets/css/swiper.css", MediaType.valueOf("text/css"))
        ok("/assets/js/katex.js", MediaType.valueOf("text/javascript"))
        ok("/assets/css/katex.css", MediaType.valueOf("text/css"))
        ok("/assets/css/fonts/KaTeX_Main-Regular.woff2")
    }

    @Test
    fun unknownPagesAreNotFound() {
        listOf("/index.html", "/blog/missing-article", "/blog/Not_A_Slug").forEach {
            webTestClient.get().uri(it).exchange().expectStatus().isNotFound
        }

        val page = webTestClient.get().uri("/blog/missing-article").accept(MediaType.TEXT_HTML).exchange()
            .expectStatus().isNotFound
            .expectHeader().contentTypeCompatibleWith(MediaType.TEXT_HTML)
            .expectBody<String>().returnResult().responseBody.orEmpty()
        page shouldContain "Страница не найдена"
        page shouldContain "href=\"/blog\""
        page shouldContain "name=\"robots\" content=\"noindex, follow\""
    }

    @Test
    fun trailingSlashRedirectsToThePageWithoutIt() {
        mapOf(
            "/blog/" to "/blog",
            "/templates/" to "/templates",
            "/about/" to "/about",
            "/privacy/" to "/privacy",
            "/blog/latex-formulas/" to "/blog/latex-formulas",
            "/templates/mdpi/?utm_source=test" to "/templates/mdpi?utm_source=test",
        ).forEach { (from, to) ->
            webTestClient.get().uri(from).exchange()
                .expectStatus().isEqualTo(HttpStatus.MOVED_PERMANENTLY)
                .expectHeader().location(to)
        }
    }

    @Test
    fun wikiIsServedAsStaticFiles() {
        listOf("/wiki", "/wiki/").forEach { path ->
            webTestClient.get().uri(path).exchange()
                .expectStatus().isFound
                .expectHeader().location("/wiki/index.html")
        }

        get("/wiki/index.html", MediaType.TEXT_HTML) shouldContain "<title>Labkeeper.io — документация"
        get("/wiki/language/syntax.html", MediaType.TEXT_HTML) shouldContain "<title>Синтаксис"
        ok("/wiki/toc.js", MediaType.valueOf("text/javascript"))
        webTestClient.get().uri("/wiki/language/syntax").exchange().expectStatus().isNotFound
    }

    private fun ok(path: String, type: MediaType = MediaType.ALL) = webTestClient.get().uri(path).exchange()
        .expectStatus().isOk
        .expectHeader().contentTypeCompatibleWith(type)

    private fun get(path: String, type: MediaType = MediaType.ALL) =
        ok(path, type).expectBody<String>().returnResult().responseBody.orEmpty()

    private companion object {
        val WHITESPACE = Regex("\\s+")
        val OG_IMAGE = Regex("property=\"og:image\" content=\"([^\"]*)\"")
    }
}
