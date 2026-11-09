package io.labkeeper.web.frontend.internal.i18n

import org.springframework.core.io.ClassPathResource
import org.springframework.stereotype.Component
import java.util.Properties

/**
 * Language of the site pages. The page comes in the language of the [COOKIE] cookie, Russian without it, so search
 * engines see the Russian text in the HTML. The editor shares the choice through localStorage under the same name,
 * and lang.js copies it into the cookie.
 */
enum class SiteLanguage(val code: String) {
    RU("ru"),
    EN("en"),
    ZH("zh"),
    ;

    companion object {
        const val COOKIE = "labkeeper_lang"

        fun of(code: String?): SiteLanguage = entries.firstOrNull { it.code == code } ?: RU
    }
}

/** Texts of the site pages from i18n/site*.properties. A key missing in a language falls back to Russian. */
@Component
class SiteTexts {
    private val texts: Map<SiteLanguage, Map<String, String>> = load("i18n/site.properties").let { russian ->
        mapOf(
            SiteLanguage.RU to russian,
            SiteLanguage.EN to russian + load("i18n/site_en.properties"),
            SiteLanguage.ZH to russian + load("i18n/site_zh.properties"),
        )
    }

    operator fun get(language: SiteLanguage): Map<String, String> = texts.getValue(language)

    private fun load(path: String): Map<String, String> {
        val properties = Properties()
        ClassPathResource(path).inputStream.reader(Charsets.UTF_8).use(properties::load)
        return properties.stringPropertyNames().associateWith(properties::getProperty)
    }
}
