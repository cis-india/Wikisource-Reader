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

package org.cis_india.wsreader.ui.screens.categories.composables

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoStories
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.LibraryBooks
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.TheaterComedy
import androidx.compose.material.icons.filled.Translate
import androidx.compose.ui.graphics.vector.ImageVector
import org.cis_india.wsreader.R

/**
 * Categories for the books.
 * @param category: Category name
 * @param nameRes: Resource ID for the category name
 */
sealed class BookCategories(val category: String, val nameRes: Int, val iconRes: ImageVector) {

    companion object {
        /**
         * All categories.
         */
        val ALL = arrayOf(
            Novels,
            Plays,
            Poems,
            ShortStories,
            Narration,
            Essays,
            Dictionaries,
            Encyclopedias,
        )

        /**
         * Get the name resource for the category.
         * @param category: Category name
         * @return Resource ID for the category name
         */
        fun getNameRes(category: String): Int {
            return when (category) {
                "novel" -> R.string.category_novels
                "poem" -> R.string.category_poetry
                "play" -> R.string.category_plays
                "short story" -> R.string.category_shortstories
                "narration" -> R.string.category_narration
                "essay" -> R.string.category_essays
                "dictionary" -> R.string.category_dictionaries
                "encyclopedia" -> R.string.category_encyclopedias
                else -> R.string.category_biography
            }
        }
    }

    data object Novels : BookCategories("novel", R.string.category_novels, Icons.Filled.Book)
    data object Poems : BookCategories("poem", R.string.category_poetry, Icons.Filled.MusicNote)
    data object Plays : BookCategories("play", R.string.category_plays, Icons.Filled.TheaterComedy)
    data object ShortStories : BookCategories("short story", R.string.category_shortstories, Icons.Filled.AutoStories)
    data object Narration : BookCategories("narration", R.string.category_narration,  Icons.Filled.MenuBook)
    data object Essays : BookCategories("essay", R.string.category_essays, Icons.Filled.Description)
    data object Dictionaries : BookCategories("dictionary", R.string.category_dictionaries, Icons.Filled.Translate)
    data object Encyclopedias : BookCategories("encyclopedia", R.string.category_encyclopedias, Icons.Filled.LibraryBooks)
}
