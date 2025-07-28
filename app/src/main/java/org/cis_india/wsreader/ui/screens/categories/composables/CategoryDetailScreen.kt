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

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import org.cis_india.wsreader.R
import org.cis_india.wsreader.helpers.NetworkObserver
import org.cis_india.wsreader.helpers.book.BookUtils
import org.cis_india.wsreader.ui.common.BookItemCard
import org.cis_india.wsreader.ui.common.BookItemShimmerLoader
import org.cis_india.wsreader.ui.common.BookLanguageSheet
import org.cis_india.wsreader.ui.common.CustomTopAppBar
import org.cis_india.wsreader.ui.common.NetworkError
import org.cis_india.wsreader.ui.common.NoBooksAvailable
import org.cis_india.wsreader.ui.common.ProgressDots
import org.cis_india.wsreader.ui.navigation.Screens
import org.cis_india.wsreader.ui.screens.categories.viewmodels.CategoryViewModel
import java.util.Locale


@Composable
fun CategoryDetailScreen(
    category: String, navController: NavController, networkStatus: NetworkObserver.Status
) {
    val viewModel: CategoryViewModel = hiltViewModel()
    val showLanguageSheet = remember { mutableStateOf(false) }

    BookLanguageSheet(
        showBookLanguage = showLanguageSheet,
        selectedLanguage = viewModel.language.value,
        onLanguageChange = { viewModel.changeLanguage(it) }
    )

    val state = viewModel.state
    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            CustomTopAppBar(headerText = stringResource(id = BookCategories.getNameRes(category)),
                actionIcon = Icons.Filled.Translate,
                onBackButtonClicked = { navController.navigateUp() },
                onActionClicked = { showLanguageSheet.value = true }
            )
        }, content = {
            LaunchedEffect(key1 = true, block = { viewModel.loadBookByCategory(category) })

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.background)
                    .padding(it)
            ) {
                AnimatedVisibility(
                    visible = state.page == 1L && state.isLoading,
                    enter = fadeIn(),
                    exit = fadeOut()
                ) {
                    BookItemShimmerLoader()
                }
                AnimatedVisibility(
                    visible = !state.isLoading && state.error != null,
                    enter = fadeIn(),
                    exit = fadeOut()
                ) {
                    NetworkError(onRetryClicked = { viewModel.reloadItems() })
                }
                AnimatedVisibility(
                    visible = !state.isLoading && state.items.isEmpty() && state.error == null,
                    enter = fadeIn(),
                    exit = fadeOut()
                ) {
                    NoBooksAvailable(
                        text = stringResource(id = R.string.no_books_found_for_lang_and_cat)
                            .format(viewModel.language.value.name.lowercase(Locale.getDefault()))
                    )
                }
                AnimatedVisibility(
                    visible = !state.isLoading || (state.items.isNotEmpty() && state.error == null),
                    enter = fadeIn(),
                    exit = fadeOut()
                ) {
                    LazyVerticalGrid(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(MaterialTheme.colorScheme.background)
                            .padding(start = 8.dp, end = 8.dp),
                        columns = GridCells.Adaptive(295.dp)
                    ) {
                        items(state.items.size) { i ->
                            val item = state.items[i]
                            if (networkStatus == NetworkObserver.Status.Available && i >= state.items.size - 1 && !state.endReached && !state.isLoading) {
                                viewModel.loadNextItems()
                            }
                            Box(
                                modifier = Modifier
                                    .padding(4.dp)
                                    .fillMaxWidth(),
                                contentAlignment = Alignment.Center
                            ) {
                                val firstLanguage = item.languages.firstOrNull() ?: "en"
                                //val authors = remember { BookUtils.getAuthorsAsString(book.authors, firstLanguage) }
                                var authors by remember { mutableStateOf("Loading...") }

                                LaunchedEffect(item.authors, firstLanguage) {
                                    val authorsString = BookUtils.getAuthorsAsString(item.authors, firstLanguage)
                                    authors = authorsString // Update the authors state once the data is fetched
                                }
                                BookItemCard(
                                    title = item.titleNativeLanguage?: item.title,
                                    author = authors,
                                    language = BookUtils.getLanguagesAsString(item.languages),
                                    /*subjects = BookUtils.getSubjectsAsString(item.subjects, 3),*/
                                    coverImageUrl = item.thumbnailUrl
                                ) {
                                    navController.navigate(Screens.BookDetailScreen.withBookId(item.id.toString()))
                                }
                            }
                        }
                        item {
                            if (state.isLoading) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(8.dp),
                                    horizontalArrangement = Arrangement.Center
                                ) {
                                    ProgressDots()
                                }
                            }
                        }
                    }
                }
            }

        })
}