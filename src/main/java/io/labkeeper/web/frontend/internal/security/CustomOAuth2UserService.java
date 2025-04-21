package io.labkeeper.web.frontend.internal.security;

import io.labkeeper.businesslogic.api.service.UserService;
import io.labkeeper.web.frontend.internal.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.security.oauth2.client.userinfo.DefaultReactiveOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.OAuth2User;
import reactor.core.publisher.Mono;

import static org.springframework.security.oauth2.core.OAuth2ErrorCodes.INVALID_TOKEN;

@RequiredArgsConstructor
public class CustomOAuth2UserService extends DefaultReactiveOAuth2UserService {
    static final String YANDEX_EMAIL_ATTRIBUTE_NAME = "default_email";

    final UserService userService;
    final UserMapper userMapper;

    @Override
    public Mono<OAuth2User> loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        return super
                .loadUser(userRequest)
                .mapNotNull(oAuth2User -> oAuth2User.getAttribute(YANDEX_EMAIL_ATTRIBUTE_NAME))
                .cast(String.class)
                .switchIfEmpty(Mono.error(new OAuth2AuthenticationException(INVALID_TOKEN)))
                .flatMap(userService::getOrCreateUserByEmail)
                .map(userMapper::toUserWebDto);
    }
}
