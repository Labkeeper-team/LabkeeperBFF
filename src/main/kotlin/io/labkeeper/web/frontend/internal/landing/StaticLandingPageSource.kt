package io.labkeeper.web.frontend.internal.landing

import org.springframework.stereotype.Component
import reactor.core.publisher.Mono

@Component
class StaticLandingPageSource : LandingPageSource {
    override fun viewer(): Mono<Boolean> = LandingPageSource.authenticated()

    override fun previewProjects(): Mono<List<LandingPreviewProject>> = Mono.just(LandingPreviewProject.fallback)
}
