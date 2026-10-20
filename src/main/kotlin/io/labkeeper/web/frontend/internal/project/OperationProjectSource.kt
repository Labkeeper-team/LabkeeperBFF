package io.labkeeper.web.frontend.internal.project

import io.labkeeper.businesslogic.operation.api.service.PreviewProjectService
import io.labkeeper.businesslogic.operation.api.service.ProjectService
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass
import org.springframework.stereotype.Component
import java.util.UUID

@Component
@ConditionalOnClass(ProjectService::class)
class OperationProjectSource(
    private val projectService: ProjectService,
    private val previewProjectService: PreviewProjectService,
) : ProjectSource {
    override suspend fun previewProjects() = previewProjectService.listPreviewProjects().map {
        LandingPreviewProject(it.projectId.toString(), it.name.ru, it.name.en, it.category, it.logoUrl)
    }

    override suspend fun title(projectId: String) =
        projectService.getProjectUnauthorizedWithProgramResult(UUID.fromString(projectId)).title
}
