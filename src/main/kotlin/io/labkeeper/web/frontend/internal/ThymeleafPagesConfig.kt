package io.labkeeper.web.frontend.internal

import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.web.reactive.config.ViewResolverRegistry
import org.springframework.web.reactive.config.WebFluxConfigurer
import org.thymeleaf.spring6.view.reactive.ThymeleafReactiveViewResolver
import org.thymeleaf.templatemode.TemplateMode
import org.thymeleaf.templateresolver.FileTemplateResolver
import org.thymeleaf.templateresolver.ITemplateResolver
import java.nio.charset.StandardCharsets

@Configuration
class ThymeleafPagesConfig {
    @Bean
    fun sourceTemplateResolver(
        resourceLocations: ResourceLocations,
        @Value("\${spring.thymeleaf.cache:true}") cacheTemplates: Boolean,
    ): ITemplateResolver {
        val resolver = FileTemplateResolver()
        val templates = resourceLocations.templatesDirectory()
        resolver.order = -1
        resolver.checkExistence = true
        resolver.templateMode = TemplateMode.HTML
        resolver.characterEncoding = StandardCharsets.UTF_8.name()
        resolver.suffix = ".html"
        if (templates == null) {
            resolver.prefix = "missing-pages-templates/"
            resolver.isCacheable = true
            return resolver
        }
        val prefix = templates.toAbsolutePath().toString().replace('\\', '/')
        resolver.prefix = if (prefix.endsWith("/")) prefix else "$prefix/"
        resolver.isCacheable = cacheTemplates
        return resolver
    }

    @Bean
    fun thymeleafViewConfigurer(viewResolver: ThymeleafReactiveViewResolver): WebFluxConfigurer {
        return object : WebFluxConfigurer {
            override fun configureViewResolvers(registry: ViewResolverRegistry) {
                registry.viewResolver(viewResolver)
            }
        }
    }
}
