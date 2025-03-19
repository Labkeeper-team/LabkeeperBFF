package io.labkeeper.web.frontend.internal.service.captcha.yandex;

import lombok.NoArgsConstructor;

@NoArgsConstructor
public class CaptchaCheckerException extends RuntimeException {
    public CaptchaCheckerException(Throwable throwable) {
        super(throwable);
    }
}
