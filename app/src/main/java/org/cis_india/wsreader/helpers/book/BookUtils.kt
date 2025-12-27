/**
 * Copyright (c) [2022 - Present] Stɑrry Shivɑm
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.cis_india.wsreader.helpers.book

import org.cis_india.wsreader.api.models.Author
import org.cis_india.wsreader.api.models.Editor
import org.cis_india.wsreader.api.models.Translator
import java.util.Locale
import kotlinx.coroutines.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.cis_india.wsreader.api.models.PlacesOfPublication
import org.cis_india.wsreader.api.models.Publisher
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

object BookUtils {

    // cached authors
    val authorStringCache = mutableMapOf<String, String>()

    /**
     * Converts the list of authors into a single string.
     *
     * @param authors List of authors.
     * @return String representation of the authors.
     */
    fun getAuthorsAsStringen(authors: List<Author>, language: String): String {
        return if (authors.isEmpty()) {
            "Unknown Author"
        } else {
            val authorIdsKey = authors.joinToString("|") {
                it.pwikidataqid
            }
            val cacheKey = "authors-$authorIdsKey-$language"

            // check for value in cache first
            authorStringCache[cacheKey]?.let { cachedResult ->
                return cachedResult
            }

            authors.joinToString(", ") { fixAuthorName(it.name) }
        }
    }

    suspend fun getAuthorsAsString(authors: List<Author>, language: String): String {
        // Construct a unique cache key
        // using unique pwikidataqid
        val authorIdsKey = authors.joinToString("|") {
            it.pwikidataqid
        }
        val cacheKey = "authors-$authorIdsKey-$language"

        // Update value from fetch
        // This updates author if value has changed.
        val result = if (authors.isEmpty()) {
            "Unknown Author"
        } else {
            val names = coroutineScope {
                authors.map { author ->
                    async {
                        val name = fetchLabelFromWikidata(author.pwikidataqid, language)
                        fixAuthorName(name ?: author.name)
                    }
                }.awaitAll() // This will collect the results
            }
            names.joinToString(", ")
        }

        // Add value to cache
        authorStringCache[cacheKey] = result

        return result
    }


    suspend fun getPublishersAsString(publishers: List<Publisher>, language: String, unknownOfPublisher: String): String {

        // Update value from fetch
        // This updates Publisher if value has changed.
        val result = if (publishers.isEmpty()) {
            unknownOfPublisher
        } else {
            val names = coroutineScope {
                publishers.map { publisher ->
                    async {
                        val name = fetchLabelFromWikidata(publisher.pwikidataqid, language)
                        fixAuthorName(name ?: publisher.name ?: unknownOfPublisher)
                    }
                }.awaitAll() // This will collect the results
            }
            names.joinToString(", ")
        }

        return result
    }

    suspend fun getPlacesOfPublicationAsString(placesOfPublication: List<PlacesOfPublication>, language: String, unknownPlacesOfPublication: String): String {

        // Update value from fetch
        // This updates Places of publication if value has changed.
        val result = if (placesOfPublication.isEmpty()) {
            unknownPlacesOfPublication
        } else {
            val names = coroutineScope {
                placesOfPublication.map { placeOfPublication ->
                    async {
                        val name = fetchLabelFromWikidata(placeOfPublication.pwikidataqid, language)
                        fixAuthorName(name ?: placeOfPublication.name ?: unknownPlacesOfPublication)
                    }
                }.awaitAll() // This will collect the results
            }
            names.joinToString(", ")
        }

        return result
    }

    suspend fun getEditors(editors: List<Editor>, language: String): List<String> {
        if (editors.isEmpty()) return emptyList()

        return coroutineScope {
            editors.map { editor ->
                async {
                    val name = fetchLabelFromWikidata(editor.pwikidataqid, language)
                    fixAuthorName(name ?: editor.name)
                }
            }.awaitAll()
        }.filter { it.isNotBlank() }
    }

    suspend fun getTranslators(translators: List<Translator>, language: String): List<String> {
        if (translators.isEmpty()) return emptyList()

        return coroutineScope {
            translators.map { translator ->
                async {
                    val name = fetchLabelFromWikidata(translator.pwikidataqid, language)
                    fixAuthorName(name ?: translator.name)
                }
            }.awaitAll()
        }.filter { it.isNotBlank() }
    }

    /**
     * For some weird reasons, gutenberg gives name of authors in
     * reversed, where first name and last are separated by a comma
     * Eg: "Fyodor Dostoyevsky" becomes "Dostoyevsky, Fyodor", This
     * function fixes that and returns name in correct format.
     *
     * @param name Name of the author.
     * @return Name of the author in correct format.
     */
    private fun fixAuthorName(name: String): String {
        return name.split(",").reversed().joinToString(" ") { it.trim() }
    }

    private suspend fun fetchLabelFromWikidata(wikidataId: String?, language: String): String? {
        if (wikidataId.isNullOrEmpty()) return null

        val apiUrl = "https://www.wikidata.org/w/api.php?action=wbgetentities&props=labels&ids=$wikidataId&languages=$language&format=json"

        return withContext(Dispatchers.IO) {
            try {
                val connection = URL(apiUrl).openConnection() as HttpURLConnection
                connection.requestMethod = "GET"
                connection.connect()

                if (connection.responseCode == HttpURLConnection.HTTP_OK) {
                    val response = connection.inputStream.bufferedReader().use { it.readText() }
                    val json = JSONObject(response)
                    val labels = json.getJSONObject("entities")
                        .getJSONObject(wikidataId)
                        .getJSONObject("labels")

                    if (labels.has(language)) {
                        labels.getJSONObject(language).getString("value")
                    } else {
                        null
                    }
                } else {
                    null
                }
            } catch (e: Exception) {
                e.printStackTrace()
                null
            }
        }
    }

    /**
     * Converts the list of languages into a single string.
     *
     * @param languages List of languages.
     * @return String representation of the languages.
     */
    fun getLanguagesAsString(languages: List<String>): String {
        return languages.joinToString(", ") { Locale(it).displayLanguage }
    }

    /**
     * Converts the list of subjects into a single string.
     *
     * @param subjects List of subjects.
     * @param limit Maximum number of subjects to show.
     * @return String representation of the subjects.
     */

    /*
    fun getSubjectsAsString(subjects: List<String>, limit: Int): String {
        val allSubjects = subjects.flatMap { it.split("--") }.map { it.trim() }.toSet()
        val truncatedSubs = if (allSubjects.size > limit) allSubjects.take(limit) else allSubjects
        return truncatedSubs.joinToString(", ")
    }
    */
}