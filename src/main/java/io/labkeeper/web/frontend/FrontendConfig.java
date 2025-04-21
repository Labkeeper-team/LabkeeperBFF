package io.labkeeper.web.frontend;

import io.labkeeper.web.frontend.internal.mapper.UserMapper;
import io.labkeeper.web.frontend.internal.mapper.UserMapperImpl;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import org.springframework.core.io.ClassPathResource;
import org.springframework.web.reactive.config.EnableWebFlux;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.RouterFunctions;
import org.springframework.web.reactive.function.server.ServerResponse;

@EnableWebFlux
@Configuration
@ComponentScan
@PropertySource("classpath:frontend.properties")
public class FrontendConfig {
    @Bean
    RouterFunction<ServerResponse> staticResourceRouter() {
        return RouterFunctions.resources(RouterFunctions
                .resourceLookupFunction("/**", new ClassPathResource("frontend/")));
    }

    @Bean
    public UserMapper userMapper() {
        return new UserMapperImpl();
    }
}
