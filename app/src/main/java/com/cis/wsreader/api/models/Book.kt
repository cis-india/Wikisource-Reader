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

package com.cis.wsreader.api.models

import androidx.annotation.Keep
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Keep
@Serializable
data class Book(
    @SerialName("authors")
    val authors: List<Author>,
    @SerialName("view_count")
    val downloadCount: Int,
    @SerialName("wikidata_qid")
    val wikidataQid: String,
    @SerialName("languages")
    val languages: List<String>,
    @SerialName("title")
    val title: String,
    @SerialName("translators")
    val translators: List<Translator>,
    @SerialName("title_native_language")
    val titleNativeLanguage: String? = null,
    @SerialName("epub_url")
    val epubUrl: String? = null,
    @SerialName("ws_url")
    val wsUrl: String? = null,
    @SerialName("thumbnail_url")
    val thumbnailUrl: String? = null,
    @SerialName("date_of_publication")
    val dateOfPublication: String? = null,
) {
    val id: Int
        get() = wikidataQid.removePrefix("Q").toInt()
}

