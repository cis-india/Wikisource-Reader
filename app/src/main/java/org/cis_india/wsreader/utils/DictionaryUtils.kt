package org.cis_india.wsreader.utils

import android.net.Uri

fun getDictionaryUrl(lang: String, word: String): String? {
    val encodedWord = Uri.encode(word)
    return when (lang.lowercase()) {
        "as" -> "https://www.xobdo.org/dic/$encodedWord"
        "fr" -> "https://www.larousse.fr/dictionnaires/francais/$encodedWord"
        "es" -> "https://www.wordreference.com/definicion/$encodedWord"
        "en" -> "https://www.onelook.com/?w=$encodedWord"
        "bn",
        "ca",
        "cs",
        "hi",
        "id",
        "it",
        "kn",
        "ml",
        "mr",
        "pl",
        "sv",
        "ta",
        "te",
        "uk",
        "vi" ->
            "https://$lang.wiktionary.org/wiki/$encodedWord"

        else -> null
    }
}