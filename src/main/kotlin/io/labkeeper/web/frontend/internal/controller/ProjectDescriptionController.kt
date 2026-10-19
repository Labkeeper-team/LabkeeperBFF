package io.labkeeper.web.frontend.internal.controller

import io.labkeeper.businesslogic.operation.api.service.ProjectService
import kotlinx.coroutines.reactor.mono
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RestController
import reactor.core.publisher.Mono
import java.util.UUID

@RestController
@ConditionalOnClass(name = ["io.labkeeper.businesslogic.operation.api.service.ProjectService"])
class ProjectDescriptionController(
    private val projectService: ProjectService,
    private val editorIndexPage: EditorIndexPage,
) {
    @GetMapping("/project/{id}", "/project/{id}/agent")
    fun projectPage(@PathVariable("id") id: String): Mono<String> {
        return editorIndexPage.content().flatMap { content ->
            mono {
                val project = projectService.getProjectUnauthorizedWithProgramResult(UUID.fromString(id))
                content
                    .replace(HTML_TITLE_TO_REPLACE, project.title)
                    .replace(HTML_DESCRIPTION_TO_REPLACE, "")
            }.onErrorReturn(content)
        }
    }

    private companion object {
        private const val HTML_TITLE_TO_REPLACE = "Labkeeper — online pdf editor with a built-in scientific calculator"
        private const val HTML_DESCRIPTION_TO_REPLACE =
            "Compose documents, formulas, and plots, convert to pdf. Editor and calculations in one place"
    }
}
