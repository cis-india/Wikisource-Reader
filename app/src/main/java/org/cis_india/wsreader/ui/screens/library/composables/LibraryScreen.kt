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

package org.cis_india.wsreader.ui.screens.library.composables

import android.app.DownloadManager
import android.content.Context
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.navigation.NavController
import com.psoffritti.taptargetcompose.TapTargetCoordinator
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.cis_india.wsreader.MainActivity
import org.cis_india.wsreader.R
import org.cis_india.wsreader.data.model.Book
import org.cis_india.wsreader.helpers.getActivity
import org.cis_india.wsreader.reader.ReaderActivityContract
import org.cis_india.wsreader.ui.common.CustomTopAppBar
import org.cis_india.wsreader.ui.common.NoBooksAvailable
import org.cis_india.wsreader.ui.screens.detail.viewmodels.BookDetailViewModel
import org.cis_india.wsreader.ui.screens.library.viewmodels.LibraryViewModel
import org.cis_india.wsreader.ui.screens.main.bottomNavPadding
import org.cis_india.wsreader.ui.screens.settings.viewmodels.SettingsViewModel
import org.cis_india.wsreader.ui.theme.poppinsFont
import org.cis_india.wsreader.utils.extensions.shareEpub
import org.json.JSONObject


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LibraryScreen(navController: NavController, lifecycleOwner: LifecycleOwner = LocalLifecycleOwner.current) {
    val view = LocalView.current
    val context = LocalContext.current
    val viewModel: LibraryViewModel = hiltViewModel()
    val bookDetailsviewModel: BookDetailViewModel = hiltViewModel()

    val snackBarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()
    val lazyListState = rememberLazyListState()

    val showImportDialog = remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        viewModel.channel.receive(lifecycleOwner) { event ->
            handleEvent(event, context)
        }
    }

    val importBookLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.OpenMultipleDocuments()) { uris ->
            // If no files are selected, return.
            if (uris.isEmpty()) return@rememberLauncherForActivityResult
            // Show dialog to indicate import process.
            showImportDialog.value = true
            // Start books import.
            viewModel.importBooks(
                context = context,
                fileUris = uris,
                onComplete = {
                    // Hide dialog and show success message.
                    showImportDialog.value = false
                    coroutineScope.launch {
                        snackBarHostState.showSnackbar(
                            message = context.getString(R.string.epub_imported),
                            actionLabel = context.getString(R.string.ok),
                            duration = SnackbarDuration.Short
                        )
                    }
                },
                onError = {
                    // Hide dialog and show error message.
                    showImportDialog.value = false
                    coroutineScope.launch {
                        snackBarHostState.showSnackbar(
                            message = context.getString(R.string.error),
                            actionLabel = context.getString(R.string.ok),
                            duration = SnackbarDuration.Short
                        )
                    }
                }
            )
        }

    val showTapTargets = remember { mutableStateOf(false) }
    LaunchedEffect(key1 = viewModel.showOnboardingTapTargets.value) {
        delay(500) // Delay to prevent flickering
        showTapTargets.value = viewModel.showOnboardingTapTargets.value
    }


    TapTargetCoordinator(
        showTapTargets = showTapTargets.value,
        onComplete = { viewModel.onboardingComplete() }
    ) {
        Scaffold(
            snackbarHost = { SnackbarHost(snackBarHostState) },
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(bottom = bottomNavPadding),
            topBar = {
                CustomTopAppBar(
                    headerText = stringResource(id = R.string.library_header),
                    iconRes = R.drawable.ic_nav_library
                )
            }
        ) { paddingValues ->
            LibraryContents(
                viewModel = viewModel,
                bookDetailsviewModel = bookDetailsviewModel,
                lazyListState = lazyListState,
                snackBarHostState = snackBarHostState,
                navController = navController,
                paddingValues = paddingValues
            )

            if (showImportDialog.value) {
                BasicAlertDialog(onDismissRequest = {}) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth(),
                        //  .padding(12.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(5.dp)
                        ),
                        shape = RoundedCornerShape(18.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(44.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            CircularProgressIndicator()
                            Spacer(modifier = Modifier.width(24.dp))
                            Text(
                                text = stringResource(id = R.string.epub_importing),
                                fontFamily = poppinsFont,
                                fontWeight = FontWeight.Medium,
                                fontSize = 17.sp,
                            )
                        }
                    }
                }
            }

        }
    }
}

