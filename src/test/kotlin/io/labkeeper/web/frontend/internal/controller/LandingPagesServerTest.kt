package io.labkeeper.web.frontend.internal.controller

import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldNotContain
import io.kotest.matchers.string.shouldStartWith
import io.labkeeper.web.frontend.local.LocalFrontendApplication
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
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
        body shouldContain "Labkeeper — онлайн-редактор LaTeX и Markdown с компиляцией в PDF"
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
        body shouldNotContain "labkeeper.io"
    }

    @Test
    fun blogPagesRenderArticlesFromYaml() {
        get("/about", MediaType.TEXT_HTML) shouldContain "<title>О нас — онлайн-редактор Labkeeper</title>"
        get("/blog", MediaType.TEXT_HTML) shouldContain "/blog/latex-formulas"
        get("/blog/ideal-lab-report-template-gost-online", MediaType.TEXT_HTML) shouldContain "<code>\${R}</code>"

        val article = get("/blog/latex-formulas", MediaType.TEXT_HTML)
        article shouldContain "Формулы и уравнения в"
        article shouldContain "amsmath"
        article shouldContain "window.__LABKEEPER_API_VERSION = \"v9\";"
        article shouldContain "https://labkeeper.test/blog/latex-formulas"
        article shouldContain "href=\"/blog/latex-amsthm\""
        article shouldContain "href=\"/blog/rinc-vak-scientific-article-template-fonts-margins\""
        article shouldNotContain "href=\"/blog/markdown-footnotes\""
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
        cv shouldContain "href=\"/templates/simple-hipster-cv\""
        cv shouldNotContain "href=\"/templates/automation-and-remote-control\""

        webTestClient.get().uri("/templates/missing-template").exchange().expectStatus().isNotFound
    }

    @Test
    fun projectPagesAreNotIndexed() {
        listOf("/project/default", "/project/aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee").forEach { path ->
            val body = webTestClient.get().uri(path).exchange()
                .expectStatus().isOk
                .expectBody<String>().returnResult().responseBody.orEmpty()
            body shouldContain "name=\"robots\" content=\"noindex, follow\""
        }
    }

    @Test
    fun staticAssetsAreServed() {
        get("/assets/css/style.css", MediaType.valueOf("text/css")) shouldContain "--c-primary"
        get("/assets/img/logo.svg") shouldContain "<svg"
        val robots = get("/robots.txt", MediaType.TEXT_PLAIN)
        robots shouldContain "Allow: /"
        robots shouldContain "Sitemap: https://labkeeper.test/sitemap.xml"
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
}
