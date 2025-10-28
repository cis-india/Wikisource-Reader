/**
 * Copyright (c) [2022 - Present] Stɑrry Shivɑm
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.cis_india.wsreader.ui.screens.home.viewmodels

import androidx.compose.runtime.MutableState
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import org.cis_india.wsreader.api.BookAPI
import org.cis_india.wsreader.api.models.Book
import org.cis_india.wsreader.helpers.Constants
import org.cis_india.wsreader.helpers.NetworkObserver
import org.cis_india.wsreader.helpers.Paginator
import org.cis_india.wsreader.helpers.PreferenceUtil
import org.cis_india.wsreader.helpers.book.BookLanguage
import org.cis_india.wsreader.helpers.book.BookSortOption
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AllBooksState(
    val isLoading: Boolean = false,
    val items: List<Book> = emptyList(),
    val error: String? = null,
    val endReached: Boolean = false,
    val page: Long = 1L
)

data class SearchBarState(
    val searchText: String = "",
    val isSearchBarVisible: Boolean = false,
    val isSortMenuVisible: Boolean = false,
    val isSearching: Boolean = false,
    val searchResults: List<Book> = emptyList()
)

sealed class UserAction {
    data object SearchIconClicked : UserAction()
    data object CloseIconClicked : UserAction()
    data class TextFieldInput(
        val text: String,
        val networkStatus: NetworkObserver.Status
    ) : UserAction()

    data class LanguageItemClicked(val language: BookLanguage) : UserAction()
    data class SortOptionClicked(val sortOption: BookSortOption) : UserAction()
}

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val bookAPI: BookAPI,
    private val preferenceUtil: PreferenceUtil
) : ViewModel() {
    var allBooksState by mutableStateOf(AllBooksState())
    var searchBarState by mutableStateOf(SearchBarState())

    private val _language: MutableState<BookLanguage> = mutableStateOf(getPreferredLanguage())
    val language: State<BookLanguage> = _language

    private val _sortOption: MutableState<BookSortOption> = mutableStateOf(getPreferredSortOption())
    val sortOption: State<BookSortOption> = _sortOption

    private var searchJob: Job? = null

    private val pagination = Paginator(initialPage = allBooksState.page, onLoadUpdated = {
        allBooksState = allBooksState.copy(isLoading = it)
    }, onRequest = { nextPage ->
        try {
            // Only add delay when loading first page to show shimmer effect
            // and avoid flickering when navigating to home from welcome screen
            // and immediately loading the first page.
            if (nextPage == 1L) delay(400L)
            bookAPI.getAllBooks(nextPage, language.value, sortOption.value.apiValue)
        } catch (exc: Exception) {
            Result.failure(exc)
        }
    }, getNextPage = {
        allBooksState.page + 1L
    }, onError = {
        allBooksState = allBooksState.copy(error = it?.localizedMessage ?: Constants.UNKNOWN_ERR)
    }, onSuccess = { bookSet, newPage ->

        val books = run {
            val books =
                bookSet.books.filter { it.epubUrl != null } as ArrayList<Book>
            // Ignore ...
            val index = books.indexOfFirst { it.id == 1513 }
            if (index != -1) {
                books.removeAt(index)
            }
            books
        }

        allBooksState = allBooksState.copy(
            items = (allBooksState.items + books),
            page = newPage,
            endReached = books.isEmpty()
        )
    })

    fun loadNextItems() {
        viewModelScope.launch {
            pagination.loadNextItems()
        }
    }

    fun reloadItems() {
        pagination.reset()
        allBooksState = AllBooksState()
        loadNextItems()
    }

    fun onAction(userAction: UserAction) {
        when (userAction) {
            UserAction.CloseIconClicked -> {
                searchBarState = searchBarState.copy(isSearchBarVisible = false)
            }

            UserAction.SearchIconClicked -> {
                searchBarState = searchBarState.copy(isSearchBarVisible = true)
            }

            is UserAction.TextFieldInput -> {
                searchBarState = searchBarState.copy(searchText = userAction.text)
                if (userAction.networkStatus == NetworkObserver.Status.Available) {
                    searchJob?.cancel()
                    searchJob = viewModelScope.launch {
                        if (userAction.text.isNotBlank()) {
                            searchBarState = searchBarState.copy(isSearching = true)
                        }
                        delay(500L)
                        searchBooks(userAction.text)
                    }
                }
            }

            is UserAction.LanguageItemClicked -> {
                changeLanguage(userAction.language)
            }

            is UserAction.SortOptionClicked -> {
                changeSortOption(userAction.sortOption)
            }
        }
    }

    private suspend fun searchBooks(query: String) {
        if (query.isBlank()) return // no need to search for empty query
        val bookSet = bookAPI.searchBooks(query)
        val books = bookSet.getOrNull()?.books?.filter { it.epubUrl != null }
        searchBarState = searchBarState.copy(
            searchResults = books ?: emptyList(),
            isSearching = false
        )
    }

    private fun changeLanguage(language: BookLanguage) {
        _language.value = language
        preferenceUtil.putString(PreferenceUtil.PREFERRED_BOOK_LANG_STR, language.isoCode)
        reloadItems()
    }

    private fun changeSortOption(sortOption: BookSortOption) {
        _sortOption.value = sortOption
        preferenceUtil.putString(PreferenceUtil.PREFERRED_BOOK_SORT_STR, sortOption.apiValue)
        reloadItems()
    }

    private fun getPreferredLanguage(): BookLanguage {
        val isoCode = preferenceUtil.getString(
            PreferenceUtil.PREFERRED_BOOK_LANG_STR,
            BookLanguage.AllBooks.isoCode
        )
        return BookLanguage.getAllLanguages().find { it.isoCode == isoCode }
            ?: BookLanguage.AllBooks
    }

    private fun getPreferredSortOption(): BookSortOption {
        val apiValue = preferenceUtil.getString(
            PreferenceUtil.PREFERRED_BOOK_SORT_STR,
            BookSortOption.PopularityHighToLow.apiValue
        )
        return BookSortOption.getAllSortOptions().find { it.apiValue == apiValue }
            ?: BookSortOption.PopularityHighToLow
    }
}