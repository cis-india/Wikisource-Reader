/*
 * Copyright 2022 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.cis_india.wsreader.utils.extensions

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.annotation.ColorInt
import androidx.annotation.ColorRes
import androidx.annotation.StringRes
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import kotlin.coroutines.resume
import kotlinx.coroutines.suspendCancellableCoroutine
import org.cis_india.wsreader.R
import java.io.File
import java.net.URLDecoder

@ColorInt
fun Context.color(@ColorRes id: Int): Int {
    return ContextCompat.getColor(this, id)
}

/**
 * Displays a confirmation [AlertDialog] and returns the user choice.
 */
suspend fun Context.confirmDialog(
    message: String,
    @StringRes positiveButton: Int = R.string.ok,
    @StringRes negativeButton: Int = R.string.cancel,
): Boolean =
    suspendCancellableCoroutine { cont ->
        AlertDialog.Builder(this)
            .setMessage(message)
            .setPositiveButton(getString(positiveButton)) { dialog, _ ->
                dialog.dismiss()
                cont.resume(true)
            }
            .setNegativeButton(getString(negativeButton)) { dialog, _ ->
                dialog.cancel()
            }
            .setOnCancelListener {
                cont.resume(false)
            }
            .show()
    }

/**
 * Shares an EPUB file to external apps.
 */
fun Context.shareEpub(href: String, mediaType: String) {
    try {
        val rawPath = href.removePrefix("file://")

        // Decode any URL-encoded characters (like %2B for '+')
        val decodedPath = URLDecoder.decode(rawPath, "UTF-8")

        val epubFile = File(decodedPath)
        if (!epubFile.exists()) {
            Toast.makeText(this, "File not found", Toast.LENGTH_SHORT).show()
            return
        }

        val fileUri: Uri = FileProvider.getUriForFile(
            this,
            "$packageName.provider",
            epubFile
        )

        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = mediaType
            putExtra(Intent.EXTRA_STREAM, fileUri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }

        startActivity(Intent.createChooser(shareIntent, "Share book via"))

    } catch (e: Exception) {
        e.printStackTrace()
        Toast.makeText(this, "Unable to share this file", Toast.LENGTH_SHORT).show()
    }
}
