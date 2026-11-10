package io.labkeeper.web.frontend.internal.i18n

import org.springframework.core.io.ClassPathResource
import org.springframework.stereotype.Component
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale
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

    /** Date as people of this language write it: «5 марта 2026», «5 March 2026», «2026年3月5日». */
    fun formatDate(date: LocalDate): String = when (this) {
        ZH -> "${date.year}年${date.monthValue}月${date.dayOfMonth}日"
        else -> date.format(DateTimeFormatter.ofPattern("d MMMM yyyy", Locale.forLanguageTag(code)))
    }

    companion object {
        const val COOKIE = "labkeeper_lang"

        fun of(code: String?): SiteLanguage = entries.firstOrNull { it.code == code } ?: RU

        /** The same date in every site language, keyed by language code: templates pick it with `[lang]`. */
        fun formatDates(date: LocalDate): Map<String, String> = entries.associate { it.code to it.formatDate(date) }
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
