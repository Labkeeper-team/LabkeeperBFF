package io.labkeeper.web.frontend;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.config.EnableWebFlux;
import org.springframework.web.reactive.function.server.RequestPredicates;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.RouterFunctions;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;

@EnableWebFlux
@Configuration
@ComponentScan
@PropertySource("classpath:frontend.properties")
public class FrontendConfig {
    private static final String WIKI_PREFIX = "/wiki/";
    private static final ClassPathResource WIKI_ROOT = new ClassPathResource("wiki/");
    private static final String DISALLOW_ALL_ROBOTS = "User-agent: *\nDisallow: /\n";

    @Bean
    RouterFunction<ServerResponse> staticResourceRouter(
            @Value("${io.labkeeper.frontend.disallow-indexing:false}") boolean disallowIndexing
    ) {
        RouterFunction<ServerResponse> staticResources = RouterFunctions.resources(RouterFunctions
                .resourceLookupFunction("/**", new ClassPathResource("pages/"))
        ).and(RouterFunctions.resources(RouterFunctions
                .resourceLookupFunction("/**", new ClassPathResource("editor/"))
        )).and(RouterFunctions.resources(FrontendConfig::wikiResource));
        if (!disallowIndexing) {
            return staticResources;
        }
        return RouterFunctions.route(
                RequestPredicates.GET("/robots.txt"),
                request -> ServerResponse.ok()
                        .contentType(MediaType.TEXT_PLAIN)
                        .bodyValue(DISALLOW_ALL_ROBOTS)
        ).and(staticResources);
    }

    private static Mono<Resource> wikiResource(ServerRequest request) {
        String path = request.path();
        if ("/wiki".equals(path)) {
            return Mono.empty();
        }
        if (WIKI_PREFIX.equals(path)) {
            return readableWikiResource("index.html");
        }
        return RouterFunctions.resourceLookupFunction("/wiki/**", WIKI_ROOT)
                .apply(request)
                .switchIfEmpty(Mono.defer(() -> wikiHtmlFallback(path)));
    }

    private static Mono<Resource> wikiHtmlFallback(String path) {
        if (!path.startsWith(WIKI_PREFIX) || path.contains("..")) {
            return Mono.empty();
        }
        String relative = path.substring(WIKI_PREFIX.length());
        if (relative.isEmpty() || relative.contains(".") || relative.endsWith("/")) {
            return Mono.empty();
        }
        return readableWikiResource(relative + ".html");
    }

    private static Mono<Resource> readableWikiResource(String relative) {
        Resource resource = WIKI_ROOT.createRelative(relative);
        if (resource.exists() && resource.isReadable()) {
            return Mono.just(resource);
        }
        return Mono.empty();
    }
}
