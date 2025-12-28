package org.cis_india.wsreader.helpers.book

import androidx.annotation.Keep
import androidx.annotation.StringRes
import org.cis_india.wsreader.R

@Keep
sealed class BookLanguage(@StringRes val name: Int, val isoCode: String) {

    companion object {
        fun getAllLanguages() =
            BookLanguage::class.sealedSubclasses.mapNotNull { it.objectInstance }
    }

    @Keep
    data object AllBooks : BookLanguage(R.string.language_name_all_books, "all")

    @Keep
    data object Arabic : BookLanguage(R.string.language_name_arabic, "ar")

    @Keep
    data object Assamese : BookLanguage(R.string.language_name_assamese, "as")

    @Keep
    data object Bangla : BookLanguage(R.string.language_name_bangla, "bn")

    @Keep
    data object Catalan : BookLanguage(R.string.language_name_catalan, "ca")
    
    @Keep
    data object Czech : BookLanguage(R.string.language_name_czech, "cs")

    @Keep
    data object Danish : BookLanguage(R.string.language_name_danish, "da")
    
    @Keep
    data object English : BookLanguage(R.string.language_name_english, "en")

    @Keep
    data object Spanish : BookLanguage(R.string.language_name_spanish, "es")

    @Keep
    data object French : BookLanguage(R.string.language_name_french, "fr")

    @Keep
    data object Gujarati : BookLanguage(R.string.language_name_gujarati, "gu")

    @Keep
    data object Hindi : BookLanguage(R.string.language_name_hindi, "hi")

    @Keep
    data object Indonesian : BookLanguage(R.string.language_name_indonesian, "id")

    @Keep
    data object Italian : BookLanguage(R.string.language_name_italian, "it")

    @Keep
    data object Javanese : BookLanguage(R.string.language_name_javanese, "jv")

    @Keep
    data object Kannada : BookLanguage(R.string.language_name_kannada, "kn")

    @Keep
    data object Malayalam : BookLanguage(R.string.language_name_malayalam, "ml")

    @Keep
    data object Marathi : BookLanguage(R.string.language_name_marathi, "mr")

    @Keep
    data object Malay : BookLanguage(R.string.language_name_malay, "ms")

    @Keep
    data object Polish : BookLanguage(R.string.language_name_polish, "pl")

    @Keep
    data object Punjabi : BookLanguage(R.string.language_name_punjabi, "pa")

    @Keep
    data object Sundanese : BookLanguage(R.string.language_name_sundanese, "su")
    
    @Keep
    data object Swedish : BookLanguage(R.string.language_name_swedish, "sv")

    @Keep
    data object Tamil : BookLanguage(R.string.language_name_tamil, "ta")

    @Keep
    data object Telugu : BookLanguage(R.string.language_name_telugu, "te")

    @Keep
    data object Tulu : BookLanguage(R.string.language_name_tulu, "tcy")

    @Keep
    data object Ukrainian : BookLanguage(R.string.language_name_ukrainian, "uk")

    @Keep
    data object Vietnamese : BookLanguage(R.string.language_name_vietnamese, "vi")

}
