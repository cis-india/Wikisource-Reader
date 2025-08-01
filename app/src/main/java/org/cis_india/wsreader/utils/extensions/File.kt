/* Module: r2-testapp-kotlin
* Developers: Quentin Gliosca, Aferdita Muriqi, Clément Baumann
*
* Copyright (c) 2020. European Digital Reading Lab. All rights reserved.
* Licensed to the Readium Foundation under one or more contributor license agreements.
* Use of this source code is governed by a BSD-style license which is detailed in the
* LICENSE file present in the project repository where this source code is maintained.
*/

package org.cis_india.wsreader.utils.extensions

import java.io.File
import java.io.FileFilter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

suspend fun File.moveTo(target: File) = withContext(Dispatchers.IO) {
    if (this@moveTo.renameTo(target)) {
        return@withContext
    }

    // renameTo might be unable to move a file from a filesystem to another. Copy instead.
    copyTo(target)
    delete()
}

/**
 * As there are cases where [File.listFiles] returns null even though it is a directory, we return
 * an empty list instead.
 */
fun File.listFilesSafely(filter: FileFilter? = null): List<File> {
    val array: Array<File>? = if (filter == null) listFiles() else listFiles(filter)
    return array?.toList() ?: emptyList()
}
