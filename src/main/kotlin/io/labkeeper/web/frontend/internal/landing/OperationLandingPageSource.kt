package io.labkeeper.web.frontend.internal.landing

import io.labkeeper.businesslogic.domain.api.model.PreviewProject
import io.labkeeper.businesslogic.operation.api.service.PreviewProjectService
import kotlinx.coroutines.reactor.mono
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass
import org.springframework.context.annotation.Primary
import org.springframework.stereotype.Component
import reactor.core.publisher.Mono

@Component
@Primary
@ConditionalOnClass(name = ["io.labkeeper.businesslogic.operation.api.service.PreviewProjectService"])
class OperationLandingPageSource(
    private val previewProjectService: PreviewProjectService,
) : LandingPageSource {
    override fun viewer(): Mono<Boolean> = LandingPageSource.authenticated()

    override fun previewProjects(): Mono<List<LandingPreviewProject>> = mono {
        previewProjectService.listPreviewProjects().map { it.toLanding() }
    }.onErrorReturn(LandingPreviewProject.fallback)

    private fun PreviewProject.toLanding(): LandingPreviewProject {
        val labels = LandingPreviewProject.labels(category)
        return LandingPreviewProject(
            id = projectId.toString(),
            nameRu = name.ru,
            nameEn = name.en,
            category = category,
            categoryRu = labels.first,
            categoryEn = labels.second,
            logoUrl = logoUrl,
        )
    }
}
