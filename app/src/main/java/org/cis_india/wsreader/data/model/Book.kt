/*
 * Copyright 2021 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.cis_india.wsreader.data.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import org.readium.r2.shared.util.AbsoluteUrl
import org.readium.r2.shared.util.mediatype.MediaType
import java.text.DateFormat
import java.util.Date

@Entity(tableName = Book.TABLE_NAME)
data class Book(
    @PrimaryKey
    @ColumnInfo(name = ID)
    var id: Long? = null,
    @ColumnInfo(name = Bookmark.CREATION_DATE, defaultValue = "CURRENT_TIMESTAMP")
    val creation: Long? = null,
    @ColumnInfo(name = HREF)
    val href: String,
    @ColumnInfo(name = TITLE)
    val title: String,
    @ColumnInfo(name = AUTHOR)
    val author: String? = null,
    @ColumnInfo(name = IDENTIFIER)
    val identifier: String,
    @ColumnInfo(name = PROGRESSION)
    val progression: String? = null,
    @ColumnInfo(name = MEDIA_TYPE)
    val rawMediaType: String,
    @ColumnInfo(name = COVER)
    val cover: String,
    @ColumnInfo(name = LANGUAGE_CODE)
    val languageCode: String
) {

    constructor(
        id: Long? = null,
        creation: Long? = null,
        href: String,
        title: String,
        author: String? = null,
        identifier: String,
        progression: String? = null,
        mediaType: MediaType,
        cover: String,
        languageCode: String,
    ) : this(
        id = id,
        creation = creation,
        href = href,
        title = title,
        author = author,
        identifier = identifier,
        progression = progression,
        rawMediaType = mediaType.toString(),
        cover = cover,
        languageCode = languageCode
    )

    val url: AbsoluteUrl get() = AbsoluteUrl(href)!!

    val mediaType: MediaType get() =
        MediaType(rawMediaType)!!

    fun getDownloadDate(): String {
        val date = creation?.let { Date(it) }
        return DateFormat.getDateInstance().format(date)
    }

    companion object {

        const val TABLE_NAME = "books"
        const val ID = "id"
        const val CREATION_DATE = "creation_date"
        const val HREF = "href"
        const val TITLE = "title"
        const val AUTHOR = "author"
        const val IDENTIFIER = "identifier"
        const val PROGRESSION = "progression"
        const val MEDIA_TYPE = "media_type"
        const val COVER = "cover"
        const val LANGUAGE_CODE = "language_code"
    }
}
