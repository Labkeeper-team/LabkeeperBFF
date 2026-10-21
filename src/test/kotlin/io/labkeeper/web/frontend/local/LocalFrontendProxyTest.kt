package io.labkeeper.web.frontend.local

import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.netty.handler.codec.http.HttpHeaderNames
import io.netty.handler.codec.http.HttpMethod
import io.netty.handler.codec.http.HttpResponseStatus
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.springframework.test.web.reactive.server.WebTestClient
import org.springframework.test.web.reactive.server.expectBody
import org.springframework.web.reactive.socket.client.ReactorNettyWebSocketClient
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import reactor.netty.DisposableServer
import reactor.netty.http.server.HttpServer
import java.net.URI
import java.nio.charset.StandardCharsets
import java.time.Duration
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.atomic.AtomicInteger

@ActiveProfiles("frontend-local")
@SpringBootTest(
    classes = [LocalFrontendApplication::class],
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
)
class LocalFrontendProxyTest(
    @Autowired private val webTestClient: WebTestClient,
    @Value($$"${local.server.port}") private val port: Int,
) {
    @BeforeEach
    fun clearCaptures() {
        captured.clear()
    }

    @Test
    fun proxiesUnmatchedRequests() {
        webTestClient.post()
            .uri("/api/v4/items?q=1")
            .header(HttpHeaders.AUTHORIZATION, "Bearer t")
            .contentType(MediaType.TEXT_PLAIN)
            .bodyValue("hello")
            .exchange()
            .expectStatus().isCreated
            .expectHeader().valueEquals(HttpHeaders.LOCATION, "http://127.0.0.1:${backendPort.get()}/after")
            .expectBody<String>().isEqualTo("ok:hello")

        val request = captured.single()
        request.method shouldBe "POST"
        request.uri shouldBe "/api/v4/items?q=1"
        request.body shouldBe "hello"
        request.authorization shouldBe "Bearer t"
    }

    @Test
    fun localPagesStayLocal() {
        webTestClient.get().uri("/").exchange()
            .expectStatus().isOk
            .expectHeader().contentTypeCompatibleWith(MediaType.TEXT_HTML)
            .expectBody<String>().returnResult().responseBody.orEmpty() shouldContain "<!DOCTYPE html>"
        webTestClient.get().uri("/blog/missing-article").exchange().expectStatus().isNotFound
        webTestClient.get().uri("/index.html").exchange().expectStatus().isNotFound
        webTestClient.get().uri("/assets/css/style.css").exchange()
            .expectStatus().isOk
            .expectBody<String>().returnResult().responseBody.orEmpty() shouldContain "--c-primary"
        captured.shouldBeEmpty()
    }

    @Test
    fun proxiesWebSocketText() {
        val received = Mono.create { sink ->
            ReactorNettyWebSocketClient().execute(URI.create("ws://localhost:$port/ws/echo")) { session ->
                val messages = session.receive().take(2).map { it.payloadAsText }.collectList()
                val send = Mono.delay(Duration.ofMillis(200))
                    .then(session.send(Mono.just(session.textMessage("ping"))))
                messages.zipWith(send.thenReturn(true)) { list, _ -> list }
                    .doOnNext(sink::success)
                    .then()
            }.subscribe({}, sink::error)
        }.block(Duration.ofSeconds(10))

        received shouldBe listOf("ready", "ping")
    }

    private data class Captured(
        val method: String,
        val uri: String,
        val body: String,
        val authorization: String?,
    )

    private companion object {
        val captured = CopyOnWriteArrayList<Captured>()
        val backendPort = AtomicInteger()
        val backend: DisposableServer = startBackend()

        @JvmStatic
        @AfterAll
        fun stopBackend() {
            backend.disposeNow()
        }

        @DynamicPropertySource
        @JvmStatic
        fun proxyHost(registry: DynamicPropertyRegistry) {
            registry.add("io.labkeeper.frontend.proxy-host") { "http://127.0.0.1:${backend.port()}" }
        }

        private fun startBackend(): DisposableServer = HttpServer.create()
            .host("127.0.0.1")
            .port(0)
            .route { routes ->
                routes.ws("/ws/echo") { inbound, outbound ->
                    outbound.sendString(
                        Flux.concat(Mono.just("ready"), inbound.receive().asString(StandardCharsets.UTF_8)),
                        StandardCharsets.UTF_8,
                    ).then()
                }
                routes.route({ true }) { request, response ->
                    request.receive().aggregate().asString(StandardCharsets.UTF_8).defaultIfEmpty("").flatMap { body ->
                        captured.add(
                            Captured(
                                method = request.method().name(),
                                uri = request.uri(),
                                body = body,
                                authorization = request.requestHeaders().get(HttpHeaderNames.AUTHORIZATION),
                            ),
                        )
                        val status = if (request.method() == HttpMethod.POST) {
                            HttpResponseStatus.CREATED
                        } else {
                            HttpResponseStatus.OK
                        }
                        response.status(status)
                            .header(HttpHeaderNames.LOCATION, "http://127.0.0.1:${backendPort.get()}/after")
                            .header(HttpHeaderNames.CONTENT_TYPE, "text/plain;charset=UTF-8")
                            .sendString(Mono.just("ok:$body"))
                            .then()
                    }
                }
            }
            .bindNow()
            .also { backendPort.set(it.port()) }
    }
}
