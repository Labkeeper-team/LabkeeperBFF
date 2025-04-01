package io.labkeeper.web.frontend.security;

import io.labkeeper.businesslogic.api.domain.LabkeeperUser;
import io.labkeeper.businesslogic.api.service.UserService;
import io.labkeeper.web.common.api.EndpointsList;
import io.labkeeper.web.common.internal.service.yandex.YandexCaptchaService;
import io.labkeeper.web.frontend.FrontendConfig;
import io.labkeeper.web.frontend.internal.security.FrontendSecurityConfig;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.security.oauth2.client.reactive.ReactiveOAuth2ClientAutoConfiguration;
import org.springframework.boot.autoconfigure.security.reactive.ReactiveSecurityAutoConfiguration;
import org.springframework.boot.autoconfigure.web.reactive.HttpHandlerAutoConfiguration;
import org.springframework.boot.autoconfigure.web.reactive.ReactiveWebServerFactoryAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.web.reactive.function.BodyInserters;
import reactor.core.publisher.Mono;

import java.net.URI;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@WebFluxTest
@ContextConfiguration(classes = {
        FrontendConfig.class,
        FrontendSecurityConfig.class,
        ReactiveWebServerFactoryAutoConfiguration.class,
        ReactiveSecurityAutoConfiguration.class,
        ReactiveOAuth2ClientAutoConfiguration.class,
        HttpHandlerAutoConfiguration.class
})
public class FormLoginTests {
    @MockBean
    UserService userService;
    @MockBean
    YandexCaptchaService yandexCaptchaService;
    @Autowired
    WebTestClient webClient;
    @Autowired
    PasswordEncoder passwordEncoder;

    @Test
    public void captchaOkTest() {
        doReturn(Mono.just(new LabkeeperUser(
                "g@gmail.com",
                1,
                passwordEncoder.encode("g")
        ))).when(userService).getUserByEmail(eq("g@gmail.com"));
        doReturn(Mono.just(true)).when(yandexCaptchaService)
                .checkCaptcha(any());
        URI location = webClient
                .post()
                .uri(EndpointsList.FORM_LOGIN_ENDPOINT)
                .body(BodyInserters
                        .fromFormData("username", "g@gmail.com")
                        .with("password", "g")
                        .with("captcha", "biba"))
                .exchange()
                .expectStatus()
                .is3xxRedirection()
                .returnResult(String.class)
                .getResponseHeaders()
                .getLocation();

        verify(yandexCaptchaService).checkCaptcha(any());
        Assertions.assertEquals(URI.create("/"), location);
    }

    @Test
    public void captchaFailedTest() {
        doReturn(Mono.just(new LabkeeperUser(
                "g@gmail.com",
                1,
                passwordEncoder.encode("g")
        ))).when(userService).getUserByEmail(eq("g@gmail.com"));
        doReturn(Mono.just(false)).when(yandexCaptchaService)
                .checkCaptcha(any());
        webClient
                .post()
                .uri(EndpointsList.FORM_LOGIN_ENDPOINT)
                .body(BodyInserters
                        .fromFormData("username", "g@gmail.com")
                        .with("password", "g"))
                .exchange()
                .expectStatus()
                .is4xxClientError();

        verify(yandexCaptchaService).checkCaptcha(any());
        verify(userService, never()).getUserByEmail(eq("g@gmail.com"));
    }
}
