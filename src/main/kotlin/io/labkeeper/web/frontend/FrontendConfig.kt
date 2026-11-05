package io.labkeeper.web.frontend

import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.ComponentScan
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.PropertySource
import org.springframework.core.io.ClassPathResource
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.web.reactive.config.EnableWebFlux
import org.springframework.web.reactive.config.ViewResolverRegistry
import org.springframework.web.reactive.config.WebFluxConfigurer
import org.springframework.web.reactive.function.server.ServerResponse
import org.springframework.web.reactive.function.server.router
import org.thymeleaf.spring6.view.reactive.ThymeleafReactiveViewResolver
import java.net.URI

@EnableWebFlux
@Configuration
@ComponentScan
@PropertySource(value = ["classpath:frontend.properties"], encoding = "UTF-8")
@EnableConfigurationProperties(FrontendProperties::class)
class FrontendConfig(
    @Qualifier("thymeleafViewResolver")
    private val thymeleafViewResolver: ThymeleafReactiveViewResolver,
    @Qualifier("sitemapViewResolver")
    private val sitemapViewResolver: ThymeleafReactiveViewResolver,
) : WebFluxConfigurer {
    // @EnableWebFlux switches off WebFluxAutoConfiguration, which would otherwise register the resolver.
    override fun configureViewResolvers(registry: ViewResolverRegistry) {
        registry.viewResolver(sitemapViewResolver)
        registry.viewResolver(thymeleafViewResolver)
    }

    @Bean
    fun staticResourceRouter(
        properties: FrontendProperties,
        @Value($$"${io.labkeeper.host}") host: String,
    ) = router {
        GET("/robots.txt") {
            val body = if (properties.disallowIndexing) {
                DISALLOW_ALL_ROBOTS
            } else {
                val disallows = SERVICE_PATHS.joinToString("") { "Disallow: $it\n" }
                "User-agent: *\n$disallows\nSitemap: $host/sitemap.xml\n"
            }
            ok().contentType(MediaType.TEXT_PLAIN).bodyValue(body)
        }
        // editor/index.html has unfilled placeholders, EditorPagesController serves the filled one.
        GET("/index.html") { notFound().build() }
        GET("/wiki") { wikiIndex() }
        GET("/wiki/") { wikiIndex() }
        resources("/wiki/**", ClassPathResource("wiki/"))
        resources("/**", ClassPathResource("static/"))
        resources("/**", ClassPathResource("editor/"))
    }
}

/**
 * Addresses without content for search engines: editor pages, the OAuth callback, the QR redirect and the API.
 * `$` and `?` pin a single page, so a future `/payment-info` or `/projects-guide` stays open to crawlers.
 * `/project/` (shared projects) stays open on purpose: those pages carry `noindex` themselves,
 * and a Disallow would keep crawlers from seeing it.
 */
private val SERVICE_PATHS = listOf(
    "/api/",
    "/projects\$",
    "/projects?",
    "/projects/",
    "/tokens\$",
    "/tokens?",
    "/pay\$",
    "/pay?",
    "/oauth2/",
    "/qr/",
)

private const val DISALLOW_ALL_ROBOTS = "User-agent: *\nDisallow: /\n"

private fun wikiIndex() = ServerResponse.status(HttpStatus.FOUND).location(URI.create("/wiki/index.html")).build()
