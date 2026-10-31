package io.labkeeper.web.frontend

import java.util.UUID

/** One public project from `io.labkeeper.frontend.preview-projects`. */
class PreviewProjectProperties {
    lateinit var id: UUID
    var nameRu: String = ""
    var nameEn: String = ""
    var category: String = ""
    var projectType: String = ""
    var logoUrl: String? = null

    /** Returned by the preview API and used to filter templates. */
    var template: Boolean = false

    /** Compiled during JVM warmup. Not part of the public API. */
    var useInWarmup: Boolean = false

    val categoryRu
        get() = when (category) {
            "lab" -> "Лабораторная работа"
            "diploma" -> "Дипломный проект"
            "cv" -> "Резюме"
            else -> category
        }

    val categoryEn
        get() = when (category) {
            "lab" -> "Laboratory work"
            "diploma" -> "Diploma project"
            "cv" -> "CV"
            else -> category
        }
}