private fun handleEvent(event: LibraryViewModel.Event, context: Context) {
    when (event) {
        is LibraryViewModel.Event.OpenPublicationError -> {
            Toast.makeText(context, event.error.message ?: "Error opening publication", Toast.LENGTH_SHORT).show()
        }

        is LibraryViewModel.Event.LaunchReader -> {
            val intent = ReaderActivityContract().createIntent(
                context,
                event.arguments
            )
            context.startActivity(intent)
        }
    }
}
@Composable
private fun LibraryContents(
    viewModel: LibraryViewModel,
    bookDetailsviewModel: BookDetailViewModel,
    lazyListState: LazyListState,
    snackBarHostState: SnackbarHostState,
    navController: NavController,
    paddingValues: PaddingValues
) {
    val context = LocalContext.current
    val settingsVm = (context.getActivity() as MainActivity).settingsViewModel
    val libraryItems = viewModel.allItems.observeAsState(listOf()).value

    // Show tooltip for library screen. Enable in future after fixes
    /*
    LaunchedEffect(key1 = true) {
        if (viewModel.shouldShowLibraryTooltip()) {
            val result = snackBarHostState.showSnackbar(
                message = context.getString(R.string.library_tooltip),
                actionLabel = context.getString(R.string.got_it),
                duration = SnackbarDuration.Indefinite
            )

            when (result) {
                SnackbarResult.ActionPerformed -> {
                    viewModel.libraryTooltipDismissed()
                }

                SnackbarResult.Dismissed -> {}
            }
        }
    }
     */

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(paddingValues)
    ) {
        if (libraryItems.isEmpty() && viewModel.refreshingBookIds.isEmpty()) {
            NoBooksAvailable(text = stringResource(id = R.string.empty_library))
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.background),
                state = lazyListState
            ) {

                val databaseIds = libraryItems.map { it.identifier }.toSet()
                val pendingRefreshes = viewModel.refreshingBookIds.keys.filter { it !in databaseIds }
                // Show loading screen when a book is re-downloading
                items(
                    count = pendingRefreshes.size,
                    key = { i -> pendingRefreshes[i] }
                ) { i ->
                    val identifier = pendingRefreshes[i]
                    val status = viewModel.refreshingBookIds[identifier]
                    val progressValue = status?.progress ?: 0f

                    Column(modifier = Modifier.padding(14.dp)) {
                        AnimatedVisibility(
                            visible = true,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 10.dp, bottom = 4.dp)
                            ) {
                                Text(
                                    text = "Re-Downloading: ${status?.title ?: ""}",
                                    textAlign = TextAlign.Center,
                                    fontFamily = poppinsFont,
                                    fontWeight = FontWeight.Medium,
                                    fontSize = 13.sp,
                                    color = MaterialTheme.colorScheme.secondary,
                                    modifier = Modifier.fillMaxWidth().padding(horizontal = 18.dp)
                                )

                                if (progressValue > 0f) {
                                    // Determinate progress bar
                                    LinearProgressIndicator(
                                        progress = { progressValue },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(16.dp)
                                            .padding(start = 14.dp, end = 14.dp, top = 6.dp)
                                            .clip(RoundedCornerShape(40.dp)),
                                        color = MaterialTheme.colorScheme.secondary,
                                    )
                                } else {
                                    // Indeterminate progress bar (Starting up)
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
                        }
                    }
                }

                items(
                    count = libraryItems.size,
                    key = { i -> libraryItems[i].id ?: i }
                ) { i ->
                    val item = libraryItems[i]
                    LibraryLazyItem(
                        modifier = Modifier.animateItem(fadeInSpec = null, fadeOutSpec = null),
                        item = item,
                        snackBarHostState = snackBarHostState,
                        navController = navController,
                        viewModel = viewModel,
                        bookDetailsviewModel = bookDetailsviewModel,
                        settingsVm = settingsVm
                    )

                }
            }

        }
    }
}


