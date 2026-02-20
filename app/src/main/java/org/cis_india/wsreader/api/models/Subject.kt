package org.cis_india.wsreader.api.models

import androidx.annotation.Keep
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonDecoder
import kotlinx.serialization.json.JsonPrimitive

@Keep
@Serializable(with = FlexibleSubjectSerializer::class)
data class Subject(
    @SerialName("name")
    val name: String = "N/A",
    @SerialName("subject_wikidata_qid")
    val subjectwikidataqid: String = "N/A",
)

object FlexibleSubjectSerializer : KSerializer<Subject> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("Subject", PrimitiveKind.STRING)

    override fun deserialize(decoder: Decoder): Subject {
        val input = (decoder as JsonDecoder).decodeJsonElement()
        return if (input is JsonPrimitive && input.isString) {
            Subject(name = input.content)
        } else {
            val surrogate = decoder.json.decodeFromJsonElement(SubjectSurrogate.serializer(), input)
            Subject(name = surrogate.name, subjectwikidataqid = surrogate.subjectwikidataqid)
        }
    }

    override fun serialize(encoder: Encoder, value: Subject) {
        val surrogate = SubjectSurrogate(value.name, value.subjectwikidataqid)
        encoder.encodeSerializableValue(SubjectSurrogate.serializer(), surrogate)
    }
}

@Serializable
@SerialName("Subject")
private data class SubjectSurrogate(
    val name: String = "N/A",
    @SerialName("subject_wikidata_qid")
    val subjectwikidataqid: String = "N/A"
)
