package org.cis_india.wsreader.ui.screens.home.viewmodels

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlinx.coroutines.CancellationException
import org.cis_india.wsreader.api.models.Book
import org.cis_india.wsreader.api.models.BookSet
import org.cis_india.wsreader.helpers.Constants

internal data class SearchResultsState(
    val books: List<Book> = emptyList(),
    val isLoading: Boolean = false,
    val endReached: Boolean = false,
    val error: String? = null
)

/** Pagination for one search; a reset invalidates any response still in flight. */
internal class SearchPagination(
    private val request: suspend (String, Long) -> Result<BookSet>
) {
    var state by mutableStateOf(SearchResultsState())
        private set
    private var query = ""
    private var page = 1L
    private var generation = 0L

    fun reset(query: String) {
        generation++
        this.query = query
        page = 1L
        state = SearchResultsState()
    }

    suspend fun loadNextPage() {
        if (query.isBlank() || state.isLoading || state.endReached) return
        val currentGeneration = generation
        state = state.copy(isLoading = true, error = null)
        try {
            val result = request(query, page)
            if (currentGeneration != generation) return
            result.fold(onSuccess = { bookSet ->
                state = state.copy(
                    books = (state.books + bookSet.books.filter { it.epubUrl != null })
                        .distinctBy { it.wikidataQid },
                    endReached = bookSet.next == null
                )
                page++
            }, onFailure = { error ->
                if (error is CancellationException) throw error
                state = state.copy(error = error.localizedMessage ?: Constants.UNKNOWN_ERR)
            })
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (error: Exception) {
            if (currentGeneration == generation) {
                state = state.copy(error = error.localizedMessage ?: Constants.UNKNOWN_ERR)
            }
        } finally {
            if (currentGeneration == generation) state = state.copy(isLoading = false)
        }
    }
}
