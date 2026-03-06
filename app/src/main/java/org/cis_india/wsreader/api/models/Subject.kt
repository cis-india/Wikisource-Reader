package org.cis_india.wsreader.api.models

import androidx.annotation.Keep
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Keep
@Serializable
data class Subject(
    @SerialName("name")
    val name: String? = null,
    @SerialName("subject_wikidata_qid")
    val subjectWikidataQid: String? = null,
)
