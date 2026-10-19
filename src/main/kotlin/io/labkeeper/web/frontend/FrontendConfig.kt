package io.labkeeper.web.frontend

import io.labkeeper.web.frontend.internal.ResourceLocations
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.ComponentScan
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.PropertySource
import org.springframework.core.io.ClassPathResource
import org.springframework.core.io.Resource
import org.springframework.http.MediaType
import org.springframework.web.reactive.config.EnableWebFlux
import org.springframework.web.reactive.function.server.RequestPredicates
import org.springframework.web.reactive.function.server.RouterFunction
import org.springframework.web.reactive.function.server.RouterFunctions
import org.springframework.web.reactive.function.server.ServerRequest
import org.springframework.web.reactive.function.server.ServerResponse
import reactor.core.publisher.Mono

@EnableWebFlux
@Configuration
@ComponentScan
@PropertySource("classpath:frontend.properties")
class FrontendConfig {
    @Bean
    fun staticResourceRouter(
        resourceLocations: ResourceLocations,
        @Value("\${io.labkeeper.frontend.disallow-indexing:false}") disallowIndexing: Boolean,
    ): RouterFunction<ServerResponse> {
        val staticResources = RouterFunctions.route(RequestPredicates.GET("/index.html")) {
            ServerResponse.notFound().build()
        }.and(RouterFunctions.resources(resourceLocations::findStatic))
            .and(RouterFunctions.resources(RouterFunctions.resourceLookupFunction("/**", ClassPathResource("editor/"))))
            .and(RouterFunctions.resources(FrontendConfig::wikiResource))
        if (!disallowIndexing) {
            return staticResources
        }
        val robots = RequestPredicates.GET("/robots.txt")
        return RouterFunctions.route(robots) {
            ServerResponse.ok().contentType(MediaType.TEXT_PLAIN).bodyValue(DISALLOW_ALL_ROBOTS)
        }.and(staticResources)
    }

    companion object {
        private const val WIKI_PREFIX = "/wiki/"
        private val WIKI_ROOT = ClassPathResource("wiki/")
        private const val DISALLOW_ALL_ROBOTS = "User-agent: *\nDisallow: /\n"

        private fun wikiResource(request: ServerRequest): Mono<Resource> {
            val path = request.path()
            if (path == "/wiki") {
                return Mono.empty()
            }
            if (path == WIKI_PREFIX) {
                return readableWikiResource("index.html")
            }
            return RouterFunctions.resourceLookupFunction("/wiki/**", WIKI_ROOT)
                .apply(request)
                .switchIfEmpty(Mono.defer { wikiHtmlFallback(path) })
        }

        private fun wikiHtmlFallback(path: String): Mono<Resource> {
            if (!path.startsWith(WIKI_PREFIX) || path.contains("..")) {
                return Mono.empty()
            }
            val relative = path.substring(WIKI_PREFIX.length)
            if (relative.isEmpty() || relative.contains(".") || relative.endsWith("/")) {
                return Mono.empty()
            }
            return readableWikiResource("$relative.html")
        }

        private fun readableWikiResource(relative: String): Mono<Resource> {
            val resource = try {
                WIKI_ROOT.createRelative(relative)
            } catch (_: Exception) {
                return Mono.empty()
            }
            return if (resource.exists() && resource.isReadable) Mono.just(resource) else Mono.empty()
        }
    }
}
