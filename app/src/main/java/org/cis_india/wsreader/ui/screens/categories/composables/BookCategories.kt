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

import org.cis_india.wsreader.R

/**
 * Categories for the books.
 * @param category: Category name
 * @param nameRes: Resource ID for the category name
 */
sealed class BookCategories(val category: String, val nameRes: Int) {

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

    data object Novels : BookCategories("novel", R.string.category_novels)
    data object Poems : BookCategories("poem", R.string.category_poetry)
    data object Plays : BookCategories("play", R.string.category_plays)
    data object ShortStories : BookCategories("short story", R.string.category_shortstories)
    data object Narration : BookCategories("narration", R.string.category_narration)
    data object Essays : BookCategories("essays", R.string.category_essays)
    data object Dictionaries : BookCategories("biography", R.string.category_dictionaries)
    data object Encyclopedias : BookCategories("novella", R.string.category_encyclopedias)
}
