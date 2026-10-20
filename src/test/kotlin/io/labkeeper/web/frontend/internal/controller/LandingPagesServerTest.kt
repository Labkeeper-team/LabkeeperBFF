package io.labkeeper.web.frontend.internal.controller

import io.labkeeper.web.frontend.local.LocalFrontendApplication
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.MediaType
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.reactive.server.WebTestClient

@ActiveProfiles("frontend-local")
@SpringBootTest(
    classes = [LocalFrontendApplication::class],
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    properties = [
        "io.labkeeper.version.major=4",
        "io.labkeeper.frontend.yandex-metrika-key=123456",
        "io.labkeeper.frontend.openpanel-client-id=client",
        "io.labkeeper.frontend.openpanel-api-url=https://openpanel.example",
        "io.labkeeper.host=http://localhost:8080",
    ],
)
class LandingPagesServerTest(
    @Autowired private val webTestClient: WebTestClient,
) {
    @Test
    fun homePageRendersPlaceholders() {
        val body = html("/")

        assertTrue(body.contains("<!DOCTYPE html>"))
        assertTrue(body.contains("Labkeeper — онлайн-редактор LaTeX и Markdown с компиляцией в PDF"))
        assertTrue(body.contains("window.__LABKEEPER_API_VERSION = \"v4\";"))
        assertTrue(body.contains("window.__LABKEEPER_OPENPANEL_CLIENT_ID = \"client\";"))
        assertTrue(body.contains("window.__LABKEEPER_OPENPANEL_API_URL = \"https://openpanel.example\";"))
        assertTrue(body.contains("ym(123456, \"init\""))
        assertFalse(body.contains("IO_LABKEEPER_FRONTEND_"))
        assertTrue(body.contains("href=\"assets/css/style.css\""))
        assertTrue(body.contains("Измерение интенсивности радиационного фона"))
        assertTrue(body.contains("header__projects"))
        assertTrue(body.contains("display: none"))
    }

    @Test
    fun aboutAndBlogKeepMarkup() {
        assertTrue(html("/about").contains("<title>О нас — онлайн-редактор Labkeeper</title>"))
        val article = html("/blog/ideal-lab-report-template-gost-online")
        assertTrue(article.contains("<code>\${R}</code>"))
        assertTrue(article.contains("window.__LABKEEPER_API_VERSION = \"v4\";"))
        assertFalse(article.contains("IO_LABKEEPER_FRONTEND_"))
    }

    @Test
    fun staticAssetsAndRawHtml() {
        val css = webTestClient.get().uri("/assets/css/style.css")
            .exchange()
            .expectStatus().isOk
            .expectHeader().contentTypeCompatibleWith(MediaType.valueOf("text/css"))
            .expectBody(String::class.java)
            .returnResult()
            .responseBody
        assertTrue(css!!.contains("--c-primary"))

        webTestClient.get().uri("/assets/img/logo.svg")
            .exchange()
            .expectStatus().isOk
            .expectBody(String::class.java)
            .returnResult()
            .responseBody
            .let { assertTrue(it!!.contains("<svg")) }

        webTestClient.get().uri("/robots.txt")
            .exchange()
            .expectStatus().isOk
            .expectBody(String::class.java)
            .returnResult()
            .responseBody
            .let { assertTrue(it!!.contains("Allow: /")) }

        webTestClient.get().uri("/index.html").exchange().expectStatus().isNotFound
        webTestClient.get().uri("/blog/missing-article").exchange().expectStatus().isNotFound
        webTestClient.get().uri("/blog/Not_A_Slug").exchange().expectStatus().isNotFound

        val article = html("/blog/latex-formulas")
        assertTrue(article.contains("Формулы и уравнения в"))
        assertTrue(article.contains("amsmath"))
        assertFalse(article.contains("IO_LABKEEPER_FRONTEND_"))
        assertTrue(html("/blog/ideal-lab-report-template-gost-online").contains("<code>\${R}</code>"))
        assertTrue(html("/blog").contains("/blog/latex-formulas"))
        val sitemap = webTestClient.get().uri("/sitemap.xml")
            .exchange()
            .expectStatus().isOk
            .expectBody(String::class.java)
            .returnResult()
            .responseBody
        assertTrue(sitemap!!.contains("https://labkeeper.io/blog/latex-formulas"))
    }

    @Test
    fun wikiRedirectsToTrailingSlash() {
        webTestClient.get().uri("/wiki")
            .exchange()
            .expectStatus().isFound
            .expectHeader().value("Location") { location -> assertTrue(location.endsWith("/wiki/")) }
    }

    private fun html(path: String): String {
        val body = webTestClient.get().uri(path)
            .exchange()
            .expectStatus().isOk
            .expectHeader().contentTypeCompatibleWith(MediaType.TEXT_HTML)
            .expectBody(String::class.java)
            .returnResult()
            .responseBody
        assertTrue(body != null && body.isNotEmpty())
        return body!!
    }
}

@SpringBootTest(
    classes = [LocalFrontendApplication::class],
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    properties = [
        "io.labkeeper.frontend.local-application=true",
        "io.labkeeper.frontend.static-directory=off",
        "io.labkeeper.version.major=9",
        "io.labkeeper.frontend.yandex-metrika-key=999",
        "io.labkeeper.frontend.openpanel-client-id=classpath-client",
        "io.labkeeper.frontend.openpanel-api-url=https://classpath.example",
    ],
)
class ClasspathLandingPagesTest(
    @Autowired private val webTestClient: WebTestClient,
) {
    @Test
    fun packagedPagesRenderThroughThymeleaf() {
        val body = webTestClient.get().uri("/")
            .exchange()
            .expectStatus().isOk
            .expectHeader().contentTypeCompatibleWith(MediaType.TEXT_HTML)
            .expectBody(String::class.java)
            .returnResult()
            .responseBody
        assertTrue(body!!.contains("window.__LABKEEPER_API_VERSION = \"v9\";"))
        assertTrue(body.contains("ym(999, \"init\""))
        assertTrue(body.contains("classpath-client"))
        assertFalse(body.contains("IO_LABKEEPER_FRONTEND_"))
        webTestClient.get().uri("/assets/js/swiper.js")
            .exchange()
            .expectStatus().isOk
            .expectHeader().contentTypeCompatibleWith(MediaType.valueOf("text/javascript"))
        webTestClient.get().uri("/assets/css/swiper.css")
            .exchange()
            .expectStatus().isOk
            .expectHeader().contentTypeCompatibleWith(MediaType.valueOf("text/css"))

        webTestClient.get().uri("/assets/css/style.css").exchange().expectStatus().isOk
    }
}
