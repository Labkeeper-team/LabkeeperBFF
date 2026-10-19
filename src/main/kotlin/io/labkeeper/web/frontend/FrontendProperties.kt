package io.labkeeper.web.frontend

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.stereotype.Component

@Component
@ConfigurationProperties(prefix = "io.labkeeper.frontend")
class FrontendProperties {
    var yandexCaptchaSiteKey: String = ""
    var sentryDsn: String = ""
    var yandexMetrikaKey: String = ""
    var openpanelClientId: String = ""
    var openpanelApiUrl: String = ""
    var disallowIndexing: Boolean = false

    /** `off` uses only the classpath. Empty or `auto` prefers `src/main/resources` when it is present. */
    var staticDirectory: String = ""
}
