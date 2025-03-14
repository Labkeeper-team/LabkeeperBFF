package io.labkeeper.web.frontend.internal.config;

import io.labkeeper.web.frontend.internal.EndpointsList;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.security.web.server.authentication.RedirectServerAuthenticationEntryPoint;
import org.springframework.security.web.server.authentication.logout.RedirectServerLogoutSuccessHandler;
import org.springframework.security.web.server.util.matcher.OrServerWebExchangeMatcher;
import org.springframework.security.web.server.util.matcher.PathPatternParserServerWebExchangeMatcher;

import java.net.URI;
import java.util.List;

import static io.labkeeper.web.frontend.internal.EndpointsList.LOGOUT_ENDPOINT;

@Configuration
public class FrontendSecurityConfig {
    @Bean
    @Order(2)
    public SecurityWebFilterChain authSecurityWebFilterChain(ServerHttpSecurity http) {
        RedirectServerLogoutSuccessHandler logoutSuccessHandler = new RedirectServerLogoutSuccessHandler();
        logoutSuccessHandler.setLogoutSuccessUrl(URI.create(EndpointsList.HOME_PAGE));
        return http
                .csrf(ServerHttpSecurity.CsrfSpec::disable)
                .securityMatcher(new OrServerWebExchangeMatcher(List.of(
                        new PathPatternParserServerWebExchangeMatcher(LOGOUT_ENDPOINT),
                        new PathPatternParserServerWebExchangeMatcher(EndpointsList.LOGIN_ENDPOINT),
                        new PathPatternParserServerWebExchangeMatcher(EndpointsList.CODE_ENDPOINT),
                        new PathPatternParserServerWebExchangeMatcher(EndpointsList.USER_INFO_ENDPOINT_V1)
                )))
                .authorizeExchange(exchange ->
                        exchange
                                .pathMatchers(EndpointsList.PROJECTS_PAGE).authenticated()
                                .anyExchange().permitAll())
                .logout(logoutSpec -> logoutSpec
                        .logoutUrl(LOGOUT_ENDPOINT)
                        .logoutSuccessHandler(logoutSuccessHandler)
                )
                .build();
    }

    @Bean
    @Order(3)
    public SecurityWebFilterChain pagesSecurityWebFilterChain(ServerHttpSecurity http) {
        return http
                .exceptionHandling(c -> c
                        .authenticationEntryPoint(new RedirectServerAuthenticationEntryPoint(EndpointsList.HOME_PAGE)))
                .securityMatcher(new OrServerWebExchangeMatcher(List.of(
                        new PathPatternParserServerWebExchangeMatcher(EndpointsList.HOME_PAGE),
                        new PathPatternParserServerWebExchangeMatcher(EndpointsList.PROJECTS_PAGE),
                        new PathPatternParserServerWebExchangeMatcher(EndpointsList.DEFAULT_PROJECT_PAGE),
                        new PathPatternParserServerWebExchangeMatcher(EndpointsList.PROJECT_PAGE)
                )))
                .authorizeExchange(exchange -> exchange
                        .pathMatchers(EndpointsList.PROJECTS_PAGE).authenticated()
                        .anyExchange().permitAll()
                )
                .build();
    }
}
