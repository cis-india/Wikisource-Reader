/*
 * Copyright 2022 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.cis_india.wsreader.reader

import androidx.datastore.core.DataStore
import org.cis_india.wsreader.MyneApp
import androidx.datastore.preferences.core.Preferences as JetpackPreferences
import org.json.JSONObject
import org.readium.adapter.exoplayer.audio.ExoPlayerEngineProvider
import org.readium.adapter.pdfium.navigator.PdfiumEngineProvider
import org.readium.navigator.media.audio.AudioNavigatorFactory
import org.readium.navigator.media.tts.TtsNavigatorFactory
import org.readium.r2.navigator.epub.EpubNavigatorFactory
import org.readium.r2.navigator.pdf.PdfNavigatorFactory
import org.readium.r2.shared.ExperimentalReadiumApi
import org.readium.r2.shared.publication.Locator
import org.readium.r2.shared.publication.Publication
import org.readium.r2.shared.publication.allAreHtml
import org.readium.r2.shared.util.DebugError
import org.readium.r2.shared.util.Try
import org.readium.r2.shared.util.getOrElse
import org.cis_india.wsreader.Readium
import org.cis_india.wsreader.data.BookRepository
import org.cis_india.wsreader.domain.PublicationError
import org.cis_india.wsreader.reader.preferences.AndroidTtsPreferencesManagerFactory
import org.cis_india.wsreader.reader.preferences.EpubPreferencesManagerFactory
import org.cis_india.wsreader.reader.preferences.ExoPlayerPreferencesManagerFactory
import org.cis_india.wsreader.reader.preferences.PdfiumPreferencesManagerFactory
import org.cis_india.wsreader.utils.CoroutineQueue
import timber.log.Timber

/**
 * Open and store publications in order for them to be listened or read.
 *
 * Ensure you call [open] before any attempt to start a [ReaderActivity].
 * Pass the method result to the activity to enable it to know which current publication it must
 * retrieve from this repository - media or visual.
 */
