package io.labkeeper.web.frontend.internal

import org.springframework.boot.autoconfigure.thymeleaf.ThymeleafProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.thymeleaf.spring6.templateresolver.SpringResourceTemplateResolver
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
            setCheckExistence(true)
            setResolvablePatterns(setOf("sitemap"))
        }
}
