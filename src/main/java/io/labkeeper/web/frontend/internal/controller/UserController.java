package io.labkeeper.web.frontend.internal.controller;

import io.labkeeper.businesslogic.api.service.UserService;
import io.labkeeper.web.common.api.domain.UserWebPresentation;
import io.labkeeper.web.common.api.service.IpExtractorService;
import io.labkeeper.web.frontend.api.dto.CodeCheckResponseDto;
import io.labkeeper.web.frontend.api.dto.UserInfoResponseDto;
import io.labkeeper.web.frontend.internal.service.captcha.yandex.YandexCaptchaService;
import jakarta.annotation.PostConstruct;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import java.util.Locale;

import static io.labkeeper.web.frontend.internal.EndpointsList.*;

@RestController
@RequiredArgsConstructor
public class UserController {
    UserInfoResponseDto noUser;

    @Value("${yandex.captcha.site.key}") String yandexCaptchaSiteKey;

    final UserService userService;
    final PasswordEncoder passwordEncoder;
    final YandexCaptchaService yandexCaptchaService;

    @PostConstruct
    public void init() {
        noUser = new UserInfoResponseDto(null, -1, false, yandexCaptchaSiteKey);
    }

    @GetMapping(USER_INFO_ENDPOINT)
    public Mono<UserInfoResponseDto> getUserInfo() {
        return ReactiveSecurityContextHolder
                .getContext()
                .map(c -> c.getAuthentication().getPrincipal())
                .cast(UserWebPresentation.class)
                .map(user -> new UserInfoResponseDto(
                        user.email(),
                        user.id(),
                        true, yandexCaptchaSiteKey))
                .onErrorReturn(noUser)
                .switchIfEmpty(Mono.just(noUser));
    }

    @PostMapping(EMAIL_ENDPOINT)
    public Mono<Void> sendEmail(@RequestParam("email") @Email String email,
                                @RequestParam("registration") boolean registration,
                                @RequestParam("captcha") String captchaToken,
                                Locale locale,
                                ServerHttpRequest serverHttpRequest) {
        var ip = IpExtractorService.extractIp(serverHttpRequest);
        if (ip == null) {
            return userService.sendPasswordSetupCodeToEmail(email, registration, locale);
        } else {
            return yandexCaptchaService
                    .checkCaptcha(captchaToken, ip)
                    .then(userService.sendPasswordSetupCodeToEmail(email, registration, locale));
        }
    }

    @PostMapping(CODE_ENDPOINT)
    public Mono<CodeCheckResponseDto> checkEmail(@RequestParam("email") @Email String email,
                                                 @RequestParam("code") @NotBlank String code) {
        return userService.checkValidCode(email, code).map(CodeCheckResponseDto::new);
    }

    @PostMapping(SET_PASSWORD_ENDPOINT)
    public Mono<Void> setPassword(@RequestParam("password") @NotBlank String password,
                                  @RequestParam("email") @Email String email,
                                  @RequestParam("code") @NotBlank String code,
                                  @RequestParam("registration") boolean registration) {
        return userService.setPassword(email, passwordEncoder.encode(password), code, registration);
    }
}
