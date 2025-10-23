/*
 * Copyright 2023 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.cis_india.wsreader.domain

import android.content.Context
import android.net.Uri
import java.io.File
import java.util.UUID
import org.readium.r2.shared.publication.Publication
import org.readium.r2.shared.publication.opds.images
import org.readium.r2.shared.util.AbsoluteUrl
import org.readium.r2.shared.util.DebugError
import org.readium.r2.shared.util.Try
import org.readium.r2.shared.util.asset.AssetRetriever
import org.readium.r2.shared.util.data.ReadError
import org.readium.r2.shared.util.file.FileSystemError
import org.readium.r2.shared.util.format.Format
import org.readium.r2.shared.util.format.FormatHints
import org.readium.r2.shared.util.getOrElse
import org.readium.r2.shared.util.http.HttpClient
import org.readium.r2.shared.util.http.HttpRequest
import org.readium.r2.shared.util.mediatype.MediaType
import org.cis_india.wsreader.utils.copyToNewFile
import org.cis_india.wsreader.utils.extensions.copyToTempFile
import org.cis_india.wsreader.utils.extensions.moveTo
import org.cis_india.wsreader.utils.tryOrLog
import timber.log.Timber

/**
 * Retrieves a publication from a remote or local source and import it into the bookshelf storage.
 *
 * If the source file is a LCP license document, the protected publication will be downloaded.
 */
class PublicationRetriever(
    context: Context,
    private val assetRetriever: AssetRetriever,
    private val httpClient: HttpClient,
    private val bookshelfDir: File,
    private val tempDir: File,
) {
    private companion object {
        private const val EBOOKS_FOLDER_NAME = "ebooks"
    }

    data class Result(
        val publication: File,
        val format: Format,
        val coverUrl: AbsoluteUrl?,
    )

    private val localPublicationRetriever: LocalPublicationRetriever =
        LocalPublicationRetriever(context, tempDir, assetRetriever)

    private val opdsPublicationRetriever: OpdsPublicationRetriever =
        OpdsPublicationRetriever(httpClient, tempDir)

    suspend fun retrieveFromStorage(
        uri: Uri,
    ): Try<Result, ImportError> {

        val file = File(uri.path!!)

        val sourceAsset = assetRetriever.retrieve(file, FormatHints(null))
            .getOrElse {
                return Try.failure(ImportError.Publication(PublicationError(it)))
            }

        val actualFormat = sourceAsset.format

        return Try.success(
            Result(file, actualFormat, coverUrl = null)
        )
    }

    suspend fun retrieveFromOpds(
        publication: Publication,
    ): Try<Result, ImportError> {
        val opdsResult = opdsPublicationRetriever
            .retrieve(publication)
            .getOrElse { return Try.failure(it) }

        val localResult = localPublicationRetriever
            .retrieve(opdsResult.tempFile, opdsResult.mediaType, opdsResult.coverUrl)
            .getOrElse {
                tryOrLog { opdsResult.tempFile.delete() }
                return Try.failure(it)
            }

        val finalResult = moveToBookshelfDir(
            localResult.tempFile,
            localResult.format,
            localResult.coverUrl
        )
            .getOrElse {
                tryOrLog { localResult.tempFile.delete() }
                return Try.failure(it)
            }

        return Try.success(
            Result(finalResult.publication, finalResult.format, finalResult.coverUrl)
        )
    }

    suspend fun retrieveFromHttp(
        url: AbsoluteUrl,
    ): Try<Result, ImportError> {
        val request = HttpRequest(
            url,
            headers = emptyMap()
        )

        val tempFile = when (val result = httpClient.stream(request)) {
            is Try.Failure ->
                return Try.failure(ImportError.Download(result.value))
            is Try.Success -> {
                result.value.body
                    .copyToNewFile(tempDir)
                    .getOrElse { return Try.failure(ImportError.FileSystem(it)) }
            }
        }

        val localResult = localPublicationRetriever
            .retrieve(tempFile)
            .getOrElse {
                tryOrLog { tempFile.delete() }
                return Try.failure(it)
            }

        val finalResult = moveToBookshelfDir(
            localResult.tempFile,
            localResult.format,
            localResult.coverUrl
        )
            .getOrElse {
                tryOrLog { localResult.tempFile.delete() }
                return Try.failure(it)
            }

        return Try.success(
            Result(finalResult.publication, finalResult.format, finalResult.coverUrl)
        )
    }

    private suspend fun moveToBookshelfDir(
        tempFile: File,
        format: Format?,
        coverUrl: AbsoluteUrl?,
    ): Try<Result, ImportError> {
        val actualFormat = format
            ?: assetRetriever.sniffFormat(tempFile)
                .getOrElse {
                    return Try.failure(ImportError.Publication(PublicationError(it)))
                }

        val ebooksDir = File(bookshelfDir, EBOOKS_FOLDER_NAME).apply { if (!exists()) mkdirs() }

        val fileName = "${UUID.randomUUID()}.${actualFormat.fileExtension.value}"

        val bookshelfFile = File(ebooksDir, fileName)

        try {
            tempFile.moveTo(bookshelfFile)
        } catch (e: Exception) {
            Timber.d(e)
            tryOrLog { bookshelfFile.delete() }
            return Try.failure(
                ImportError.Publication(
                    PublicationError.Reading(
                        ReadError.Access(FileSystemError.IO(e))
                    )
                )
            )
        }

        return Try.success(
            Result(bookshelfFile, actualFormat, coverUrl)
        )
    }
}

