package com.junkfood.seal.util

import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.core.os.LocaleListCompat
import com.junkfood.seal.R
import java.util.Locale

// Do not modify
private const val SIMPLIFIED_CHINESE = 1
private const val ENGLISH = 2
private const val CZECH = 3
private const val FRENCH = 4
private const val GERMAN = 5
private const val NORWEGIAN_BOKMAL = 6
private const val DANISH = 7
private const val SPANISH = 8
private const val TURKISH = 9
private const val UKRAINIAN = 10
private const val RUSSIAN = 11
private const val ARABIC = 12
private const val PERSIAN = 13
private const val INDONESIAN = 14
private const val FILIPINO = 15
private const val ITALIAN = 16
private const val DUTCH = 17
private const val PORTUGUESE_BRAZIL = 18
private const val JAPANESE = 19
private const val POLISH = 20
private const val HUNGARIAN = 21
private const val MALAY = 22
private const val TRADITIONAL_CHINESE = 23
private const val VIETNAMESE = 24
private const val BELARUSIAN = 25
private const val CROATIAN = 26
private const val BASQUE = 27
private const val HINDI = 28
private const val MALAYALAM = 29
private const val SINHALA = 30
private const val SERBIAN = 31
private const val AZERBAIJANI = 32
private const val NORWEGIAN_NYNORSK = 33
private const val PUNJABI = 34
private const val TAMIL = 35
private const val KOREAN = 36
private const val SWEDISH = 37
private const val PORTUGUESE_PORTUGAL = 38
private const val CATALAN = 39
private const val HEBREW = 40
private const val PORTUGUESE = 41
private const val THAI = 42
private const val BENGALI = 43
private const val KHMER = 44
private const val KANNADA = 45
private const val GREEK = 46
private const val MONGOLIAN = 47

val LocaleLanguageCodeMap =
    mapOf(
        Locale.forLanguageTag("ar") to ARABIC,
        Locale.forLanguageTag("az") to AZERBAIJANI,
        Locale.forLanguageTag("eu") to BASQUE,
        Locale.forLanguageTag("be") to BELARUSIAN,
        Locale.forLanguageTag("bn") to BENGALI,
        Locale.forLanguageTag("ca") to CATALAN,
        Locale.forLanguageTag("zh-Hans") to SIMPLIFIED_CHINESE,
        Locale.forLanguageTag("zh-Hant") to TRADITIONAL_CHINESE,
        Locale.forLanguageTag("hr") to CROATIAN,
        Locale.forLanguageTag("cs") to CZECH,
        Locale.forLanguageTag("da") to DANISH,
        Locale.forLanguageTag("nl") to DUTCH,
        Locale.forLanguageTag("en-US") to ENGLISH,
        Locale.forLanguageTag("fil") to FILIPINO,
        Locale.forLanguageTag("fr") to FRENCH,
        Locale.forLanguageTag("de") to GERMAN,
        Locale.forLanguageTag("el") to GREEK,
        Locale.forLanguageTag("he") to HEBREW,
        Locale.forLanguageTag("hi") to HINDI,
        Locale.forLanguageTag("hu") to HUNGARIAN,
        Locale.forLanguageTag("id") to INDONESIAN,
        Locale.forLanguageTag("it") to ITALIAN,
        Locale.forLanguageTag("ja") to JAPANESE,
        Locale.forLanguageTag("kn") to KANNADA,
        Locale.forLanguageTag("km") to KHMER,
        Locale.forLanguageTag("ko") to KOREAN,
        Locale.forLanguageTag("ms") to MALAY,
        Locale.forLanguageTag("ml") to MALAYALAM,
        Locale.forLanguageTag("mn") to MONGOLIAN,
        Locale.forLanguageTag("nb") to NORWEGIAN_BOKMAL,
        Locale.forLanguageTag("nn") to NORWEGIAN_NYNORSK,
        Locale.forLanguageTag("fa") to PERSIAN,
        Locale.forLanguageTag("pl") to POLISH,
        Locale.forLanguageTag("pt") to PORTUGUESE,
        Locale.forLanguageTag("pt-PT") to PORTUGUESE_PORTUGAL,
        Locale.forLanguageTag("pt-BR") to PORTUGUESE_BRAZIL,
        Locale.forLanguageTag("pa") to PUNJABI,
        Locale.forLanguageTag("ru") to RUSSIAN,
        Locale.forLanguageTag("sr") to SERBIAN,
        Locale.forLanguageTag("si") to SINHALA,
        Locale.forLanguageTag("es") to SPANISH,
        Locale.forLanguageTag("sv") to SWEDISH,
        Locale.forLanguageTag("ta") to TAMIL,
        Locale.forLanguageTag("th") to THAI,
        Locale.forLanguageTag("tr") to TURKISH,
        Locale.forLanguageTag("uk") to UKRAINIAN,
        Locale.forLanguageTag("vi") to VIETNAMESE,
    )

@Composable
fun Locale?.toDisplayName(): String =
    this?.getDisplayName(this) ?: stringResource(id = R.string.follow_system)

fun setLanguage(locale: Locale?) {
    val localeList =
        locale?.let { LocaleListCompat.create(it) } ?: LocaleListCompat.getEmptyLocaleList()
    AppCompatDelegate.setApplicationLocales(localeList)
}
