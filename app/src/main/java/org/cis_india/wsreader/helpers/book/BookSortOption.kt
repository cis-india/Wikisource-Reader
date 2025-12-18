package org.cis_india.wsreader.helpers.book

import androidx.annotation.Keep
import androidx.annotation.StringRes
import org.cis_india.wsreader.R

@Keep
sealed class BookSortOption(@StringRes val displayName: Int, val apiValue: String) {

    companion object {
        fun getAllSortOptions() =
            BookSortOption::class.sealedSubclasses.mapNotNull { it.objectInstance }
    }

    @Keep
    data object PopularityHighToLow : BookSortOption(R.string.high_to_low, "-view_count")

    @Keep
    data object PopularityLowToHigh : BookSortOption(R.string.low_to_high, "view_count")

    @Keep
    data object TitleAToZ : BookSortOption(R.string.a_to_z, "title")

    @Keep
    data object TitleZToA : BookSortOption(R.string.z_to_a, "-title")

    @Keep
    data object Random : BookSortOption(R.string.random, "random")
}