@Composable
private fun LibraryLazyItem(
    modifier: Modifier,
    item: Book,
    snackBarHostState: SnackbarHostState,
    navController: NavController,
    viewModel: LibraryViewModel,
    bookDetailsviewModel: BookDetailViewModel,
    settingsVm: SettingsViewModel
) {
    val context = LocalContext.current

    val coroutineScope = rememberCoroutineScope()
    val openDeleteDialog = remember { mutableStateOf(false) }
    val openRefreshDialog = remember { mutableStateOf(false) }
    val totalProgressionPercentage = remember(item.progression) {
        try {
            val json = JSONObject(item.progression ?: "{}")
            val locations = json.optJSONObject("locations")
            val totalProgression = locations?.optDouble("totalProgression", 0.0) ?: 0.0
            (totalProgression * 100).toInt()
        } catch (e: Exception) {
            0
        }
    }

    // Swipe actions to show book details. Should enable in future after fixes
    /*
    val detailsAction = SwipeAction(icon = painterResource(
        id = if (settingsVm.getCurrentTheme() == ThemeMode.Dark) R.drawable.ic_info else R.drawable.ic_info_white
    ), background = MaterialTheme.colorScheme.primary, onSwipe = {
        viewModel.viewModelScope.launch {
            delay(250L)
            //Navigate to Screens.BookDetailScreen.withBookId
        }
    })
     */

    // Swipe actions to share book. Should enable in future after fixes
    /*
    val shareAction = SwipeAction(icon = painterResource(
        id = if (settingsVm.getCurrentTheme() == ThemeMode.Dark) R.drawable.ic_share else R.drawable.ic_share_white
    ), background = MaterialTheme.colorScheme.primary, onSwipe = {
        val uri = FileProvider.getUriForFile(
            context,
            BuildConfig.APPLICATION_ID + ".provider",
            File(item.filePath)
        )
        val intent = Intent(Intent.ACTION_SEND)
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        intent.type = context.contentResolver.getType(uri)
        intent.putExtra(Intent.EXTRA_STREAM, uri)
        context.startActivity(
            Intent.createChooser(
                intent,
                context.getString(R.string.share_app_chooser)
            )
        )
    })



    SwipeableActionsBox(
        modifier = modifier.padding(vertical = 4.dp),
        startActions = listOf(shareAction),
        endActions = listOf(detailsAction),
        swipeThreshold = 85.dp
    ) { */

        LibraryCard(title = item.title,
            author = item.author?: "Unknown",
            item.href,
            item.rawMediaType,
            item.getDownloadDate(),
            onReadClick = {
                item.id?.let {
                    viewModel.openPublication(it)
                }
            },
            onDeleteClick = { openDeleteDialog.value = true },
            onRefreshClick = { openRefreshDialog.value = true },
            progression = totalProgressionPercentage
        )


    if (openDeleteDialog.value) {
        AlertDialog(onDismissRequest = {
            openDeleteDialog.value = false
        }, title = {
            Text(
                text = stringResource(id = R.string.library_delete_dialog_title),
                color = MaterialTheme.colorScheme.onSurface,
            )
        }, confirmButton = {
            FilledTonalButton(
                onClick = {
                    openDeleteDialog.value = false
                    viewModel.deletePublication(item)
                },
                colors = ButtonDefaults.filledTonalButtonColors(
                    contentColor = MaterialTheme.colorScheme.onErrorContainer,
                    containerColor = MaterialTheme.colorScheme.errorContainer
                )
            ) {
                Text(stringResource(id = R.string.confirm))
            }
        }, dismissButton = {
            TextButton(onClick = {
                openDeleteDialog.value = false
            }) {
                Text(stringResource(id = R.string.cancel))
            }
        })
    }

    if (openRefreshDialog.value) {
        AlertDialog(onDismissRequest = {
            openRefreshDialog.value = false
        },
        title = {
            Text(text = "Refresh Publication?")
        },
        text = {
            Text("Warning: Refreshing this book will replace the current version. Any existing notes, highlights, and reading progress might be lost.")
        }, confirmButton = {
            FilledTonalButton(
                onClick = {
                    coroutineScope.launch {
                        viewModel.markAsRefreshing(item.identifier, item.title)
                        openRefreshDialog.value = false
                        // Fetch The Book From The API, This adds it to model state
                        bookDetailsviewModel.fetchBookDetails(item.identifier)
                        val state = bookDetailsviewModel.state

                        val refreshedBook = state.bookSet?.books?.firstOrNull()
                        if (refreshedBook != null) {

                            // Delete current book in library
                            viewModel.deletePublication(item)

                            // Start the new download
                            bookDetailsviewModel.downloadBook(
                                refreshedBook,
                                downloadProgressListener = { downloadProgress, downloadStatus ->
                                    viewModel.updateProgress(item.identifier, downloadProgress, item.title)
                                    when (downloadStatus){
                                        DownloadManager.STATUS_RUNNING -> {
                                        }

                                        DownloadManager.STATUS_SUCCESSFUL -> {
                                            openRefreshDialog.value = false
                                            viewModel.clearRefreshing(item.identifier)
                                        }

                                        else -> {
                                            viewModel.clearRefreshing(item.identifier)
                                        }
                                    }
                                }
                            )
                        } else {
                            openRefreshDialog.value = false
                            viewModel.clearRefreshing(item.identifier)
                        }
                    }
                },
                colors = ButtonDefaults.filledTonalButtonColors(
                    contentColor = MaterialTheme.colorScheme.onErrorContainer,
                    containerColor = MaterialTheme.colorScheme.errorContainer
                )
            ) {
                Text(stringResource(id = R.string.confirm))
            }
        }, dismissButton = {
            TextButton(onClick = {
                openRefreshDialog.value = false
            }) {
                Text(stringResource(id = R.string.cancel))
            }
        })
    }
}

