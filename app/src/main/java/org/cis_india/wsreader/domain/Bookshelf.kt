package org.cis_india.wsreader.domain


import android.net.Uri
import java.io.File
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch
import org.readium.r2.shared.publication.Publication
import org.readium.r2.shared.util.AbsoluteUrl
import org.readium.r2.shared.util.DebugError
import org.readium.r2.shared.util.Try
import org.readium.r2.shared.util.asset.AssetRetriever
import org.readium.r2.shared.util.file.FileSystemError
import org.readium.r2.shared.util.format.Format
import org.readium.r2.shared.util.getOrElse
import org.readium.r2.shared.util.toUrl
import org.readium.r2.streamer.PublicationOpener
import org.cis_india.wsreader.data.BookRepository
import org.cis_india.wsreader.data.model.Book
import org.cis_india.wsreader.utils.tryOrLog
import timber.log.Timber

/**
 * The [Bookshelf] supports two different processes:
 * - directly _adding_ the url to a remote asset or an asset from shared storage to the database
 * - _importing_ an asset, that is downloading or copying the publication the asset points to to the app storage
 *   before adding it to the database
 */
class Bookshelf(
    private val bookRepository: BookRepository,
    private val coverStorage: CoverStorage,
    private val publicationOpener: PublicationOpener,
    private val assetRetriever: AssetRetriever,
    private val publicationRetriever: PublicationRetriever,
) {
    sealed class Event {
        data object ImportPublicationSuccess :
            Event()

        class ImportPublicationError(
            val error: ImportError,
        ) : Event()
    }

    val channel: Channel<Event> =
        Channel(Channel.UNLIMITED)

    private val coroutineScope: CoroutineScope =
        MainScope()

    fun importPublicationFromStorage(
        uri: Uri,
        wdIdentifier: String? = null,
        thumbnailUrl: String? = null,
    ) {
        coroutineScope.launch {
            addBookFeedback(publicationRetriever.retrieveFromStorage(uri),wdIdentifier, thumbnailUrl)
        }
    }

    fun importPublicationFromHttp(
        url: AbsoluteUrl,
    ) {
        coroutineScope.launch {
            addBookFeedback(publicationRetriever.retrieveFromHttp(url))
        }
    }

    fun importPublicationFromOpds(
        publication: Publication,
    ) {
        coroutineScope.launch {
            addBookFeedback(publicationRetriever.retrieveFromOpds(publication))
        }
    }

    fun addPublicationFromWeb(
        url: AbsoluteUrl,
    ) {
        coroutineScope.launch {
            addBookFeedback(url)
        }
    }

    fun addPublicationFromStorage(
        url: AbsoluteUrl,
    ) {
        coroutineScope.launch {
            addBookFeedback(url)
        }
    }

    private suspend fun addBookFeedback(
        retrieverResult: Try<PublicationRetriever.Result, ImportError>,
        wdIdentifier: String? = null,
        thumbnailUrl: String? = null,
    ) {
        retrieverResult
            .map { addBook(it.publication.toUrl(), it.format, it.coverUrl, wdIdentifier, thumbnailUrl) }
            .onSuccess { channel.send(Event.ImportPublicationSuccess) }
            .onFailure { channel.send(Event.ImportPublicationError(it)) }
    }

    private suspend fun addBookFeedback(
        url: AbsoluteUrl,
        format: Format? = null,
        coverUrl: AbsoluteUrl? = null,
        wdIdentifier: String? = null
    ) {
        addBook(url, format, coverUrl, wdIdentifier)
            .onSuccess { channel.send(Event.ImportPublicationSuccess) }
            .onFailure { channel.send(Event.ImportPublicationError(it)) }
    }

    private suspend fun addBook(
        url: AbsoluteUrl,
        format: Format? = null,
        coverUrl: AbsoluteUrl? = null,
        wdIdentifier: String? = null,
        thumbnailUrl: String? = null,
    ): Try<Unit, ImportError> {
        val asset =
            if (format == null) {
                assetRetriever.retrieve(url)
            } else {
                assetRetriever.retrieve(url, format)
            }.getOrElse {
                return Try.failure(
                    ImportError.Publication(PublicationError(it))
                )
            }


        publicationOpener.open(
            asset,
            allowUserInteraction = false
        ).onSuccess { publication ->
            val coverFile =
                coverStorage.storeCover(publication, coverUrl)
                    .getOrElse {
                        return Try.failure(
                            ImportError.FileSystem(
                                FileSystemError.IO(it)
                            )
                        )
                    }

            val id = bookRepository.insertBook(
                url,
                asset.format.mediaType,
                publication,
                coverFile,
                wdIdentifier,
                thumbnailUrl
            )
            if (id == -1L) {
                coverFile.delete()
                return Try.failure(
                    ImportError.Database(
                        DebugError("Could not insert book into database.")
                    )
                )
            }
        }
            .onFailure {
                Timber.e("Cannot open publication: $it.")
                return Try.failure(
                    ImportError.Publication(PublicationError(it))
                )
            }

        return Try.success(Unit)
    }

    suspend fun deleteBook(book: Book) {
        val id = book.id!!
        bookRepository.deleteBook(id)
        tryOrLog { book.url.toFile()?.delete() }
        tryOrLog { File(book.cover).delete() }
    }
}