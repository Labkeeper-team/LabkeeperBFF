package io.labkeeper.web.frontend.local

import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.cloud.gateway.route.RouteLocator
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder
import org.springframework.context.annotation.Bean
import java.net.URI

/**
 * Landing pages without the backend: `gradlew :Web:Frontend:bootRun`.
 * Spring Cloud Gateway forwards every other request to `io.labkeeper.frontend.proxy-host`.
 */
@SpringBootApplication
class LocalFrontendApplication {
    @Bean
    fun frontendProxyRoutes(
        builder: RouteLocatorBuilder,
        @Value($$"${io.labkeeper.frontend.proxy-host:https://labkeeper.io}") proxyHost: String,
    ): RouteLocator {
        val routes = builder.routes()
        val host = proxyHost.trim().trimEnd('/')
        if (host.isNotEmpty()) {
            routes.route("frontend-proxy") { it.path("/**").uri(origin(host)) }
        }
        return routes.build()
    }
}

fun main(args: Array<String>) {
    @Suppress("SpreadOperator")
    runApplication<LocalFrontendApplication>(*args) { setAdditionalProfiles("frontend-local") }
}

private fun origin(host: String): String {
    val uri = URI(host)
    val scheme = uri.scheme?.lowercase()
    require(scheme == "http" || scheme == "https") {
        "io.labkeeper.frontend.proxy-host must start with http:// or https://"
    }
    require(!uri.host.isNullOrBlank()) { "io.labkeeper.frontend.proxy-host must include a host" }
    require(uri.rawPath.isNullOrEmpty() && uri.rawQuery == null && uri.rawFragment == null) {
        "io.labkeeper.frontend.proxy-host must be an origin without a path"
    }
    return host
}
