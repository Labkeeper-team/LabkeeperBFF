package io.labkeeper.web.frontend.internal.i18n

import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldNotContain
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import org.springframework.boot.web.error.ErrorAttributeOptions
import org.springframework.http.HttpCookie
import org.springframework.http.HttpStatus
import org.springframework.mock.http.server.reactive.MockServerHttpRequest
import org.springframework.mock.web.server.MockServerWebExchange
import org.springframework.web.reactive.function.server.HandlerStrategies
import org.springframework.web.reactive.function.server.ServerRequest
import org.springframework.web.server.ResponseStatusException

class SiteErrorAttributesTest {
    private val errorAttributes = SiteErrorAttributes(SiteTexts())

    private fun attributes(accept: String?): Map<String, Any?> {
        val builder = MockServerHttpRequest.get("/blog/missing-article").cookie(HttpCookie(SiteLanguage.COOKIE, "zh"))
        accept?.let { builder.header("Accept", it) }
        val exchange = MockServerWebExchange.from(builder)
        errorAttributes.storeErrorInformation(ResponseStatusException(HttpStatus.NOT_FOUND), exchange)
        val request = ServerRequest.create(exchange, HandlerStrategies.withDefaults().messageReaders())
        return errorAttributes.getErrorAttributes(request, ErrorAttributeOptions.defaults())
    }

    @Test
    fun browsersGetTheLanguageAndTheTexts() {
        val page = attributes("text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
        page["lang"] shouldBe "zh"
        page.keys shouldContain "i18n"
    }

    @Test
    fun clientsThatAcceptAnythingGetAPlainError() {
        listOf("*/*", "application/json, text/plain, */*", "application/json", null).forEach { accept ->
            val error = attributes(accept)
            error.keys shouldNotContain "i18n"
            error.keys shouldNotContain "lang"
            error["status"] shouldBe 404
        }
    }

    @Test
    fun malformedAcceptHeaderGetsAPlainError() {
        val error = attributes("foo")
        error.keys shouldNotContain "i18n"
        error["status"] shouldBe 404
    }
}
