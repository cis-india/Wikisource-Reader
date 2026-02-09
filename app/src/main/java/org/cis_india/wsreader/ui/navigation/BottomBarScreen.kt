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

package org.cis_india.wsreader.ui.navigation

import org.cis_india.wsreader.R

sealed class BottomBarScreen(
    val route: String,
    val title: Int,
    val icon: Int,
    val tap_target_coodinator_title: Int,
    val tap_target_coodinator_description: Int,
) {
    data object Home : BottomBarScreen(
        route = "home",
        title = R.string.navigation_home,
        icon = R.drawable.ic_nav_home,
        tap_target_coodinator_title = R.string.home_tap_target_coodinator_title,
        tap_target_coodinator_description =  R.string.home_tap_target_coodinator_description
    )

    data object Categories : BottomBarScreen(
        route = "categories",
        title = R.string.navigation_categories,
        icon = R.drawable.ic_nav_categories,
        tap_target_coodinator_title = R.string.category_tap_target_coodinator_title,
        tap_target_coodinator_description =  R.string.category_tap_target_coodinator_description
    )

    data object Library : BottomBarScreen(
        route = "library",
        title = R.string.navigation_library,
        icon = R.drawable.ic_nav_library,
        tap_target_coodinator_title = R.string.library_tap_target_coodinator_title,
        tap_target_coodinator_description =  R.string.library_tap_target_coodinator_description
    )

    data object Settings : BottomBarScreen(
        route = "settings",
        title = R.string.navigation_settings,
        icon = R.drawable.ic_nav_settings,
        tap_target_coodinator_title = R.string.settings_tap_target_coodinator_title,
        tap_target_coodinator_description =  R.string.settings_tap_target_coodinator_description
    )
}