/**
 * Retrieves a publication from a file (publication) stored on the device.
 */
private class LocalPublicationRetriever(
    private val context: Context,
    private val tempDir: File,
    private val assetRetriever: AssetRetriever,
) {

    data class Result(
        val tempFile: File,
        val format: Format?,
        val coverUrl: AbsoluteUrl?,
    )

    /**
     * Retrieves the publication from the given local [uri].
     */
    suspend fun retrieve(
        uri: Uri,
    ): Try<Result, ImportError> {
        val tempFile = uri.copyToTempFile(context, tempDir)
            .getOrElse {
                return Try.failure(ImportError.ContentResolver(it))
            }
        return retrieveFromStorage(tempFile, coverUrl = null)
            .onFailure { tryOrLog { tempFile.delete() } }
    }

    /**
     * Retrieves the publication stored at the given [tempFile].
     */
    suspend fun retrieve(
        tempFile: File,
        mediaType: MediaType? = null,
        coverUrl: AbsoluteUrl? = null,
    ): Try<Result, ImportError> {
        return retrieveFromStorage(tempFile, mediaType, coverUrl)
    }

    private suspend fun retrieveFromStorage(
        tempFile: File,
        mediaType: MediaType? = null,
        coverUrl: AbsoluteUrl? = null,
    ): Try<Result, ImportError> {
        val sourceAsset = assetRetriever.retrieve(tempFile, FormatHints(mediaType))
            .getOrElse {
                return Try.failure(ImportError.Publication(PublicationError(it)))
            }

        sourceAsset.close()
        return Try.success(
            Result(tempFile, sourceAsset.format, coverUrl)
        )
    }
}

/**
 * Retrieves a publication from an OPDS entry.
 */
private class OpdsPublicationRetriever(
    private val httpClient: HttpClient,
    private val tempDir: File,
) {

    data class Result(
        val tempFile: File,
        val mediaType: MediaType?,
        val coverUrl: AbsoluteUrl?,
    )

    /**
     * Retrieves the file of the given OPDS [publication].
     */
    suspend fun retrieve(publication: Publication): Try<Result, ImportError> {
        val acquisitionLink = publication.links
            .firstOrNull { it.mediaType?.isPublication == true || it.mediaType == MediaType.LCP_LICENSE_DOCUMENT }

        val publicationUrl = (acquisitionLink?.url() as? AbsoluteUrl)
            ?: return Try.failure(
                ImportError.Opds(DebugError("No supported link to acquire publication."))
            )

        val mediaType = acquisitionLink.mediaType

        val coverUrl = publication.images.firstOrNull()
            ?.let { publication.url(it) as? AbsoluteUrl }

        val request = HttpRequest(
            publicationUrl,
            headers = emptyMap()
        )

        val file = when (val result = httpClient.stream(request)) {
            is Try.Failure ->
                return Try.failure(ImportError.Download(result.value))
            is Try.Success -> {
                result.value.body
                    .copyToNewFile(tempDir)
                    .getOrElse { return Try.failure(ImportError.FileSystem(it)) }
            }
        }

        return Try.success(
            Result(file, mediaType = mediaType, coverUrl = coverUrl)
        )
    }
}