@Composable
private fun LibraryCard(
    title: String,
    author: String,
    href: String,
    mediaType: String,
    date: String,
    onReadClick: () -> Unit,
    onDeleteClick: () -> Unit,
    onRefreshClick: () -> Unit,
    progression: Int
) {
    Card(
        modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(4.dp)
        ), shape = RoundedCornerShape(0.dp)
    ) {
        Row(
            modifier = Modifier.padding(start = 12.dp, end = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .height(90.dp)
                    .width(90.dp)
                    .padding(10.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = ImageVector.vectorResource(
                        id = R.drawable.ic_library_item
                    ),
                    contentDescription = stringResource(id = R.string.back_button_desc),
                    tint = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.size(32.dp)
                )
            }

            Column(modifier = Modifier.padding(8.dp)) {
                Text(
                    text = title,
                    fontStyle = MaterialTheme.typography.headlineMedium.fontStyle,
                    fontSize = 18.sp,
                    fontFamily = poppinsFont,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = MaterialTheme.colorScheme.onSurface,
                )

                Text(
                    text = author,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.85f),
                    maxLines = 1,
                    fontStyle = MaterialTheme.typography.bodySmall.fontStyle,
                    fontFamily = poppinsFont,
                    fontWeight = FontWeight.Medium,
                    fontSize = 16.sp,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.offset(y = (-8).dp)
                )

                /*
                Row(modifier = Modifier.offset(y = (-8).dp)) {
                    Text(
                        text = fileSize,
                        fontFamily = poppinsFont,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.Light,
                        fontSize = 14.sp,
                        modifier = Modifier.padding(end = 6.dp)
                    )
                    VerticalDivider(
                        modifier = Modifier
                            .height(17.5.dp)
                            .width(1.dp)
                            .clip(CircleShape),
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Text(
                        text = date,
                        fontFamily = poppinsFont,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.Light,
                        fontSize = 14.sp,
                        modifier = Modifier.padding(start = 6.dp)
                    )
                }
                */

                Row(modifier = Modifier.offset(y = (-4).dp), verticalAlignment = Alignment.CenterVertically) {
                    val context = LocalContext.current

                    LibraryCardButton(text = stringResource(id = R.string.library_read_button),
                        icon = ImageVector.vectorResource(id = R.drawable.ic_library_read),
                        onClick = { onReadClick() })

                    Spacer(modifier = Modifier.width(10.dp))

                    Icon(
                        imageVector = Icons.Outlined.Delete,
                        contentDescription = "Delete",
                        modifier = Modifier
                            .size(20.dp)
                            .clickable {
                                onDeleteClick()
                            }
                    )

                    Spacer(modifier = Modifier.width(10.dp))

                    Icon(
                        imageVector = Icons.Outlined.Share,
                        contentDescription = "Share",
                        modifier = Modifier
                            .size(20.dp)
                            .clickable {
                                context.shareEpub(href, mediaType)
                            }
                    )

                    Spacer(modifier = Modifier.width(10.dp))

                    CircularProgressWithText(
                        progress = progression,
                    )

                    Spacer(modifier = Modifier.weight(1f))

                    Icon(
                        imageVector = Icons.Outlined.Refresh,
                        contentDescription = "Refresh",
                        modifier = Modifier
                            .size(20.dp)
                            .clickable {
                                onRefreshClick()
                            }
                    )
                }
                Spacer(modifier = Modifier.height(2.dp))
            }
        }
    }
}

@Composable
private fun LibraryCardButton(
    text: String,
    icon: ImageVector,
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.onSurface,
                shape = RoundedCornerShape(8.dp)
            )
            .clickable { onClick() }
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = "Favorite Icon",
                tint = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.size(14.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = text,
                color = MaterialTheme.colorScheme.onSurface,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
fun CircularProgressWithText(
    progress: Int, // The percentage (0-100)
) {

    val progressFloat = progress / 100f

    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier.size(28.dp)
    ) {
        CircularProgressIndicator(
            progress = progressFloat,
            modifier = Modifier.size(28.dp),
            color = MaterialTheme.colorScheme.onSurface,
            trackColor = MaterialTheme.colorScheme.primaryContainer,
            strokeWidth = 1.dp
        )

        Text(
            text = "$progress%",
            fontSize = 10.sp,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}


@ExperimentalMaterial3Api
@Composable
@Preview
fun LibraryScreenPreview() {
    LibraryCard(title = "The Idiot",
        author = "Fyodor Dostoevsky",
        mediaType = "",
        href = "",
        date = "01- Jan -2020",
        onReadClick = {},
        onDeleteClick = {},
        onRefreshClick = {},
        progression = 80
    )
}
