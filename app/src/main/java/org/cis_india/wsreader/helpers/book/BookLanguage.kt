package org.cis_india.wsreader.helpers.book

import androidx.annotation.Keep

@Keep
sealed class BookLanguage(val name: String, val isoCode: String) {

    companion object {
        fun getAllLanguages() =
            BookLanguage::class.sealedSubclasses.mapNotNull { it.objectInstance }
    }

    @Keep
    data object AllBooks : BookLanguage("All Books", "all")

    @Keep
    data object Assamese : BookLanguage("Assamese", "as")

    @Keep
    data object Bangla : BookLanguage("Bangla", "bn")

    @Keep
    data object Catalan : BookLanguage("Catalan", "ca")
    
    @Keep
    data object Czech : BookLanguage("Czech", "cs")

    @Keep
    data object Danish : BookLanguage("Danish", "da")
    
    @Keep
    data object English : BookLanguage("English", "en")

    @Keep
    data object Spanish : BookLanguage("Spanish", "es")

    @Keep
    data object French : BookLanguage("French", "fr")

    @Keep
    data object Gujarati : BookLanguage("Gujarati", "gu")

    @Keep
    data object Hindi : BookLanguage("Hindi", "hi")

    @Keep
    data object Indonesian : BookLanguage("Indonesian", "id")

    @Keep
    data object Italian : BookLanguage("Italian", "it")

    @Keep
    data object Javanese : BookLanguage("Javanese", "jv")

    @Keep
    data object Kannada : BookLanguage("Kannada", "kn")

    @Keep
    data object Malayalam : BookLanguage("Malayalam", "ml")

    @Keep
    data object Marathi : BookLanguage("Marathi", "mr")

    @Keep
    data object Malay : BookLanguage("Malay", "ms")

    @Keep
    data object Polish : BookLanguage("Polish", "pl")

    @Keep
    data object Punjabi : BookLanguage("Punjabi", "pa")

    @Keep
    data object Sundanese : BookLanguage("Sundanese", "su")
    
    @Keep
    data object Swedish : BookLanguage("Swedish", "sv")

    @Keep
    data object Tamil : BookLanguage("Tamil", "ta")

    @Keep
    data object Telugu : BookLanguage("Telugu", "te")

    @Keep
    data object Tulu : BookLanguage("Tulu", "tcy")

    @Keep
    data object Ukrainian : BookLanguage("Ukrainian", "uk")

    @Keep
    data object Vietnamese : BookLanguage("Vietnamese", "vi")

}
