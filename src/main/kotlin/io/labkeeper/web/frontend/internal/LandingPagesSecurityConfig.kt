package io.labkeeper.web.frontend.internal

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.config.web.server.ServerHttpSecurity
import org.springframework.security.web.server.SecurityWebFilterChain
import org.springframework.security.web.server.util.matcher.ServerWebExchangeMatchers.pathMatchers

/** Public pages still go through a chain: it loads the session, so pages can tell whether the visitor is logged in. */
@Configuration
class LandingPagesSecurityConfig {
    @Bean
    fun landingPagesSecurityWebFilterChain(http: ServerHttpSecurity): SecurityWebFilterChain = http
        .securityMatcher(
            pathMatchers(
                "/",
                "/about",
                "/blog",
                "/blog/**",
                "/templates",
                "/templates/**",
                "/privacy",
                "/soglas",
                "/sogl_yam",
                "/sogl_adv",
                "/sogl_ds",
                "/oferta",
                "/assets/**",
            ),
        )
        .csrf { it.disable() }
        .authorizeExchange { it.anyExchange().permitAll() }
        .build()
}
