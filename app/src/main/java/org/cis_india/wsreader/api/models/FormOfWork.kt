package org.cis_india.wsreader.api.models

import androidx.annotation.Keep
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Keep
@Serializable
data class FormOfWork(
    @SerialName("type_of_work")
    val typeOfWork: String? = null,
    @SerialName("type_of_work_wikidata_qid")
    val typeOfWorkWikidataQid: String? = null,
)
