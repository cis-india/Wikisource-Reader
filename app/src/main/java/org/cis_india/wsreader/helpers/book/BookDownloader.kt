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

package org.cis_india.wsreader.helpers.book

import android.annotation.SuppressLint
import android.app.DownloadManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.database.Cursor
import android.net.Uri
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import org.cis_india.wsreader.MainActivity
import org.cis_india.wsreader.MainViewModel
import org.cis_india.wsreader.R
import org.cis_india.wsreader.api.models.Book
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import java.io.File
import java.util.UUID


/**
 * Class to handle downloading of books.
 * @param context [Context] required to access [DownloadManager] and to get file path for the downloaded file.
 */
class BookDownloader(private val context: Context) {

    companion object {
        private const val TAG = "BookDownloader"
        const val BOOKS_FOLDER = "ebooks"
        const val TEMP_FOLDER = "temp_books"
        private const val MAX_FILENAME_LENGTH = 200
        private const val NOTIFICATION_CHANNEL_ID = "book_download_channel"
        private const val NOTIFICATION_CHANNEL_NAME = "Book Downloads"
        private const val NOTIFICATION_GROUP_KEY = "book_downloads_group"
        private const val SUMMARY_NOTIFICATION_ID = 0

        /**
         * Sanitizes book title by replacing forbidden chars which are not allowed
         * as the file name and builds file name for the epub file by joining all
         * of the words in the  book title at the end.
         * @param title title of the book for which file name is required.
         * @return [String] file name for the given book.
         */
        fun createFileName(title: String): String {
            val sanitizedTitle = title
                .replace(":", ";")
                .replace("\"", "")
                .replace("/", "-")
                .replace("\\", "-")
                .split(" ")
                .joinToString(separator = "+") { word ->
                    word.replace(Regex("[^\\p{ASCII}]"), "")
                }.take(MAX_FILENAME_LENGTH)
                    .trim()
                    .ifEmpty { UUID.randomUUID().toString() }
            return "$sanitizedTitle.epub"
        }
    }

    private val downloadJob = Job()
    private val downloadScope = CoroutineScope(Dispatchers.IO + downloadJob)
    private val downloadManager by lazy { context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager }
    private val notificationManager by lazy { context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager }

    init {
        createNotificationChannel()
    }

