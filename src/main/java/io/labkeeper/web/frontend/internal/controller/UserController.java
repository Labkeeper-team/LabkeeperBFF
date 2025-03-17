package io.labkeeper.web.frontend.internal.controller;

import io.labkeeper.web.common.api.domain.UserWebPresentation;
import io.labkeeper.web.frontend.api.dto.UserInfoResponseDto;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import static io.labkeeper.web.frontend.internal.EndpointsList.USER_INFO_ENDPOINT;

@RestController
public class UserController {
    static final UserInfoResponseDto NO_USER = new UserInfoResponseDto(null, -1, false);

    @GetMapping(USER_INFO_ENDPOINT)
    public Mono<UserInfoResponseDto> getUserInfo() {
        return ReactiveSecurityContextHolder
                .getContext()
                .map(c -> c.getAuthentication().getPrincipal())
                .cast(UserWebPresentation.class)
                .map(user -> new UserInfoResponseDto(
                        user.email(),
                        user.id(),
                        true))
                .onErrorReturn(NO_USER);
    };
}
