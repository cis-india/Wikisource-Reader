package org.cis_india.wsreader.helpers.book

import androidx.annotation.Keep

@Keep
sealed class BookSortOption(val displayName: String, val apiValue: String) {

    companion object {
        fun getAllSortOptions() =
            BookSortOption::class.sealedSubclasses.mapNotNull { it.objectInstance }
    }

    @Keep
    data object PopularityHighToLow : BookSortOption("Popularity: High to Low", "-view_count")

    @Keep
    data object PopularityLowToHigh : BookSortOption("Popularity: Low to High", "view_count")

    @Keep
    data object TitleAToZ : BookSortOption("Title: A to Z", "title")

    @Keep
    data object TitleZToA : BookSortOption("Title: Z to A", "-title")

    @Keep
    data object Random : BookSortOption("Randomised Sort", "random")
}
