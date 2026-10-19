package io.labkeeper.web.frontend.local

import org.springframework.boot.SpringApplication
import org.springframework.boot.autoconfigure.SpringBootApplication

@SpringBootApplication(scanBasePackages = ["io.labkeeper.web.frontend.local"])
class LocalFrontendApplication

fun main(args: Array<String>) {
    val application = SpringApplication(LocalFrontendApplication::class.java)
    application.setAdditionalProfiles("frontend-local")
    @Suppress("SpreadOperator")
    application.run(*args)
}
