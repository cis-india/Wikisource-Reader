package org.cis_india.wsreader.api.models

import androidx.annotation.Keep
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Keep
@Serializable
data class Editor(
    @SerialName("birth_year")
    val birthYear: Int? = null,
    @SerialName("death_year")
    val deathYear: Int? = null,
    @SerialName("name")
    val name: String = "N/A",
    @SerialName("person_wikidata_qid")
    val pwikidataqid: String = "N/A",
)