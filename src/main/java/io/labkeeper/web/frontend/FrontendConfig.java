package io.labkeeper.web.frontend;

import io.labkeeper.web.frontend.internal.service.captcha.yandex.YandexCaptchaService;
import io.labkeeper.web.frontend.internal.service.captcha.yandex.YandexCaptchaServiceImpl;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import org.springframework.core.io.ClassPathResource;
import org.springframework.web.reactive.config.EnableWebFlux;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.RouterFunctions;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;

@EnableWebFlux
@Configuration
@ComponentScan
@PropertySource("classpath:frontend.properties")
public class FrontendConfig {
    @Bean
    RouterFunction<ServerResponse> staticResourceRouter() {
        return RouterFunctions.resources(RouterFunctions
                .resourceLookupFunction("/**", new ClassPathResource("frontend/")));
    }

    @Bean
    @ConditionalOnExpression("!T(org.springframework.util.StringUtils).isEmpty('${yandex.captcha.secret:}')")
    public YandexCaptchaService yandexCaptchaService(
            @Value("${yandex.captcha.secret}") String captchaSecret
    ) {
        return new YandexCaptchaServiceImpl(captchaSecret);
    }

    @Bean
    @ConditionalOnMissingBean
    public YandexCaptchaService yandexCaptchaServiceStub() {
        return (a, b) -> Mono.empty();
    }
}
