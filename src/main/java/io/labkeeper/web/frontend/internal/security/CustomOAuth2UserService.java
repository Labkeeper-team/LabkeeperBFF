package io.labkeeper.web.frontend.internal.security;

import io.labkeeper.businesslogic.api.service.UserService;
import io.labkeeper.web.frontend.internal.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.security.oauth2.client.userinfo.DefaultReactiveOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.oauth2.jwt.ReactiveJwtDecoder;
import reactor.core.publisher.Mono;

import static org.springframework.security.oauth2.core.OAuth2ErrorCodes.INVALID_TOKEN;

@RequiredArgsConstructor
public class CustomOAuth2UserService extends DefaultReactiveOAuth2UserService {
    final ReactiveJwtDecoder jwtDecoder;
    final UserService userService;
    final UserMapper userMapper;

    @Override
    public Mono<OAuth2User> loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        var registration = userRequest.getClientRegistration();
        String claim = null;
        if (registration != null) {
            var registrationId = registration.getRegistrationId();
            if (registrationId != null && registrationId.equals("sso")) {
                claim = "email";
            }
        }
        if (claim == null) {
            throw new OAuth2AuthenticationException(INVALID_TOKEN);
        }
        final String finalClaim = claim;
        return jwtDecoder
                .decode(userRequest.getAccessToken().getTokenValue())
                .flatMap(jwt -> {
                    var email = jwt.getClaims().get(finalClaim).toString();
                    return userService
                            .getOrCreateUserByEmail(email)
                            .map(userMapper::toUserWebDto);
                });
    }
}
