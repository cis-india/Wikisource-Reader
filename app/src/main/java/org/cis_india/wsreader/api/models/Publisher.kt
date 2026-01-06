package org.cis_india.wsreader.api.models

import androidx.annotation.Keep
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Keep
@Serializable
data class Publisher(
    @SerialName("name")
    val name: String? = "N/A",
    @SerialName("wikidata_qid")
    val pwikidataqid: String? = "N/A",
)
