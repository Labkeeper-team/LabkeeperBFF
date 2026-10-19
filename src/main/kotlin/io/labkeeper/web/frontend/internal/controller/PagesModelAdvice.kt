package io.labkeeper.web.frontend.internal.controller

import io.labkeeper.web.frontend.internal.blog.BlogCatalog
import org.springframework.beans.factory.annotation.Value
import org.springframework.web.bind.annotation.ControllerAdvice
import org.springframework.web.bind.annotation.ModelAttribute

@ControllerAdvice
class PagesModelAdvice(
    private val blogCatalog: BlogCatalog,
    @Value("\${io.labkeeper.version.major:4}") private val majorVersion: Int,
    @Value("\${io.labkeeper.frontend.yandex-metrika-key:}") private val yandexMetrikaKey: String,
    @Value("\${io.labkeeper.frontend.openpanel-client-id:}") private val openpanelClientId: String,
    @Value("\${io.labkeeper.frontend.openpanel-api-url:}") private val openpanelApiUrl: String,
) {
    @ModelAttribute("articles")
    fun articles() = blogCatalog.articles

    @ModelAttribute("blogSliderJson")
    fun blogSliderJson(): String = blogCatalog.blogSliderJson

    @ModelAttribute("apiVersion")
    fun apiVersion(): String = "v$majorVersion"

    @ModelAttribute("yandexMetrikaKey")
    fun yandexMetrikaKey(): String = LandingPages.metrikaLiteral(yandexMetrikaKey)

    @ModelAttribute("openpanelClientId")
    fun openpanelClientId(): String = openpanelClientId

    @ModelAttribute("openpanelApiUrl")
    fun openpanelApiUrl(): String = openpanelApiUrl
}
