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
    ],
)
class LandingPagesServerTest(
    @Autowired private val webTestClient: WebTestClient,
) {
    @Test
    fun homePageRendersSettingsAndFallbackProjects() {
        val body = get("/", MediaType.TEXT_HTML)

        body shouldContain "<!DOCTYPE html>"
        body shouldContain "Labkeeper — онлайн-редактор LaTeX и Markdown с компиляцией в PDF"
        body shouldContain "window.__LABKEEPER_API_VERSION = \"v9\";"
        body shouldContain "window.__LABKEEPER_OPENPANEL_CLIENT_ID = \"client\";"
        body shouldContain "window.__LABKEEPER_OPENPANEL_API_URL = \"https://openpanel.example\";"
        body shouldContain "ym(123456, \"init\""
        body shouldNotContain "IO_LABKEEPER_FRONTEND_"
        body shouldContain "href=\"assets/css/style.css\""
        body shouldContain "Измерение интенсивности радиационного фона"
        body shouldContain "header__projects"
        body shouldContain "display: none"
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
        article shouldNotContain "IO_LABKEEPER_FRONTEND_"

        val sitemap = get("/sitemap.xml", MediaType.APPLICATION_XML)
        sitemap shouldStartWith "<?xml"
        sitemap shouldContain "<loc>https://labkeeper.io/blog/latex-formulas</loc>"
    }

    @Test
    fun staticAssetsAreServed() {
        get("/assets/css/style.css", MediaType.valueOf("text/css")) shouldContain "--c-primary"
        get("/assets/img/logo.svg") shouldContain "<svg"
        get("/robots.txt", MediaType.TEXT_PLAIN) shouldContain "Allow: /"
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
    fun wikiRedirectsToTrailingSlash() {
        webTestClient.get().uri("/wiki").exchange()
            .expectStatus().isFound
            .expectHeader().location("/wiki/")
    }

    private fun ok(path: String, type: MediaType = MediaType.ALL) = webTestClient.get().uri(path).exchange()
        .expectStatus().isOk
        .expectHeader().contentTypeCompatibleWith(type)

    private fun get(path: String, type: MediaType = MediaType.ALL) =
        ok(path, type).expectBody<String>().returnResult().responseBody.orEmpty()
}
