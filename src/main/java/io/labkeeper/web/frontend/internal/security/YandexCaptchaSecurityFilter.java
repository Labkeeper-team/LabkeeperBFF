package io.labkeeper.web.frontend.internal.security;

import io.labkeeper.web.common.api.service.IpExtractorService;
import io.labkeeper.web.frontend.internal.EndpointsList;
import io.labkeeper.web.frontend.internal.service.captcha.yandex.YandexCaptchaService;
import lombok.RequiredArgsConstructor;
import org.apache.logging.log4j.util.Strings;
import org.springframework.security.web.server.DefaultServerRedirectStrategy;
import org.springframework.security.web.server.ServerRedirectStrategy;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

import java.net.URI;

@RequiredArgsConstructor
public class YandexCaptchaSecurityFilter implements WebFilter {
    final YandexCaptchaService yandexCaptchaService;
    final ServerRedirectStrategy redirectStrategy = new DefaultServerRedirectStrategy();
    final URI redirectUri = URI.create(EndpointsList.HOME_PAGE + "?error=captcha");

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        if (exchange.getRequest().getPath().toString().equals(EndpointsList.FORM_LOGIN_ENDPOINT)) {
            var ip = IpExtractorService.extractIp(exchange.getRequest());
            if (ip == null) {
                return chain.filter(exchange);
            }
            return exchange
                    .getFormData()
                    .mapNotNull(data -> data.getFirst("captcha"))
                    .switchIfEmpty(Mono.just(Strings.EMPTY))
                    .flatMap(token -> yandexCaptchaService.checkCaptcha(token, ip))
                    .then(chain.filter(exchange))
                    .onErrorResume(ex -> redirectStrategy.sendRedirect(exchange, redirectUri));
        }
        return chain.filter(exchange);
    }
}
