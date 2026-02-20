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
import kotlinx.serialization.json.JsonDecoder
import kotlinx.serialization.json.JsonPrimitive

@Keep
@Serializable(with = FlexibleGenreSerializer::class)
data class Genre(
    @SerialName("name")
    val name: String = "N/A",
    @SerialName("genre_wikidata_qid")
    val genrewikidataqid: String = "N/A",
)

object FlexibleGenreSerializer : KSerializer<Genre> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("Genre", PrimitiveKind.STRING)

    override fun deserialize(decoder: Decoder): Genre {
        val input = (decoder as JsonDecoder).decodeJsonElement()
        return if (input is JsonPrimitive && input.isString) {
            Genre(name = input.content)
        } else {
            val surrogate = decoder.json.decodeFromJsonElement(GenreSurrogate.serializer(), input)
            Genre(name = surrogate.name, genrewikidataqid = surrogate.genrewikidataqid)
        }
    }

    override fun serialize(encoder: Encoder, value: Genre) {
        val surrogate = GenreSurrogate(value.name, value.genrewikidataqid)
        encoder.encodeSerializableValue(GenreSurrogate.serializer(), surrogate)
    }
}

@Serializable
@SerialName("Genre")
private data class GenreSurrogate(
    val name: String = "N/A",
    @SerialName("genre_wikidata_qid")
    val genrewikidataqid: String = "N/A"
)
