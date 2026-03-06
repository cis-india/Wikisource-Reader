package org.cis_india.wsreader.api.models

import androidx.annotation.Keep
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Keep
@Serializable
data class Genre(
    @SerialName("name")
    val name: String? = null,
    @SerialName("genre_wikidata_qid")
    val genreWikidataQid: String? = null,
)
