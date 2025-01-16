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

package com.cis.wsreader.ui.screens.detail.viewmodels

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cis.wsreader.api.BookAPI
import com.cis.wsreader.api.models.Book
import com.cis.wsreader.api.models.BookSet
import com.cis.wsreader.api.models.ExtraInfo
import com.cis.wsreader.database.library.LibraryDao
import com.cis.wsreader.database.library.LibraryItem
import com.cis.wsreader.helpers.Constants
import com.cis.wsreader.helpers.PreferenceUtil
import com.cis.wsreader.helpers.book.BookDownloader
import com.cis.wsreader.helpers.book.BookUtils
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

data class BookDetailScreenState(
    val isLoading: Boolean = true,
    val bookSet: BookSet = BookSet(0, null, null, emptyList()),
    val extraInfo: ExtraInfo = ExtraInfo(),
    val bookLibraryItem: LibraryItem? = null,
    val error: String? = null
)


@HiltViewModel
class BookDetailViewModel @Inject constructor(
    private val bookAPI: BookAPI,
    val libraryDao: LibraryDao,
    val bookDownloader: BookDownloader,
    private val preferenceUtil: PreferenceUtil
) : ViewModel() {
    var state by mutableStateOf(BookDetailScreenState())
        private set

    fun getInternalReaderSetting() = preferenceUtil.getBoolean(
        PreferenceUtil.INTERNAL_READER_BOOL, true
    )

    fun getBookDetails(bookId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                // This method can be called multiple times. like if
                // request fails and user clicks on retry button.
                // So, we are setting isLoading to true before making
                // the request to show the loading indicator in the UI.
                state = state.copy(isLoading = true)
                val bookSet = bookAPI.getBookById(bookId).getOrNull()!!
                val extraInfo = null
                //val extraInfo = bookAPI.getExtraInfo(bookSet.books.first().title)
                // If API response is cached, it will not show the loading
                // indicator. So, we are adding a delay to show the loading
                // indicator. This is just for better UX.
                if (bookSet.isCached) delay(400)

                /*
                state = if (extraInfo != null) {
                    state.copy(bookSet = bookSet, extraInfo = extraInfo)
                } else {
                    state.copy(bookSet = bookSet)
                }
                */
                state = state.copy(bookSet = bookSet)

                state = state.copy(
                    bookLibraryItem = libraryDao.getItemByBookId(bookId.toInt()),
                    isLoading = false,
                    error = null
                )
            } catch (exc: Exception) {
                state = state.copy(
                    error = exc.localizedMessage ?: Constants.UNKNOWN_ERR,
                    isLoading = false
                )
            }
        }
    }

    fun reFetchLibraryItem(bookId: Int, onComplete: (LibraryItem) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            val libraryItem = libraryDao.getItemByBookId(bookId)
            state = state.copy(bookLibraryItem = libraryItem)
            libraryItem?.let { withContext(Dispatchers.Main) { onComplete(libraryItem) } }
        }
    }

    fun downloadBook(
        book: Book, downloadProgressListener: (Float, Int) -> Unit
    ) {
        bookDownloader.downloadBook(book = book,
            downloadProgressListener = downloadProgressListener,
            onDownloadSuccess = { filePath ->
                insertIntoDB(book = book, filePath = filePath)
                state = state.copy(bookLibraryItem = libraryDao.getItemByBookId(book.id))
            }
        )
    }

    private fun insertIntoDB(book: Book, filePath: String) {
        val libraryItem = LibraryItem(
            bookId = book.id,
            title = book.title,
            authors = BookUtils.getAuthorsAsStringen(book.authors),
            filePath = filePath,
            createdAt = System.currentTimeMillis()
        )
        libraryDao.insert(libraryItem)
    }
}