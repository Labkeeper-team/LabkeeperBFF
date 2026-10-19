package io.labkeeper.web.frontend.internal

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.Ordered
import org.springframework.core.annotation.Order
import org.springframework.security.config.web.server.ServerHttpSecurity
import org.springframework.security.web.server.SecurityWebFilterChain
import org.springframework.security.web.server.util.matcher.ServerWebExchangeMatchers.pathMatchers

@Configuration
class LandingPagesSecurityConfig {
    @Bean
    @Order(Ordered.LOWEST_PRECEDENCE)
    fun landingPagesSecurityWebFilterChain(http: ServerHttpSecurity): SecurityWebFilterChain {
        return http
            .securityMatcher(
                pathMatchers(
                    "/",
                    "/about",
                    "/blog",
                    "/blog/**",
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
            .httpBasic { it.disable() }
            .formLogin { it.disable() }
            .authorizeExchange { exchanges -> exchanges.anyExchange().permitAll() }
            .build()
    }
}
