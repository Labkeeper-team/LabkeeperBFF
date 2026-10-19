package io.labkeeper.web.frontend.internal.controller

import org.springframework.beans.factory.annotation.Value
import org.springframework.core.io.ClassPathResource
import org.springframework.stereotype.Component
import reactor.core.publisher.Mono
import java.io.IOException
import java.nio.charset.StandardCharsets

@Component
class EditorIndexPage(
    @Value($$"${io.labkeeper.frontend.sentry-dsn}") private val sentryDsn: String,
    @Value($$"${io.labkeeper.frontend.yandex-metrika-key}") private val yandexMetrikaKey: String,
    @Value($$"${io.labkeeper.frontend.yandex-captcha-site-key}") private val yandexCaptchaSiteKey: String,
    @Value($$"${io.labkeeper.frontend.openpanel-client-id}") private val openpanelClientId: String,
    @Value($$"${io.labkeeper.frontend.openpanel-api-url}") private val openpanelApiUrl: String,
) {
    private val html: Mono<String> = Mono
        .just(ClassPathResource("editor/index.html"))
        .handle<String> { resource, sink ->
            try {
                sink.next(resource.getContentAsString(StandardCharsets.UTF_8))
            } catch (exception: IOException) {
                sink.error(IllegalStateException(exception))
            }
        }
        .map { source ->
            source
                .replace(SENTRY_DSN_PARAM, sentryDsn)
                .replace(YANDEX_METRIKA_PARAM, yandexMetrikaKey)
                .replace(YANDEX_CAPTCHA_SITE_KEY, yandexCaptchaSiteKey)
                .replace(OPENPANEL_CLIENT_ID_PARAM, openpanelClientId)
                .replace(OPENPANEL_API_URL_PARAM, openpanelApiUrl)
        }
        .cache()

    fun content(): Mono<String> = html

    private companion object {
        private const val SENTRY_DSN_PARAM = "IO_LABKEEPER_FRONTEND_SENTRY_DSN"
        private const val YANDEX_METRIKA_PARAM = "IO_LABKEEPER_FRONTEND_YANDEX_METRIKA_KEY"
        private const val YANDEX_CAPTCHA_SITE_KEY = "IO_LABKEEPER_FRONTEND_YANDEX_CAPTCHA_SITE_KEY"
        private const val OPENPANEL_CLIENT_ID_PARAM = "IO_LABKEEPER_FRONTEND_OPENPANEL_CLIENT_ID"
        private const val OPENPANEL_API_URL_PARAM = "IO_LABKEEPER_FRONTEND_OPENPANEL_API_URL"
    }
}
