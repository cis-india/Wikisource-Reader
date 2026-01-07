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

package org.cis_india.wsreader.ui.screens.detail.composables

import android.app.DownloadManager
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import org.cis_india.wsreader.MainActivity
import org.cis_india.wsreader.R
import org.cis_india.wsreader.data.model.Book
import org.cis_india.wsreader.helpers.Utils
import org.cis_india.wsreader.helpers.book.BookUtils
import org.cis_india.wsreader.helpers.getActivity
import org.cis_india.wsreader.helpers.weakHapticFeedback
import org.cis_india.wsreader.reader.ReaderActivityContract
import org.cis_india.wsreader.ui.common.BookDetailTopUI
import org.cis_india.wsreader.ui.common.NetworkError
import org.cis_india.wsreader.ui.common.ProgressDots
import org.cis_india.wsreader.ui.screens.detail.viewmodels.BookDetailViewModel
import org.cis_india.wsreader.ui.screens.library.viewmodels.LibraryViewModel
import org.cis_india.wsreader.ui.theme.pacificoFont
import org.cis_india.wsreader.ui.theme.poppinsFont
import java.util.Locale


@Composable
fun BookDetailScreen(
    bookId: String, navController: NavController, lifecycleOwner: LifecycleOwner = LocalLifecycleOwner.current
) {
    val context = LocalContext.current
    val viewModel: BookDetailViewModel = hiltViewModel()
    val libraryViewModel: LibraryViewModel = hiltViewModel()
    val state = viewModel.state

    LaunchedEffect(Unit) {
        viewModel.channel.receive(lifecycleOwner) { event ->
            handleEvent(event, context)
        }
    }

    val snackBarHostState = remember { SnackbarHostState() }
    Scaffold(
        snackbarHost = { SnackbarHost(snackBarHostState) },
        content = { paddingValues ->
            LaunchedEffect(key1 = true, block = {
                if (state.isLoading) viewModel.getBookDetails(bookId)
            })

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .background(MaterialTheme.colorScheme.background)
            ) {
                BookDetailTopBar(onBackClicked = {
                    navController.navigateUp()
                }, onShareClicked = {
                    val book = state.bookSet.books.find { it.id.toString() == bookId }!!
                    val shareText = "Check out \"${book.titleNativeLanguage}\" on Wikisource at ${book.wsUrl}"

                    val intent = Intent(Intent.ACTION_SEND).apply {
                        type = "text/plain"
                        putExtra(Intent.EXTRA_TEXT, shareText)
                    }
                    val chooser = Intent.createChooser(
                        intent, context.getString(R.string.share_intent_header)
                    )
                    context.startActivity(chooser)
                })

                Crossfade(
                    targetState = state.isLoading,
                    label = "BookDetailLoadingCrossFade"
                ) { isLoading ->
                    if (isLoading) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(bottom = 65.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            ProgressDots()
                        }
                    } else {
                        if (state.error != null) {
                            NetworkError(onRetryClicked = {
                                viewModel.getBookDetails(bookId)
                            })
                        } else {
                            BookDetailContents(
                                viewModel = viewModel,
                                navController = navController,
                                snackBarHostState = snackBarHostState,
                                libraryViewModel = libraryViewModel
                            )
                        }
                    }

                }
            }
        })
}

