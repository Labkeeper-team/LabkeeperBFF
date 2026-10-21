package io.labkeeper.web.frontend

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "io.labkeeper.frontend")
class FrontendProperties {
    var yandexCaptchaSiteKey: String = ""
    var sentryDsn: String = ""
    var yandexMetrikaKey: String = ""
    var openpanelClientId: String = ""
    var openpanelApiUrl: String = ""
    var contactEmail: String = ""
    var contactPhone: String = ""
    var defaultTitle: String = ""
    var defaultDescription: String = ""
    var disallowIndexing: Boolean = false
}
