package io.labkeeper.web.frontend.internal.service.captcha.yandex;

public record YandexCaptchaResponseDto(
        String status,
        String host,
        String message
) {
}
