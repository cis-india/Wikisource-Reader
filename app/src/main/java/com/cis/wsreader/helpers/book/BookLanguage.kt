package com.cis.wsreader.helpers.book

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
    data object English : BookLanguage("English", "en")

    @Keep
    data object Bangla : BookLanguage("Bangla", "bn")

    @Keep
    data object French : BookLanguage("French", "fr")

    @Keep
    data object German : BookLanguage("German", "de")

    @Keep
    data object Greek : BookLanguage("Greek", "el")

    @Keep
    data object Italian : BookLanguage("Italian", "it")

    @Keep
    data object Russian : BookLanguage("Russian", "ru")

    @Keep
    data object Spanish : BookLanguage("Spanish", "es")

}