@Composable
private fun BookDetailContents(
    viewModel: BookDetailViewModel,
    libraryViewModel: LibraryViewModel,
    navController: NavController,
    snackBarHostState: SnackbarHostState
) {
    val view = LocalView.current
    val context = LocalContext.current
    val settingsVM = (context.getActivity() as MainActivity).settingsViewModel

    val bookItems = viewModel.allItems.observeAsState(listOf()).value
    val libraryItems = libraryViewModel.allItems.observeAsState(listOf()).value
    val state = viewModel.state
    val coroutineScope = rememberCoroutineScope()

    val book = remember { state.bookSet.books.first() }
    val bookDetailId = book.id

    // Check if the current book is in the library
    val foundBookInLibrary by remember(libraryItems) {
        mutableStateOf(libraryItems.firstOrNull { it.identifier.toIntOrNull() == bookDetailId })
    }

    Column(
        Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
    ) {
        val unknownAuthorString = stringResource(R.string.unknown_author_info)
        val firstLanguage = Locale.getDefault().language ?: "en"
        //val authors = remember { BookUtils.getAuthorsAsString(book.authors, firstLanguage) }
        var authors by remember { mutableStateOf("Loading...") }
        var publishers by remember { mutableStateOf("Loading...") }
        var placeOfPublication by remember { mutableStateOf("Loading...") }
        val unknownPlaceOfPublicationString = stringResource(id = R.string.unknown_place_of_publication_info)
        val unknownPublisherString = stringResource(id = R.string.unknown_publishers_info)


        LaunchedEffect(book.authors, firstLanguage) {
            val authorsString = BookUtils.getAuthorsAsString(book.authors, firstLanguage, unknownAuthorString)
            val publisherString = BookUtils.getPublishersAsString(book.publishers, firstLanguage, unknownPublisherString)
            val placeOfPublicationString = BookUtils.getPlacesOfPublicationAsString(book.place_of_publication, firstLanguage, unknownPlaceOfPublicationString)

            authors = authorsString // Update the authors state once the data is fetched
            publishers = publisherString
            placeOfPublication = placeOfPublicationString
        }

        var editors by remember { mutableStateOf<List<String>>(emptyList()) }
        LaunchedEffect(book.editors, firstLanguage) {
            editors = BookUtils.getEditors(book.editors, firstLanguage)
        }

        var translators by remember { mutableStateOf<List<String>>(emptyList()) }
        LaunchedEffect(book.translators, firstLanguage) {
            translators = BookUtils.getTranslators(book.translators, firstLanguage)
        }


        val genres = book.genre.filter { it.isNotBlank() }
        val subjects = book.subjects.filter { it.isNotBlank() }

        // Checks if there are values of publisher or places of publication
        val isPublishersBlank = book.publishers.all { it.name?.isBlank() ?: true }
        val isPlaceOfPublicationBlank = book.place_of_publication.all { it.name?.isBlank() ?: true }

        BookDetailTopUI(
            title = book.titleNativeLanguage ?: book.title,
            authors = authors,
            imageData = book.thumbnailUrl,
            currentThemeMode = settingsVM.getCurrentTheme()
        )
        /*
        val pageCount = remember {

            if (state.extraInfo.pageCount > 0) {
                state.extraInfo.pageCount.toString()
            } else {
                context.getString(R.string.not_applicable)
            }
        }

        val pageCount = remember {
            book.dateOfPublication?.substring(0, 4) ?: "NA"
        }
        */
        val pageCount = remember {
            val publicationYear = book.dateOfPublication?.substring(0, 4) ?: "NA"
            if (state.extraInfo.pageCount > 0) {
                state.extraInfo.pageCount.toString()
            } else {
                publicationYear
            }
        }



        var buttonText by remember { mutableStateOf("") }

        // Update button text based on download status.

        LaunchedEffect(foundBookInLibrary) {
            buttonText = if (foundBookInLibrary != null) {
                context.getString(R.string.read_book_button)
            } else if (viewModel.bookDownloader.isBookCurrentlyDownloading(book.id)) {
                context.getString(R.string.cancel)
            } else {
                when (state.bookLibraryItem) {
                    null -> context.getString(R.string.download_book_button)
                    else -> context.getString(R.string.read_book_button)
                }
            }
        }


        var progressState by remember { mutableFloatStateOf(0f) }
        var showProgressBar by remember { mutableStateOf(false) }

        // Callable which updates book details screen button.

        val updateBtnText: (Int?) -> Unit = { downloadStatus ->
            buttonText = when (downloadStatus) {
                DownloadManager.STATUS_RUNNING -> {
                    showProgressBar = true
                    context.getString(R.string.cancel)
                }

                DownloadManager.STATUS_SUCCESSFUL -> {
                    showProgressBar = false
                    context.getString(R.string.read_book_button)
                }

                else -> {
                    showProgressBar = false
                    context.getString(R.string.download_book_button)
                }
            }
        }
        //buttonText = context.getString(R.string.download_book_button)

        // Check if this book is in downloadQueue.
        /*
        if (viewModel.bookDownloader.isBookCurrentlyDownloading(book.id)) {
            progressState =
                viewModel.bookDownloader.getRunningDownload(book.id)?.progress?.collectAsState()?.value!!
            LaunchedEffect(key1 = progressState, block = {
                updateBtnText(viewModel.bookDownloader.getRunningDownload(book.id)?.status)
            })
        }
        */

        MiddleBar(
            bookLang = BookUtils.getLanguagesAsString(book.languages),
            pageCount = pageCount,
            viewCount = Utils.prettyCount(book.viewCount),
            progressValue = progressState,
            buttonText = buttonText,
            showProgressBar = showProgressBar
        ) {
            when (buttonText) {
                /*
                //Takes to old reader, need to update, meanwhile, using Go To Library button
                context.getString(R.string.read_book_button) -> {
                    /**
                     *  Library item could be null if we reload the screen
                     *  while some download was running, in that case we'll
                     *  de-attach from our old state where download function
                     *  will update library item and our new state will have
                     *  no library item, i.e. null.
                     */
                    if (state.bookLibraryItem == null) {
                        viewModel.reFetchLibraryItem(
                            bookId = book.id,
                            onComplete = { libraryItem ->
                                BookUtils.openBookFile(
                                    context = context,
                                    internalReader = viewModel.getInternalReaderSetting(),
                                    libraryItem = libraryItem,
                                    navController = navController
                                )
                            }
                        )
                    } else {
                        BookUtils.openBookFile(
                            context = context,
                            internalReader = viewModel.getInternalReaderSetting(),
                            libraryItem = state.bookLibraryItem,
                            navController = navController
                        )
                    }
                }
                 */

                context.getString(R.string.read_book_button) -> {
                    view.weakHapticFeedback()

                    // Find the corresponding LibraryItem using the bookDetailId
                    val currentBook = foundBookInLibrary ?: libraryItems.firstOrNull { it.identifier.toIntOrNull() == bookDetailId }

                    if (currentBook != null) {
                        val bookItemId: Long? = currentBook.id

                        if (bookItemId != null) {
                            viewModel.openPublication(bookItemId)
                        }
                    }
                }

                context.getString(R.string.download_book_button) -> {
                    view.weakHapticFeedback()

                    viewModel.bookDownloader.cancelAllRunningDownloads()

                    viewModel.downloadBook(
                        book = book,
                        downloadProgressListener = { downloadProgress, downloadStatus ->
                            progressState = downloadProgress
                            updateBtnText(downloadStatus)
                        })
                    //viewModel.downloadBook(book.epubUrl)
                    /*
                   val epubUrl = book.epubUrl
                   if (epubUrl != null) {
                       viewModel.addPublicationFromWeb(epubUrl)
                   } else {
                       //show an error and fallback
                   }
                   coroutineScope.launch {
                       snackBarHostState.showSnackbar(
                           message = context.getString(R.string.download_started),
                       )
                   }*/
                }
                context.getString(R.string.cancel) -> {
                    viewModel.bookDownloader.cancelDownload(
                        viewModel.bookDownloader.getRunningDownload(book.id)?.downloadId
                    )
                }

            }
        }
        /*
        Text(
            text = stringResource(id = R.string.book_synopsis),
            modifier = Modifier.padding(start = 13.dp, end = 8.dp),
            fontSize = 18.sp,
            fontFamily = poppinsFont,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onBackground,
        )*/

        val synopsis = state.extraInfo.description.ifEmpty { null }
        if (synopsis != null) {
            /*
            Text(
                text = synopsis,
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                fontFamily = poppinsFont,
                fontWeight = FontWeight.Normal,
                color = MaterialTheme.colorScheme.onBackground,
            )
            */
        } else {
            Column {
                InfoLine(stringResource(id = R.string.editors_info), editors)
                InfoLine(stringResource(R.string.translators_info), translators)
                InfoLine(stringResource(R.string.genres_info), genres)
                InfoLine(stringResource(R.string.subjects_info), subjects)
                InfoStringContent(stringResource(R.string.publishers_info), publishers, isPublishersBlank)
                InfoStringContent(stringResource(R.string.place_of_publication_info),placeOfPublication, isPlaceOfPublicationBlank)
            }
        }
    }
}

