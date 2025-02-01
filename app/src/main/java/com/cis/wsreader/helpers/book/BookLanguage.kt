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
    data object Spanish : BookLanguage("Spanish", "es")

    @Keep
    data object Marathi : BookLanguage("Marathi", "mr")

    @Keep
    data object German : BookLanguage("Hindi", "hi")

    @Keep
    data object Greek : BookLanguage("Telugu", "te")

}
