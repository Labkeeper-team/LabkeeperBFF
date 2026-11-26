package io.labkeeper.web.frontend.internal

import io.kotest.assertions.withClue
import io.kotest.matchers.collections.shouldNotBeEmpty
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldNotContain
import io.kotest.matchers.string.shouldStartWith
import io.labkeeper.web.frontend.local.LocalFrontendApplication
import org.junit.jupiter.api.Test
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.server.LocalServerPort
import org.springframework.core.io.support.PathMatchingResourcePatternResolver
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.client.reactive.ReactorClientHttpConnector
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.reactive.server.WebTestClient
import reactor.netty.http.client.HttpClient

/** Issue Labkeeper-team/LabkeeperEditor#285: a full load of the editor downloaded about 10 MB every time. */
@ActiveProfiles("frontend-local")
@SpringBootTest(
    classes = [LocalFrontendApplication::class],
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    properties = [
        "io.labkeeper.version.major=9",
        "io.labkeeper.host=https://labkeeper.test",
        "io.labkeeper.frontend.proxy-host=",
    ],
)
class StaticFilesServerTest(
    @LocalServerPort private val port: Int,
) {
    /** The default test client asks for gzip on its own and unpacks the answer, which drops Content-Encoding. */
    private val webTestClient by lazy {
        WebTestClient.bindToServer(ReactorClientHttpConnector(HttpClient.create().compress(false)))
            .baseUrl("http://localhost:$port")
            .build()
    }

    @Test
    fun editorBuildFilesAreCachedForGood() {
        listOf(editorFile("index-*.js"), editorFile("index-*.css"), editorFile("*.ttf")).forEach { path ->
            withClue(path) {
                val headers = headersOf(path)
                val cacheControl = headers.cacheControl.shouldNotBeNull()
                cacheControl shouldContain "max-age=31536000"
                cacheControl shouldContain "public"
                cacheControl shouldContain "immutable"
                cacheControl shouldNotContain "no-store"
                headers.pragma.shouldBeNull()
                headers.getFirst(HttpHeaders.EXPIRES).shouldBeNull()
            }
        }
    }

    /** Landing files keep their names across releases: a year of cache would hide every change. */
    @Test
    fun filesWithoutHashAreRevalidated() {
        listOf("/assets/js/lang.js", "/assets/css/style.css", "/mathjax/loader.js").forEach { path ->
            withClue(path) {
                val headers = headersOf(path)
                headers.cacheControl shouldBe "no-cache"
                val etag = headers.eTag.shouldNotBeNull()
                webTestClient.get().uri(path).header(HttpHeaders.IF_NONE_MATCH, etag).exchange()
                    .expectStatus().isEqualTo(HttpStatus.NOT_MODIFIED)
            }
        }
    }

    @Test
    fun pagesAreNotCached() {
        headersOf("/").cacheControl.orEmpty() shouldContain "no-store"
    }

    @Test
    fun dictionariesArePlainText() {
        listOf(editorFile("*.dic"), editorFile("*.aff")).forEach { path ->
            withClue(path) {
                headersOf(path).contentType?.toString().orEmpty() shouldStartWith "text/plain"
            }
        }
    }

    private fun headersOf(path: String, acceptEncoding: String? = null): HttpHeaders {
        val request = webTestClient.get().uri(path)
        acceptEncoding?.let { request.header(HttpHeaders.ACCEPT_ENCODING, it) }
        return request.exchange().expectStatus().isOk.returnResult(ByteArray::class.java).responseHeaders
    }

    /** Build file names change with every editor commit, so they are looked up in the built editor. */
    private fun editorFile(pattern: String): String {
        val files = PathMatchingResourcePatternResolver().getResources("classpath*:editor/assets/$pattern")
        files.toList().shouldNotBeEmpty()
        return "/assets/${files.first().filename}"
    }
}
