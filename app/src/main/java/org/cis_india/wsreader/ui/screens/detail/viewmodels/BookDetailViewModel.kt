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

package org.cis_india.wsreader.ui.screens.detail.viewmodels

import android.app.Application
import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.viewModelScope
import org.cis_india.wsreader.api.BookAPI
import org.cis_india.wsreader.api.models.Book
import org.cis_india.wsreader.api.models.BookSet
import org.cis_india.wsreader.api.models.ExtraInfo
import org.cis_india.wsreader.data.db.BooksDao
import org.cis_india.wsreader.helpers.Constants
import org.cis_india.wsreader.helpers.PreferenceUtil
import org.cis_india.wsreader.helpers.book.BookDownloader
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.cis_india.wsreader.reader.OpeningError
import org.cis_india.wsreader.reader.ReaderActivityContract
import org.cis_india.wsreader.utils.EventChannel
import javax.inject.Inject
import java.io.File

data class BookDetailScreenState(
    val isLoading: Boolean = true,
    val bookSet: BookSet = BookSet(0, null, null, emptyList()),
    val extraInfo: ExtraInfo = ExtraInfo(),
    val bookLibraryItem: BooksDao? = null,
    val error: String? = null
)

@HiltViewModel
class BookDetailViewModel @Inject constructor(
    application: Application,
    private val bookAPI: BookAPI,
    //val libraryDao: LibraryDao,
    val bookDownloader: BookDownloader,
    private val preferenceUtil: PreferenceUtil,
) : AndroidViewModel(application) {

    private val app get() =
        getApplication<org.cis_india.wsreader.WikisourceReader>()

    val channel = EventChannel(Channel<Event>(Channel.BUFFERED), viewModelScope)

    val allItems: LiveData<List<org.cis_india.wsreader.data.model.Book>> = app.bookRepository.books()

    var state by mutableStateOf(BookDetailScreenState())
        private set

    fun openPublication(
        bookId: Long,
    ) {
        viewModelScope.launch {
            app.readerRepository
                .open(bookId)
                .onFailure {
                    channel.send(Event.OpenPublicationError(it))
                }
                .onSuccess {
                    val arguments = ReaderActivityContract.Arguments(bookId)
                    channel.send(Event.LaunchReader(arguments))
                }
        }
    }

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

                state = state.copy(bookSet = bookSet)

                state = state.copy(
                    bookLibraryItem = null,
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

    suspend fun fetchBookDetails(bookId: String) {
        state = state.copy(isLoading = true)
        try {
            val bookSet = bookAPI.getBookById(bookId).getOrNull()!!

            if (bookSet.isCached) delay(400)

            state = state.copy(bookSet = bookSet, isLoading = false)
        } catch (exc: Exception) {
            state = state.copy(
                error = exc.localizedMessage ?: Constants.UNKNOWN_ERR,
                isLoading = false
            )
        }
    }

    /*
    fun reFetchLibraryItem(bookId: Int, onComplete: (LibraryItem) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            val libraryItem = libraryDao.getItemByBookId(bookId)
            state = state.copy(bookLibraryItem = libraryItem)
            libraryItem?.let { withContext(Dispatchers.Main) { onComplete(libraryItem) } }
        }
    }
     */

    fun downloadBook(
        book: Book, downloadProgressListener: (Float, Int) -> Unit
    ) {
        bookDownloader.downloadBook(book = book,
            downloadProgressListener = downloadProgressListener,
            onDownloadSuccess = { filePath ->
                val file = File(filePath)
                val uri = Uri.fromFile(file)
                app.bookshelf.importPublicationFromStorage(
                    uri = uri,
                    wdIdentifier = book.id.toString(),
                    thumbnailUrl = book.thumbnailUrl.toString()
                )
                viewModelScope.launch {
                    bookAPI.postDownloadedBookDetails(book.wikidataQid, book.title)
                }
                //state = state.copy(bookLibraryItem = libraryDao.getItemByBookId(book.id))
            }
        )
    }



    /*
    Currently not working since ws-export doent return Content-length. See T384803 on Phabricator
     fun addPublicationFromWeb(bookUrl: String) {
        viewModelScope.launch {
            val absoluteUrl = AbsoluteUrl(bookUrl)
            if (absoluteUrl != null){
                app.bookshelf.addPublicationFromWeb(absoluteUrl)
            }
        }
    }
    */


    sealed class Event {

        class OpenPublicationError(
            val error: OpeningError,
        ) : Event()

        class LaunchReader(
            val arguments: ReaderActivityContract.Arguments,
        ) : Event()
    }

}