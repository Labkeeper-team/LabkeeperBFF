package io.labkeeper.web.frontend.internal.service.captcha.yandex;

import lombok.RequiredArgsConstructor;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

@RequiredArgsConstructor
public class YandexCaptchaServiceImpl implements YandexCaptchaService {
    private static final String BASE_YANDEX_URL = "https://smartcaptcha.yandexcloud.net";

    final WebClient webClient = WebClient.builder().baseUrl(BASE_YANDEX_URL).build();

    final String captchaSecret;

    public Mono<Void> checkCaptcha(String token, String ip) throws CaptchaCheckerException {
        if (captchaSecret == null || captchaSecret.isEmpty()) {
            return Mono.empty();
        }
        return webClient
                .get()
                .uri(builder -> builder
                        .pathSegment("validate")
                        .queryParam("token", token)
                        .queryParam("secret", captchaSecret)
                        .queryParam("ip", ip)
                        .build())
                .exchangeToMono(clientResponse -> clientResponse.bodyToMono(YandexCaptchaResponseDto.class))
                .filter(body -> body.status().equals("ok"))
                .onErrorMap(CaptchaCheckerException::new)
                .then();
    }
}
