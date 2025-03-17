package io.labkeeper.web.frontend.internal.security;

import io.labkeeper.businesslogic.api.service.UserService;
import io.labkeeper.web.frontend.internal.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.ReactiveUserDetailsService;
import org.springframework.security.core.userdetails.UserDetails;
import reactor.core.publisher.Mono;

@RequiredArgsConstructor
public class CustomReactiveUserDetailsService implements ReactiveUserDetailsService {
    final UserService userService;
    final UserMapper userMapper;

    @Override
    public Mono<UserDetails> findByUsername(String username) {
        return userService
                .getUserByEmail(username)
                .map(userMapper::toUserWebDto);
    }
}