@Composable
private fun MiddleBar(
    bookLang: String,
    pageCount: String,
    viewCount: String,
    progressValue: Float,
    buttonText: String,
    showProgressBar: Boolean,
    onButtonClick: () -> Unit
) {
    val progress by animateFloatAsState(
        targetValue = progressValue,
        label = "download progress bar"
    )
    Column(modifier = Modifier.fillMaxWidth()) {
        AnimatedVisibility(visible = showProgressBar) {
            if (progressValue > 0f) {
                // Determinate progress bar.
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(16.dp)
                        .padding(start = 14.dp, end = 14.dp, top = 6.dp)
                        .clip(RoundedCornerShape(40.dp)),
                    color = MaterialTheme.colorScheme.secondary,
                )
            } else {
                // Indeterminate progress bar.
                LinearProgressIndicator(
                    color = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(16.dp)
                        .padding(start = 14.dp, end = 14.dp, top = 6.dp)
                        .clip(RoundedCornerShape(40.dp))
                )
            }
        }

        Card(
            modifier = Modifier
                .height(90.dp)
                .fillMaxWidth()
                .padding(start = 12.dp, end = 12.dp, top = 12.dp, bottom = 6.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(
                    2.dp
                )
            )
        ) {
            Row(modifier = Modifier.fillMaxSize(), verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Row {
                        Icon(
                            imageVector = ImageVector.vectorResource(id = R.drawable.ic_book_language),
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onBackground,
                            modifier = Modifier.padding(top = 14.dp, bottom = 14.dp, end = 4.dp)
                        )
                        Text(
                            text = bookLang,
                            modifier = Modifier.padding(top = 14.dp, bottom = 14.dp, start = 4.dp),
                            fontSize = 16.sp,
                            fontFamily = poppinsFont,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onBackground,
                        )
                    }

                }
                VerticalDivider(
                    modifier = Modifier
                        .fillMaxHeight(0.6f)
                        .width(2.dp)
                )
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Row {
                        Icon(
                            imageVector = ImageVector.vectorResource(id = R.drawable.ic_published_year),
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onBackground,
                            modifier = Modifier.padding(top = 13.dp, bottom = 15.dp, end = 4.dp)
                        )
                        Text(
                            text = pageCount,
                            modifier = Modifier.padding(top = 14.dp, bottom = 14.dp, start = 4.dp),
                            fontSize = 16.sp,
                            fontFamily = poppinsFont,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onBackground,
                        )
                    }
                }
                VerticalDivider(
                    modifier = Modifier
                        .fillMaxHeight(0.6f)
                        .width(2.dp)
                )
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Row {
                        Icon(
                            imageVector = ImageVector.vectorResource(id = R.drawable.ic_book_views),
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onBackground,
                            modifier = Modifier.padding(top = 15.dp, bottom = 13.dp, end = 4.dp)
                        )
                        Text(
                            text = viewCount,
                            modifier = Modifier.padding(top = 14.dp, bottom = 14.dp, start = 4.dp),
                            fontSize = 16.sp,
                            fontFamily = poppinsFont,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onBackground,
                        )
                    }
                }
            }
        }

        Card(
            onClick = { onButtonClick() },
            modifier = Modifier
                .height(75.dp)
                .fillMaxWidth()
                .padding(start = 12.dp, end = 12.dp, top = 6.dp, bottom = 12.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer
            ),
            shape = RoundedCornerShape(16.dp)
        ) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    text = buttonText,
                    fontSize = 17.sp,
                    fontFamily = poppinsFont,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                )
            }
        }
    }
}

