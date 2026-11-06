package io.labkeeper.web.frontend.internal.i18n

import io.kotest.assertions.withClue
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.maps.shouldNotBeEmpty
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldNotContain
import org.junit.jupiter.api.Test
import org.springframework.core.io.support.PathMatchingResourcePatternResolver

class SiteTextsTest {
    private val texts = SiteTexts()

    private val templates = PathMatchingResourcePatternResolver()
        .getResources("classpath*:templates/**/*.html")
        .associate { it.filename.orEmpty() to it.getContentAsString(Charsets.UTF_8) }

    @Test
    fun englishHasEveryKey() {
        val english = PathMatchingResourcePatternResolver().getResource("classpath:i18n/site_en.properties")
            .getContentAsString(Charsets.UTF_8)
        texts[SiteLanguage.RU].keys.filter { !english.contains(Regex("(?m)^${Regex.escape(it)}=")) }.shouldBeEmpty()
    }

    @Test
    fun templatesUseOnlyKnownKeys() {
        templates.shouldNotBeEmpty()
        templates.forEach { (name, html) ->
            withClue(name) {
                html shouldNotContain "data-i18n"
                KEY.findAll(html).map { it.groupValues[1] }.filter { it !in texts[SiteLanguage.RU] }.toList()
                    .shouldBeEmpty()
            }
        }
    }

    @Test
    fun everyKeyIsUsed() {
        val used = templates.values.flatMap { html -> KEY.findAll(html).map { it.groupValues[1] } }.toSet()
        texts[SiteLanguage.RU].keys.filter { it !in used }.shouldBeEmpty()
    }

    @Test
    fun textsKeepTheirMarkup() {
        texts[SiteLanguage.RU].getValue("hero-title") shouldBe
            "Создавайте документы <span class=\"text-blue\">вместе с&nbsp;ИИ</span>"
        texts[SiteLanguage.EN].getValue("nav-templates") shouldBe "Templates"
    }

    @Test
    fun languageComesFromTheCookieValue() {
        SiteLanguage.of("en") shouldBe SiteLanguage.EN
        SiteLanguage.of("ru") shouldBe SiteLanguage.RU
        SiteLanguage.of(null) shouldBe SiteLanguage.RU
        SiteLanguage.of("EN") shouldBe SiteLanguage.RU
    }

    private companion object {
        val KEY = Regex("""\$\{i18n\['([^']+)']}""")
    }
}
