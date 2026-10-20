package io.labkeeper.web.frontend

import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.ComponentScan
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.PropertySource
import org.springframework.core.io.ClassPathResource
import org.springframework.core.io.Resource
import org.springframework.http.MediaType
import org.springframework.web.reactive.config.EnableWebFlux
import org.springframework.web.reactive.config.ViewResolverRegistry
import org.springframework.web.reactive.config.WebFluxConfigurer
import org.springframework.web.reactive.function.server.RouterFunctions
import org.springframework.web.reactive.function.server.ServerRequest
import org.springframework.web.reactive.function.server.router
import org.thymeleaf.spring6.view.reactive.ThymeleafReactiveViewResolver
import reactor.core.publisher.Mono

@EnableWebFlux
@Configuration
@ComponentScan
@PropertySource("classpath:frontend.properties")
@EnableConfigurationProperties(FrontendProperties::class)
class FrontendConfig(
    private val thymeleafViewResolver: ThymeleafReactiveViewResolver,
) : WebFluxConfigurer {
    // @EnableWebFlux switches off WebFluxAutoConfiguration, which would otherwise register the resolver.
    override fun configureViewResolvers(registry: ViewResolverRegistry) {
        registry.viewResolver(thymeleafViewResolver)
    }

    @Bean
    fun staticResourceRouter(properties: FrontendProperties) = router {
        if (properties.disallowIndexing) {
            GET("/robots.txt") { ok().contentType(MediaType.TEXT_PLAIN).bodyValue(DISALLOW_ALL_ROBOTS) }
        }
        // editor/index.html has unfilled placeholders, EditorPagesController serves the filled one.
        GET("/index.html") { notFound().build() }
        resources("/**", ClassPathResource("static/"))
        resources("/**", ClassPathResource("editor/"))
        resources(::wikiResource)
    }
}

private const val WIKI_PREFIX = "/wiki/"
private const val DISALLOW_ALL_ROBOTS = "User-agent: *\nDisallow: /\n"
private val WIKI_ROOT = ClassPathResource("wiki/")
private val WIKI_FILES = RouterFunctions.resourceLookupFunction("/wiki/**", WIKI_ROOT)

/** Wiki pages are also reachable without the `.html` extension. */
private fun wikiResource(request: ServerRequest): Mono<Resource> {
    val path = request.path()
    return when {
        !path.startsWith(WIKI_PREFIX) -> Mono.empty()
        path == WIKI_PREFIX -> wikiPage("index")
        else -> WIKI_FILES.apply(request).switchIfEmpty(Mono.defer { wikiPage(path.removePrefix(WIKI_PREFIX)) })
    }
}

private fun wikiPage(name: String): Mono<Resource> =
    if ('.' in name || name.endsWith('/')) {
        Mono.empty()
    } else {
        Mono.just(WIKI_ROOT.createRelative("$name.html")).filter(Resource::isReadable)
    }