@Composable
private fun BookDetailTopBar(
    onBackClicked: () -> Unit, onShareClicked: () -> Unit
) {
    val view = LocalView.current
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .padding(horizontal = 22.dp, vertical = 16.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceColorAtElevation(4.dp))
                .clickable {
                    view.weakHapticFeedback()
                    onBackClicked()
                }
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                contentDescription = stringResource(id = R.string.back_button_desc),
                tint = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(14.dp)
            )
        }

        Spacer(modifier = Modifier.weight(1f))

        Text(
            text = stringResource(id = R.string.book_detail_header),
            modifier = Modifier.padding(bottom = 2.dp),
            color = MaterialTheme.colorScheme.onBackground,
            fontStyle = MaterialTheme.typography.headlineMedium.fontStyle,
            fontFamily = pacificoFont,
            fontSize = 22.sp
        )

        Spacer(modifier = Modifier.weight(1f))

        Box(modifier = Modifier
            .padding(22.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.surfaceColorAtElevation(4.dp))
            .clickable {
                view.weakHapticFeedback()
                onShareClicked()
            }) {
            Icon(
                imageVector = Icons.Outlined.Share,
                contentDescription = stringResource(id = R.string.back_button_desc),
                tint = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(14.dp)
            )
        }
    }
}

/*
@Composable
private fun NoSynopsisUI() {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        val compositionResult: LottieCompositionResult =
            rememberLottieComposition(
                spec = LottieCompositionSpec.RawRes(R.raw.synopis_not_found_lottie)
            )
        val progressAnimation by animateLottieCompositionAsState(
            compositionResult.value,
            isPlaying = true,
            iterations = LottieConstants.IterateForever,
            speed = 1f
        )

        Spacer(modifier = Modifier.weight(2f))
        LottieAnimation(
            composition = compositionResult.value,
            progress = { progressAnimation },
            modifier = Modifier
                .fillMaxWidth(0.85f)
                .height(200.dp),
            enableMergePaths = true
        )

        Text(
            text = stringResource(id = R.string.book_synopsis_not_found),
            modifier = Modifier.padding(14.dp),
            fontFamily = poppinsFont,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onBackground,
        )
        Spacer(modifier = Modifier.weight(1f))
    }
}
*/

