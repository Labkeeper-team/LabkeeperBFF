package io.labkeeper.web.frontend.internal.project

/** Projects from BusinessLogic:Operation. The bean is missing when the frontend runs without that module. */
interface ProjectSource {
    suspend fun previewProjects(): List<LandingPreviewProject>

    suspend fun title(projectId: String): String
}

class LandingPreviewProject(
    val id: String,
    val nameRu: String,
    val nameEn: String,
    val category: String,
    val logoUrl: String?,
) {
    val categoryRu = when (category) {
        "lab" -> "Лабораторная работа"
        "diploma" -> "Дипломный проект"
        else -> category
    }
    val categoryEn = when (category) {
        "lab" -> "Laboratory work"
        "diploma" -> "Diploma project"
        else -> category
    }

    companion object {
        val FALLBACK = listOf(
            LandingPreviewProject(
                "092bacc5-c98a-4709-b67d-7455fd4588e4",
                "Измерение интенсивности радиационного фона",
                "Radiation background intensity measurement",
                "lab",
                "https://labkeeper.io/assets/img/case1.png",
            ),
            LandingPreviewProject(
                "1acb4436-d4d6-4f40-9fc2-92039adff180",
                "Статистическая обработка результатов многократных измерений",
                "Statistical processing of multiple measurement results",
                "lab",
                "https://labkeeper.io/assets/img/case2.png",
            ),
            LandingPreviewProject(
                "c4c71322-14da-4394-b546-2ed0ace1ebfa",
                "Определение систематических и случайных погрешностей",
                "Determination of systematic and random errors",
                "lab",
                "https://labkeeper.io/assets/img/case4.png",
            ),
            LandingPreviewProject(
                "d24a814f-ff8f-4b8c-8267-f824cf8c7b6b",
                "Изучение линейной временной модальной логики",
                "Study of linear temporal modal logic",
                "diploma",
                "https://labkeeper.io/assets/img/case3.png",
            ),
        )
    }
}
