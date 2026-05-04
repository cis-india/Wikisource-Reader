package org.cis_india.wsreader.api.models

import androidx.annotation.Keep
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Keep
@Serializable
data class FormOfWork(
    @SerialName("name")
    val typeOfWork: String? = null,
    @SerialName("wikidata_qid")
    val typeOfWorkWikidataQid: String? = null,
)
