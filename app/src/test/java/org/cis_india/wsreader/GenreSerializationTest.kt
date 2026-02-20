package org.cis_india.wsreader

import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import org.cis_india.wsreader.api.models.Genre
import org.junit.Assert.assertEquals
import org.junit.Test

class GenreSerializationTest {
    private val json = Json { ignoreUnknownKeys = true }

    @Test
    fun testStringGenre() {
        val input = """["Action", "Drama"]"""
        val result = json.decodeFromString<List<Genre>>(input)
        assertEquals(2, result.size)
        assertEquals("Action", result[0].name)
        assertEquals("Drama", result[1].name)
    }

    @Test
    fun testObjectGenre() {
        val input = """[{"name": "Action", "genre_wikidata_qid": "Q123"}]"""
        val result = json.decodeFromString<List<Genre>>(input)
        assertEquals(1, result.size)
        assertEquals("Action", result[0].name)
        assertEquals("Q123", result[0].genrewikidataqid)
    }

    @Test
    fun testMixedGenre() {
        val input = """["Action", {"name": "Drama", "genre_wikidata_qid": "Q456"}]"""
        val result = json.decodeFromString<List<Genre>>(input)
        assertEquals(2, result.size)
        assertEquals("Action", result[0].name)
        assertEquals("Drama", result[1].name)
        assertEquals("Q456", result[1].genrewikidataqid)
    }
}
