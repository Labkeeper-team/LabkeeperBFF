package io.labkeeper.web.frontend.local

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

/** Landing pages without the backend: `gradlew :Web:Frontend:bootRun`. */
@SpringBootApplication
class LocalFrontendApplication

fun main(args: Array<String>) {
    @Suppress("SpreadOperator")
    runApplication<LocalFrontendApplication>(*args) { setAdditionalProfiles("frontend-local") }
}
