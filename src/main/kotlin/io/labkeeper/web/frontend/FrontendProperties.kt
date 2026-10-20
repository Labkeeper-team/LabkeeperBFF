package io.labkeeper.web.frontend

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "io.labkeeper.frontend")
class FrontendProperties(
    val yandexCaptchaSiteKey: String,
    val sentryDsn: String,
    val yandexMetrikaKey: String,
    val openpanelClientId: String,
    val openpanelApiUrl: String,
    /** Origin of the public site, without a trailing slash. */
    val publicUrl: String,
    val contactEmail: String,
    val contactPhone: String,
    val defaultTitle: String,
    val defaultDescription: String,
    val disallowIndexing: Boolean,
) {
    val siteUrl: String = publicUrl.trim().trimEnd('/')
    val siteHost: String = siteUrl.substringAfter("://").substringBefore('/')
}
