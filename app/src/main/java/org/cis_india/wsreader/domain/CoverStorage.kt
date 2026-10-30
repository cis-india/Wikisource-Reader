package org.cis_india.wsreader.domain

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import java.io.File
import java.io.FileOutputStream
import java.util.UUID
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.readium.r2.shared.publication.Publication
import org.readium.r2.shared.publication.services.cover
import org.readium.r2.shared.util.AbsoluteUrl
import org.readium.r2.shared.util.Try
import org.readium.r2.shared.util.http.HttpClient
import org.readium.r2.shared.util.http.HttpError
import org.readium.r2.shared.util.http.HttpRequest
import org.readium.r2.shared.util.http.fetchWithDecoder
import org.cis_india.wsreader.utils.tryOrLog

class CoverStorage(
    private val appStorageDir: File,
    private val httpClient: HttpClient,
) {

    suspend fun storeCover(publication: Publication, overrideUrl: AbsoluteUrl?): Try<File, Exception> {
        val coverBitmap: Bitmap? = overrideUrl?.fetchBitmap()
            ?: publication.cover()
        return try {
            Try.success(storeCover(coverBitmap))
        } catch (e: Exception) {
            Try.failure(e)
        }
    }

    private suspend fun AbsoluteUrl.fetchBitmap(): Bitmap? =
        tryOrLog {
            when {
                isFile -> toFile()?.toBitmap()
                isHttp -> httpClient.fetchBitmap(HttpRequest(this)).getOrNull()
                else -> null
            }
        }

    private suspend fun File.toBitmap(): Bitmap? =
        withContext(Dispatchers.IO) {
            tryOrLog {
                val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                BitmapFactory.decodeFile(path, options)
                options.inSampleSize = calculateInSampleSize(options.outWidth, options.outHeight)
                options.inJustDecodeBounds = false
                BitmapFactory.decodeFile(path, options)
            }
        }

    private suspend fun HttpClient.fetchBitmap(request: HttpRequest): Try<Bitmap, HttpError> =
        fetchWithDecoder(request) { response ->
            val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeByteArray(response.body, 0, response.body.size, options)
            options.inSampleSize = calculateInSampleSize(options.outWidth, options.outHeight)
            options.inJustDecodeBounds = false
            BitmapFactory.decodeByteArray(response.body, 0, response.body.size, options)
        }

    private suspend fun storeCover(cover: Bitmap?): File =
        withContext(Dispatchers.IO) {
            val coverImageFile = File(coverDir(), "${UUID.randomUUID()}.png")
            val resized = cover?.let { Bitmap.createScaledBitmap(it, COVER_WIDTH, COVER_HEIGHT, true) }
            val fos = FileOutputStream(coverImageFile)
            resized?.compress(Bitmap.CompressFormat.PNG, 80, fos)
            fos.flush()
            fos.close()
            coverImageFile
        }

    private fun coverDir(): File =
        File(appStorageDir, "covers/")
            .apply { if (!exists()) mkdirs() }

    companion object {
        private const val COVER_WIDTH = 120
        private const val COVER_HEIGHT = 200

        /**
         * Returns the largest power-of-two sample size that keeps the decoded image
         * at least [reqWidth] x [reqHeight], so it can still be scaled down to the
         * cover size without losing quality.
         */
        internal fun calculateInSampleSize(
            width: Int,
            height: Int,
            reqWidth: Int = COVER_WIDTH,
            reqHeight: Int = COVER_HEIGHT,
        ): Int {
            var inSampleSize = 1
            if (height > reqHeight || width > reqWidth) {
                val halfHeight = height / 2
                val halfWidth = width / 2
                while (halfHeight / inSampleSize >= reqHeight && halfWidth / inSampleSize >= reqWidth) {
                    inSampleSize *= 2
                }
            }
            return inSampleSize
        }
    }
}
