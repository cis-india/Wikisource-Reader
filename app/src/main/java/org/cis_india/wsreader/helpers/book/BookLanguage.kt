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
    data object AllBooks : BookLanguage(R.string.all_books_language_lebel, "all")

    @Keep
    data object Arabic : BookLanguage(R.string.arabic_language_label, "ar")

    @Keep
    data object Assamese : BookLanguage(R.string.assamese_language_label, "as")

    @Keep
    data object Bangla : BookLanguage(R.string.bangla_language_label, "bn")

    @Keep
    data object Catalan : BookLanguage(R.string.catalan_language_label, "ca")
    
    @Keep
    data object Czech : BookLanguage(R.string.czech_language_label, "cs")

    @Keep
    data object Danish : BookLanguage(R.string.danish_language_lebel, "da")
    
    @Keep
    data object English : BookLanguage(R.string.english_language_label, "en")

    @Keep
    data object Spanish : BookLanguage(R.string.spanish_language_label, "es")

    @Keep
    data object French : BookLanguage(R.string.french_language_label, "fr")

    @Keep
    data object Gujarati : BookLanguage(R.string.gujarati_language_label, "gu")

    @Keep
    data object Hindi : BookLanguage(R.string.hindi_language_label, "hi")

    @Keep
    data object Indonesian : BookLanguage(R.string.indonesian_language_label, "id")

    @Keep
    data object Italian : BookLanguage(R.string.italian_language_label, "it")

    @Keep
    data object Javanese : BookLanguage(R.string.javanese_language_label, "jv")

    @Keep
    data object Kannada : BookLanguage(R.string.kannada_language_label, "kn")

    @Keep
    data object Malayalam : BookLanguage(R.string.malayalam_language_label, "ml")

    @Keep
    data object Marathi : BookLanguage(R.string.marathi_language_label, "mr")

    @Keep
    data object Malay : BookLanguage(R.string.malay_language_label, "ms")

    @Keep
    data object Polish : BookLanguage(R.string.polish_language_label, "pl")

    @Keep
    data object Punjabi : BookLanguage(R.string.punjabi_language_label, "pa")

    @Keep
    data object Sundanese : BookLanguage(R.string.sundanese_language_label, "su")
    
    @Keep
    data object Swedish : BookLanguage(R.string.swedish_language_label, "sv")

    @Keep
    data object Tamil : BookLanguage(R.string.tamil_language_label, "ta")

    @Keep
    data object Telugu : BookLanguage(R.string.telugu_language_label, "te")

    @Keep
    data object Tulu : BookLanguage(R.string.tulu_language_label, "tcy")

    @Keep
    data object Ukrainian : BookLanguage(R.string.ukrainian_language_label, "uk")

    @Keep
    data object Vietnamese : BookLanguage(R.string.vietnamese_language_label, "vi")

}