@OptIn(ExperimentalReadiumApi::class)
class ReaderRepository(
    private val application: MyneApp,
    private val readium: Readium,
    private val bookRepository: BookRepository,
    private val preferencesDataStore: DataStore<JetpackPreferences>,
) {

    private val coroutineQueue: CoroutineQueue =
        CoroutineQueue()

    private val repository: MutableMap<Long, ReaderInitData> =
        mutableMapOf()

    private val mediaServiceFacade: MediaServiceFacade =
        MediaServiceFacade(application)

    fun isEmpty() =
        repository.isEmpty()

    operator fun get(bookId: Long): ReaderInitData? =
        repository[bookId]

    suspend fun open(bookId: Long): Try<Unit, OpeningError> =
        coroutineQueue.await { doOpen(bookId) }

    private suspend fun doOpen(bookId: Long): Try<Unit, OpeningError> {
        if (bookId in repository.keys) {
            return Try.success(Unit)
        }

        val book = checkNotNull(bookRepository.get(bookId)) { "Cannot find book in database." }

        val asset = readium.assetRetriever.retrieve(
            book.url,
            book.mediaType
        ).getOrElse {
            return Try.failure(
                OpeningError.PublicationError(
                    PublicationError(it)
                )
            )
        }

        val publication = readium.publicationOpener.open(
            asset,
            allowUserInteraction = true
        ).getOrElse {
            return Try.failure(
                OpeningError.PublicationError(
                    PublicationError(it)
                )
            )
        }

        val initialLocator = book.progression
            ?.let { Locator.fromJSON(JSONObject(it)) }
        val sessionConfig = SessionReaderConfig(
            continuousChapters = book.continuousChapters
        )

        val readerInitData = when {
            publication.conformsTo(Publication.Profile.AUDIOBOOK) ->
                openAudio(bookId, publication, initialLocator)
            publication.conformsTo(Publication.Profile.EPUB) || publication.readingOrder.allAreHtml ->
                openEpub(bookId, publication, initialLocator, sessionConfig = sessionConfig)
            publication.conformsTo(Publication.Profile.PDF) ->
                openPdf(bookId, publication, initialLocator, sessionConfig = sessionConfig)
            publication.conformsTo(Publication.Profile.DIVINA) ->
                openImage(bookId, publication, initialLocator, sessionConfig = sessionConfig)
            else ->
                Try.failure(
                    OpeningError.CannotRender(
                        DebugError("No navigator supports this publication.")
                    )
                )
        }

        return readerInitData.map {
            repository[bookId] = it

            if (it is MediaReaderInitData) {
                mediaServiceFacade.openSession(bookId, it.mediaNavigator)
            }
        }
    }

    private suspend fun openAudio(
        bookId: Long,
        publication: Publication,
        initialLocator: Locator?,
    ): Try<MediaReaderInitData, OpeningError> {
        val preferencesManager = ExoPlayerPreferencesManagerFactory(preferencesDataStore)
            .createPreferenceManager(bookId)
        val initialPreferences = preferencesManager.preferences.value

        val navigatorFactory = AudioNavigatorFactory(
            publication,
            ExoPlayerEngineProvider(application)
        ) ?: return Try.failure(
            OpeningError.CannotRender(
                DebugError("Cannot create audio navigator factory.")
            )
        )

        val navigator = navigatorFactory.createNavigator(
            initialLocator,
            initialPreferences
        ).getOrElse {
            return Try.failure(
                when (it) {
                    is AudioNavigatorFactory.Error.EngineInitialization ->
                        OpeningError.AudioEngineInitialization(it)
                    is AudioNavigatorFactory.Error.UnsupportedPublication ->
                        OpeningError.CannotRender(it)
                }
            )
        }

        val initData = MediaReaderInitData(
            bookId,
            publication,
            navigator,
            preferencesManager,
            navigatorFactory
        )
        return Try.success(initData)
    }

    private suspend fun openEpub(
        bookId: Long,
        publication: Publication,
        initialLocator: Locator?,
        sessionConfig: SessionReaderConfig,
    ): Try<EpubReaderInitData, OpeningError> {
        val preferencesManager = EpubPreferencesManagerFactory(preferencesDataStore)
            .createPreferenceManager(bookId)
        val navigatorFactory = EpubNavigatorFactory(publication)
        val ttsInitData = getTtsInitData(bookId, publication)

        val initData = EpubReaderInitData(
            bookId,
            publication,
            initialLocator,
            preferencesManager,
            navigatorFactory,
            ttsInitData,
            sessionConfig = sessionConfig
        )
        return Try.success(initData)
    }

    private suspend fun openPdf(
        bookId: Long,
        publication: Publication,
        initialLocator: Locator?,
        sessionConfig: SessionReaderConfig,
    ): Try<PdfReaderInitData, OpeningError> {
        val preferencesManager = PdfiumPreferencesManagerFactory(preferencesDataStore)
            .createPreferenceManager(bookId)
        val pdfEngine = PdfiumEngineProvider()
        val navigatorFactory = PdfNavigatorFactory(publication, pdfEngine)
        val ttsInitData = getTtsInitData(bookId, publication)

        val initData = PdfReaderInitData(
            bookId,
            publication,
            initialLocator,
            preferencesManager,
            navigatorFactory,
            ttsInitData,
            sessionConfig
        )
        return Try.success(initData)
    }

    private suspend fun openImage(
        bookId: Long,
        publication: Publication,
        initialLocator: Locator?,
        sessionConfig: SessionReaderConfig,
    ): Try<ImageReaderInitData, OpeningError> {
        val initData = ImageReaderInitData(
            bookId = bookId,
            publication = publication,
            initialLocation = initialLocator,
            ttsInitData = getTtsInitData(bookId, publication),
            sessionConfig
        )
        return Try.success(initData)
    }

    private suspend fun getTtsInitData(
        bookId: Long,
        publication: Publication,
    ): TtsInitData? {
        val preferencesManager = AndroidTtsPreferencesManagerFactory(preferencesDataStore)
            .createPreferenceManager(bookId)
        val navigatorFactory = TtsNavigatorFactory(
            application,
            publication
        ) ?: return null
        return TtsInitData(mediaServiceFacade, navigatorFactory, preferencesManager)
    }

    fun close(bookId: Long) {
        coroutineQueue.launch {
            Timber.v("Closing Publication $bookId.")
            when (val initData = repository.remove(bookId)) {
                is MediaReaderInitData -> {
                    mediaServiceFacade.closeSession()
                    initData.publication.close()
                }
                is VisualReaderInitData -> {
                    mediaServiceFacade.closeSession()
                    initData.publication.close()
                }
                null, is DummyReaderInitData -> {
                    // Do nothing
                }
            }
        }
    }
}
