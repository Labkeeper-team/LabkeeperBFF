package io.labkeeper.web.frontend.internal.security;

import io.labkeeper.businesslogic.api.service.UserService;
import io.labkeeper.web.common.api.EndpointsList;
import io.labkeeper.web.common.api.filter.YandexCaptchaSecurityFilter;
import io.labkeeper.web.common.internal.service.yandex.YandexCaptchaService;
import io.labkeeper.web.frontend.internal.mapper.UserMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.config.web.server.SecurityWebFiltersOrder;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.core.userdetails.ReactiveUserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.client.userinfo.DefaultReactiveOAuth2UserService;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.web.server.DefaultServerRedirectStrategy;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.security.web.server.ServerRedirectStrategy;
import org.springframework.security.web.server.authentication.RedirectServerAuthenticationEntryPoint;
import org.springframework.security.web.server.authentication.ServerAuthenticationFailureHandler;
import org.springframework.security.web.server.authentication.logout.RedirectServerLogoutSuccessHandler;
import org.springframework.security.web.server.util.matcher.OrServerWebExchangeMatcher;
import org.springframework.security.web.server.util.matcher.PathPatternParserServerWebExchangeMatcher;

import java.net.URI;
import java.util.List;

import static io.labkeeper.web.common.api.EndpointsList.FORM_LOGIN_ENDPOINT;
import static io.labkeeper.web.common.api.EndpointsList.HOME_PAGE;
import static io.labkeeper.web.common.api.EndpointsList.LOGOUT_ENDPOINT;
import static io.labkeeper.web.common.api.EndpointsList.OAUTH_CODE_ENDPOINT;
import static io.labkeeper.web.common.api.EndpointsList.OAUTH_LOGIN_ENDPOINT;
import static io.labkeeper.web.common.api.EndpointsList.PROJECTS_PAGE;
import static org.springframework.security.web.server.util.matcher.ServerWebExchangeMatchers.pathMatchers;

@Configuration
public class FrontendSecurityConfig {
    @Bean
    public SecurityWebFilterChain authSecurityWebFilterChain(ServerHttpSecurity http,
                                                             YandexCaptchaService yandexCaptchaService) {
        RedirectServerLogoutSuccessHandler logoutSuccessHandler = new RedirectServerLogoutSuccessHandler();
        logoutSuccessHandler.setLogoutSuccessUrl(URI.create(HOME_PAGE));
        return http
                .csrf(ServerHttpSecurity.CsrfSpec::disable)
                .securityMatcher(pathMatchers(
                        LOGOUT_ENDPOINT,
                        OAUTH_LOGIN_ENDPOINT,
                        OAUTH_CODE_ENDPOINT,
                        FORM_LOGIN_ENDPOINT
                ))
                .oauth2Login(c -> c
                        .loginPage(HOME_PAGE)
                        .authenticationFailureHandler(authenticationFailureHandler())
                )
                .authorizeExchange(exchange ->
                        exchange
                                .pathMatchers(PROJECTS_PAGE).authenticated()
                                .anyExchange().permitAll())
                .logout(logoutSpec -> logoutSpec
                        .logoutUrl(LOGOUT_ENDPOINT)
                        .logoutSuccessHandler(logoutSuccessHandler)
                )
                .addFilterAfter(new YandexCaptchaSecurityFilter(yandexCaptchaService), SecurityWebFiltersOrder.FIRST)
                .formLogin(form -> form
                        .loginPage(FORM_LOGIN_ENDPOINT)
                        .authenticationFailureHandler(authenticationFailureHandler()))
                .build();
    }

    @Bean
    public ServerAuthenticationFailureHandler authenticationFailureHandler() {
        ServerRedirectStrategy redirectStrategy = new DefaultServerRedirectStrategy();
        var badCredentials = URI.create(HOME_PAGE + "?error=bad_credentials");
        var oauthError = URI.create(HOME_PAGE + "?error=oauth_error");
        var unknownError = URI.create(HOME_PAGE + "?error=unknown_error");
        return (webFilterExchange, exception) -> {
            if (exception instanceof BadCredentialsException) {
                return redirectStrategy.sendRedirect(webFilterExchange.getExchange(), badCredentials);
            }
            if (exception instanceof OAuth2AuthenticationException) {
                return redirectStrategy.sendRedirect(webFilterExchange.getExchange(), oauthError);
            }
            return redirectStrategy.sendRedirect(webFilterExchange.getExchange(), unknownError);
        };
    }

    @Bean
    public SecurityWebFilterChain pagesSecurityWebFilterChain(ServerHttpSecurity http) {
        return http
                .exceptionHandling(c -> c
                        .authenticationEntryPoint(new RedirectServerAuthenticationEntryPoint(HOME_PAGE)))
                .securityMatcher(new OrServerWebExchangeMatcher(List.of(
                        new PathPatternParserServerWebExchangeMatcher(HOME_PAGE),
                        new PathPatternParserServerWebExchangeMatcher(PROJECTS_PAGE),
                        new PathPatternParserServerWebExchangeMatcher(EndpointsList.DEFAULT_PROJECT_PAGE),
                        new PathPatternParserServerWebExchangeMatcher(EndpointsList.PROJECT_PAGE)
                )))
                .authorizeExchange(exchange -> exchange
                        .pathMatchers(PROJECTS_PAGE).authenticated()
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
    public DefaultReactiveOAuth2UserService oauth2UserService(UserService userService, UserMapper userMapper) {
        return new CustomOAuth2UserService(userService, userMapper);
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
