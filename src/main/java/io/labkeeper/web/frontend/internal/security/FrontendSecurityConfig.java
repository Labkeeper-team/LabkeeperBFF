package io.labkeeper.web.frontend.internal.security;

import io.labkeeper.businesslogic.api.service.UserService;
import io.labkeeper.web.frontend.internal.EndpointsList;
import io.labkeeper.web.frontend.internal.mapper.UserMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.core.userdetails.ReactiveUserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.client.userinfo.DefaultReactiveOAuth2UserService;
import org.springframework.security.oauth2.jwt.ReactiveJwtDecoder;
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
    public SecurityWebFilterChain authSecurityWebFilterChain(ServerHttpSecurity http) {
        RedirectServerLogoutSuccessHandler logoutSuccessHandler = new RedirectServerLogoutSuccessHandler();
        logoutSuccessHandler.setLogoutSuccessUrl(URI.create(EndpointsList.HOME_PAGE));
        return http
                .csrf(ServerHttpSecurity.CsrfSpec::disable)
                .securityMatcher(new OrServerWebExchangeMatcher(List.of(
                        new PathPatternParserServerWebExchangeMatcher(LOGOUT_ENDPOINT),
                        new PathPatternParserServerWebExchangeMatcher(EndpointsList.SSO_LOGIN_ENDPOINT),
                        new PathPatternParserServerWebExchangeMatcher(EndpointsList.SSO_CODE_ENDPOINT),
                        new PathPatternParserServerWebExchangeMatcher(EndpointsList.USER_INFO_ENDPOINT)
                )))
                .oauth2Login(c -> c
                        .loginPage(EndpointsList.HOME_PAGE)
                )
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

    @Bean
    public ReactiveUserDetailsService userDetailsService(UserService userService,
                                                         UserMapper userMapper) {
        return new CustomReactiveUserDetailsService(userService, userMapper);
    }

    @Bean
    public DefaultReactiveOAuth2UserService oauth2UserService(
            ReactiveJwtDecoder jwtDecoder,
            UserService userService,
            UserMapper userMapper
    ) {
        return new CustomOAuth2UserService(jwtDecoder, userService, userMapper);
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