    /**
     * Creates notification channel for book download notifications (required for Android O+)
     */
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                NOTIFICATION_CHANNEL_ID,
                NOTIFICATION_CHANNEL_NAME,
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Notifications for book download completion"
            }
            notificationManager.createNotificationChannel(channel)
        }
    }

    /**
     * Data class to store download info for a book.
     * @param downloadId id of the download.
     * @param status status of the download.
     * @param progress progress of the download.
     */
    data class DownloadInfo(
        val downloadId: Long,
        var status: Int = DownloadManager.STATUS_RUNNING,
        val progress: MutableStateFlow<Float> = MutableStateFlow(0f)
    )

    /** Stores running download with book id as key */
    private val runningDownloads = HashMap<Int, DownloadInfo>()

    /**
     * Start downloading epub file for the given [Book] object.
     * @param book [Book] which needs to be downloaded.
     * @param downloadProgressListener a callable which takes download progress; [Float] and
     * download status; [Int] as arguments.
     * @param onDownloadSuccess: a callable which will be executed after download has been
     * completed successfully.
     */
    @SuppressLint("Range")
    fun downloadBook(
        book: Book, downloadProgressListener: (progress: Float, status: Int) -> Unit,
        onDownloadSuccess: (filePath: String) -> Unit
    ) {
        // Check if book is already being downloaded.
        if (runningDownloads.containsKey(book.id)) return

        // Create file for the downloaded book.
        val filename = createFileName(book.title)
        val tempFolder = File(context.getExternalFilesDir(null), TEMP_FOLDER)
        if (!tempFolder.exists()) tempFolder.mkdirs()
        val tempFile = File(tempFolder, filename)
        Log.d(TAG, "downloadBook: Destination file path: ${tempFile.absolutePath}")

        // Start download...
        val downloadUri = Uri.parse(book.epubUrl)
        val request = DownloadManager.Request(downloadUri)

        // Use native language title if available, otherwise use English title, otherwise use a generic message
        val notificationTitle = book.titleNativeLanguage?.ifBlank { null }
            ?: book.title.ifBlank { context.getString(R.string.downloading_book) }

        request.setTitle(notificationTitle)
            .setDescription(context.getString(R.string.downloading))
            .setDestinationUri(Uri.fromFile(tempFile))
            .setAllowedOverRoaming(true)
            .setAllowedOverMetered(true)
            // Show download progress notification but hide completion notification
            // (we'll show our own custom notification with Library intent)
            .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE)

        Log.d(TAG, "downloadBook: Starting download for book: ${book.title}")
        val downloadId = downloadManager.enqueue(request)

        // Start coroutine to listen for download progress.
        downloadScope.launch {
            var isDownloadFinished = false
            var progress = 0f
            var status: Int
            runningDownloads[book.id] = DownloadInfo(downloadId)

            while (!isDownloadFinished) {
                val cursor: Cursor =
                    downloadManager.query(DownloadManager.Query().setFilterById(downloadId))
                if (cursor.moveToFirst()) {
                    status = cursor.getInt(cursor.getColumnIndex(DownloadManager.COLUMN_STATUS))
                    when (status) {
                        DownloadManager.STATUS_RUNNING -> {
                            val totalBytes: Long =
                                cursor.getLong(cursor.getColumnIndex(DownloadManager.COLUMN_TOTAL_SIZE_BYTES))
                            if (totalBytes > 0) {
                                val downloadedBytes: Long =
                                    cursor.getLong(cursor.getColumnIndex(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR))
                                progress = (downloadedBytes * 100 / totalBytes).toFloat() / 100
                            }
                        }

                        DownloadManager.STATUS_SUCCESSFUL -> {
                            Log.d(TAG, "downloadBook: Download successful for book: ${book.title}")
                            isDownloadFinished = true
                            progress = 1f
                            // Move file to books folder.
                            val booksFolder = File(context.filesDir, BOOKS_FOLDER)
                            if (!booksFolder.exists()) booksFolder.mkdirs()
                            val bookFile = File(booksFolder, filename)
                            tempFile.copyTo(bookFile, true)
                            tempFile.delete()
                            // Show custom notification with Library intent
                            // Prefer native language title, fallback to English title
                            val title = book.titleNativeLanguage?.ifBlank { null } ?: book.title
                            showDownloadCompleteNotification(title, book.id)
                            onDownloadSuccess(bookFile.absolutePath)
                        }

                        DownloadManager.STATUS_PAUSED, DownloadManager.STATUS_PENDING -> {
                            // Do nothing, wait for download to resume.
                            Log.d(TAG, "downloadBook: Download pending for book: ${book.title}")
                        }

                        DownloadManager.STATUS_FAILED -> {
                            Log.d(TAG, "downloadBook: Download failed for book: ${book.title}")
                            isDownloadFinished = true
                            progress = 0f
                        }
                    }

                } else {
                    /** Download cancelled by the user. */
                    Log.d(TAG, "downloadBook: Download cancelled for book: ${book.title}")
                    isDownloadFinished = true
                    progress = 0f
                    status = DownloadManager.STATUS_FAILED
                }

                /** update download info at the end of iteration. */
                runningDownloads[book.id]?.status = status
                downloadProgressListener(progress, status)
                runningDownloads[book.id]?.progress?.value = progress
                cursor.close()
            }
            /**
            Remove download from running downloads when loop ends.
            added dome delay here so we get time to update our UI before
            download info gets removed.
             */
            delay(500L)
            runningDownloads.remove(book.id)
        }
    }

    /**
     * Returns true if book with the given id is currently being downloaded
     * false otherwise.
     * @param bookId id of the book for which download status is required.
     * @return [Boolean] true if book is currently being downloaded, false otherwise.
     */
    fun isBookCurrentlyDownloading(bookId: Int) = runningDownloads.containsKey(bookId)

    /**
     * Returns [DownloadInfo] if book with the given id is currently being downloaded.
     * @param bookId id of the book for which download info is required.
     * @return [DownloadInfo] if book is currently being downloaded, null otherwise.
     */
    fun getRunningDownload(bookId: Int) = runningDownloads[bookId]

    /**
     * Cancels download of book by using it's download id (if download is running).
     * @param downloadId id of the download which needs to be cancelled.
     */
    fun cancelDownload(downloadId: Long?) = downloadId?.let { downloadManager.remove(it) }

    /**
     * Cancels all currently running downloads
     */
    fun cancelAllRunningDownloads(){
        if (runningDownloads.isNotEmpty()){

            for (key in runningDownloads.keys){

                val downloadInfo = runningDownloads.get(key)
                val downloadId = downloadInfo?.downloadId
                downloadId?.let { downloadManager.remove(it) }
            }
        }
    }

    /**
     * Shows a custom notification when download completes that opens the Library screen when tapped
     */
    private fun showDownloadCompleteNotification(bookTitle: String, bookId: Int) {
        // Create intent to open Library screen when notification is tapped
        val intent = Intent(context, MainActivity::class.java).apply {
            data = Uri.parse("${MainViewModel.LAUNCHER_SHORTCUT_SCHEME}://library")
            putExtra(MainViewModel.LC_SC_BOOK_LIBRARY, true)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            bookId, // Use bookId as request code to make each notification unique
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Use book title if available, otherwise use a generic message
        val displayTitle = bookTitle.ifBlank { context.getString(R.string.download_complete) }
        val displayText = if (bookTitle.isBlank()) "" else context.getString(R.string.download_complete)

        // Build and show notification
        val notification = NotificationCompat.Builder(context, NOTIFICATION_CHANNEL_ID)
            .setContentTitle(displayTitle)
            .setContentText(displayText)
            .setSmallIcon(android.R.drawable.stat_sys_download_done)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setGroup(NOTIFICATION_GROUP_KEY) // Group notifications together
            .build()

        notificationManager.notify(bookId, notification)

        // Show a summary notification for grouped downloads
        showSummaryNotification()
    }

    /**
     * Shows a summary notification when multiple downloads are grouped
     */
    private fun showSummaryNotification() {
        val intent = Intent(context, MainActivity::class.java).apply {
            data = Uri.parse("${MainViewModel.LAUNCHER_SHORTCUT_SCHEME}://library")
            putExtra(MainViewModel.LC_SC_BOOK_LIBRARY, true)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val summaryNotification = NotificationCompat.Builder(context, NOTIFICATION_CHANNEL_ID)
            .setContentTitle(context.getString(R.string.download_complete))
            .setContentText(context.getString(R.string.tap_to_view_library))
            .setSmallIcon(android.R.drawable.stat_sys_download_done)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setGroup(NOTIFICATION_GROUP_KEY)
            .setGroupSummary(true)
            .build()

        notificationManager.notify(SUMMARY_NOTIFICATION_ID, summaryNotification)
    }

}