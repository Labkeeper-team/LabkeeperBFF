package io.labkeeper.web.frontend.internal.controller

internal object LandingPages {
    fun metrikaLiteral(key: String?): String = if (key.isNullOrBlank()) "undefined" else key
}
