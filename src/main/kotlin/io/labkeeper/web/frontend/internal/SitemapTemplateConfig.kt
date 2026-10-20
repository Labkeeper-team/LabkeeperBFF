package io.labkeeper.web.frontend.internal

import org.springframework.boot.autoconfigure.thymeleaf.ThymeleafProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.MediaType
import org.thymeleaf.spring6.ISpringWebFluxTemplateEngine
import org.thymeleaf.spring6.templateresolver.SpringResourceTemplateResolver
import org.thymeleaf.spring6.view.reactive.ThymeleafReactiveViewResolver
import org.thymeleaf.templatemode.TemplateMode
import org.thymeleaf.templateresolver.ITemplateResolver
import java.nio.charset.StandardCharsets

@Configuration
class SitemapTemplateConfig {
    // HTML pages use the .html resolver. This one only claims the sitemap template.
    @Bean
    fun sitemapTemplateResolver(properties: ThymeleafProperties): ITemplateResolver =
        SpringResourceTemplateResolver().apply {
            prefix = "classpath:/templates/"
            suffix = ".xml"
            templateMode = TemplateMode.XML
            characterEncoding = StandardCharsets.UTF_8.name()
            isCacheable = properties.isCache
            order = -1
            checkExistence = true
            setResolvablePatterns(setOf("sitemap"))
        }

    /** HTML views stay text/html. The sitemap view is the one XML response. */
    @Bean
    fun sitemapViewResolver(templateEngine: ISpringWebFluxTemplateEngine) = ThymeleafReactiveViewResolver().apply {
        this.templateEngine = templateEngine
        supportedMediaTypes = listOf(MediaType.APPLICATION_XML)
        viewNames = arrayOf("sitemap")
    }
}