@Composable
fun InfoLine(label: String, values: List<String>) {
    val filtered = values.filter { it.isNotBlank() }
    if (filtered.isNotEmpty()) {
        // Decide label based on count
        val labelToShow = if (filtered.size > 1) label else label.removeSuffix("s")
        Text(
            text = "$labelToShow: ${filtered.joinToString(", ")}",
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 4.dp),
            fontFamily = poppinsFont,
            fontWeight = FontWeight.Normal,
            color = MaterialTheme.colorScheme.onBackground,
        )
    }
}
@Composable
fun InfoStringContent(label: String, value: String?, isValueBlank: Boolean ) {
    if (!isValueBlank)  {
        Text(
            text = "$label: $value",
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 4.dp),
            fontFamily = poppinsFont,
            fontWeight = FontWeight.Normal,
            color = MaterialTheme.colorScheme.onBackground,
        )
    }
}

private fun handleEvent(event: BookDetailViewModel.Event, context: Context) {
    when (event) {
        is BookDetailViewModel.Event.OpenPublicationError -> {
            Toast.makeText(context, event.error.message ?: "Error opening publication", Toast.LENGTH_SHORT).show()
        }

        is BookDetailViewModel.Event.LaunchReader -> {
            val intent = ReaderActivityContract().createIntent(
                context,
                event.arguments
            )
            context.startActivity(intent)
        }
    }
}

@Composable
@Preview(showBackground = true)
fun BookDetailScreenPreview() {
    BookDetailScreen(
        bookId = "0",
        navController = rememberNavController(),
    )
}