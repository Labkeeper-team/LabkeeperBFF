package io.labkeeper.web.frontend.internal.service.captcha.yandex;

import reactor.core.publisher.Mono;

public interface YandexCaptchaService {
    Mono<Void> checkCaptcha(String token, String ip) throws CaptchaCheckerException;
}
