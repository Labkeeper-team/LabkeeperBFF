package io.labkeeper.web.frontend.internal.landing

import org.springframework.security.authentication.AnonymousAuthenticationToken
import org.springframework.security.core.context.ReactiveSecurityContextHolder
import reactor.core.publisher.Mono

data class LandingPreviewProject(
    val id: String,
    val nameRu: String,
    val nameEn: String,
    val category: String,
    val categoryRu: String,
    val categoryEn: String,
    val logoUrl: String?,
) {
    companion object {
        val fallback: List<LandingPreviewProject> = listOf(
            sample(
                "092bacc5-c98a-4709-b67d-7455fd4588e4",
                "Измерение интенсивности радиационного фона",
                "Radiation background intensity measurement",
                "lab",
                "https://labkeeper.io/assets/img/case1.png",
            ),
            sample(
                "1acb4436-d4d6-4f40-9fc2-92039adff180",
                "Статистическая обработка результатов многократных измерений",
                "Statistical processing of multiple measurement results",
                "lab",
                "https://labkeeper.io/assets/img/case2.png",
            ),
            sample(
                "c4c71322-14da-4394-b546-2ed0ace1ebfa",
                "Определение систематических и случайных погрешностей",
                "Determination of systematic and random errors",
                "lab",
                "https://labkeeper.io/assets/img/case4.png",
            ),
            sample(
                "d24a814f-ff8f-4b8c-8267-f824cf8c7b6b",
                "Изучение линейной временной модальной логики",
                "Study of linear temporal modal logic",
                "diploma",
                "https://labkeeper.io/assets/img/case3.png",
            ),
        )

        fun labels(category: String): Pair<String, String> = when (category) {
            "lab" -> "Лабораторная работа" to "Laboratory work"
            "diploma" -> "Дипломный проект" to "Diploma project"
            else -> category to category
        }

        private fun sample(
            id: String,
            nameRu: String,
            nameEn: String,
            category: String,
            logoUrl: String,
        ): LandingPreviewProject {
            val labels = labels(category)
            return LandingPreviewProject(
                id = id,
                nameRu = nameRu,
                nameEn = nameEn,
                category = category,
                categoryRu = labels.first,
                categoryEn = labels.second,
                logoUrl = logoUrl,
            )
        }
    }
}

interface LandingPageSource {
    fun viewer(): Mono<Boolean>

    fun previewProjects(): Mono<List<LandingPreviewProject>>

    companion object {
        fun authenticated(): Mono<Boolean> = ReactiveSecurityContextHolder.getContext()
            .filter { it.authentication != null }
            .map { context ->
                val authentication = context.authentication
                authentication.isAuthenticated && authentication !is AnonymousAuthenticationToken
            }
            .defaultIfEmpty(false)
    }
}
