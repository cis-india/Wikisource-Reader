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

package org.cis_india.wsreader.ui.screens.main

import android.annotation.SuppressLint
import android.content.Intent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.psoffritti.taptargetcompose.TapTargetCoordinator
import com.psoffritti.taptargetcompose.TapTargetScope
import com.psoffritti.taptargetcompose.TapTargetStyle
import com.psoffritti.taptargetcompose.TextDefinition
import org.cis_india.wsreader.MainViewModel
import org.cis_india.wsreader.R
import org.cis_india.wsreader.helpers.NetworkObserver
import org.cis_india.wsreader.ui.navigation.BottomBarScreen
import org.cis_india.wsreader.ui.navigation.NavGraph
import org.cis_india.wsreader.ui.navigation.Screens
import org.cis_india.wsreader.ui.screens.settings.viewmodels.SettingsViewModel
import org.cis_india.wsreader.ui.theme.poppinsFont

/**
 * Padding for the bottom navigation bar.
 */
val bottomNavPadding = 70.dp

@SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
@Composable
fun  MainScreen(
    intent: Intent,
    startDestination: String,
    networkStatus: NetworkObserver.Status,
) {
    val settingsViewModel: SettingsViewModel = hiltViewModel()

    val navController = rememberNavController()


    val showNavTapTargets = remember { mutableStateOf(false) }


    LaunchedEffect(settingsViewModel.showNavOnboardingTapTargets.value) {
        if(settingsViewModel.showNavOnboardingTapTargets.value) {
            showNavTapTargets.value = true
        }else{
            showNavTapTargets.value = false
        }
    }

    Scaffold(
        bottomBar = {
            TapTargetCoordinator(
                showTapTargets = showNavTapTargets.value,
                onComplete = {settingsViewModel.navOnboardingComplete()}
            ) {
                BottomBar(navController = navController)
            }
        }, containerColor = MaterialTheme.colorScheme.background
    ) {
        NavGraph(
            startDestination = startDestination,
            navController = navController,
            networkStatus = networkStatus,
            settingsViewModel= settingsViewModel
        )

        val shouldHandleShortCut = remember { mutableStateOf(false) }
        LaunchedEffect(key1 = true) {
            shouldHandleShortCut.value = true
        }
        if (shouldHandleShortCut.value) {
            HandleShortcutIntent(intent, navController)
        }
    }
}

@Composable
private fun  TapTargetScope.BottomBar(navController: NavHostController) {
    val screens = listOf(
        BottomBarScreen.Home,
        BottomBarScreen.Categories,
        BottomBarScreen.Library,
        BottomBarScreen.Settings,
    )

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination
    val bottomBarDestination = screens.any { it.route == currentDestination?.route }

    AnimatedVisibility(visible = bottomBarDestination,
        modifier = Modifier.fillMaxWidth(),
        enter = slideInVertically(initialOffsetY = { it }),
        exit = slideOutVertically(targetOffsetY = { it }),
        content = {

                Row(
                    modifier = Modifier
                        .background(MaterialTheme.colorScheme.surfaceColorAtElevation(3.dp))
                        .padding(12.dp)
                        .fillMaxWidth()
                        .windowInsetsPadding(WindowInsets.navigationBars),
                    horizontalArrangement = Arrangement.SpaceAround,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    screens.forEachIndexed { index, screen ->
                        CustomBottomNavigationItem(
                            screen = screen,
                            isSelected = screen.route == currentDestination?.route,
                            index = index // used in tab target order.
                        ) {
                            navController.navigate(screen.route) {
                                popUpTo(navController.graph.findStartDestination().id)
                                launchSingleTop = true
                            }
                        }
                    }
                }
        })
}

@Composable
private fun TapTargetScope.CustomBottomNavigationItem(
    screen: BottomBarScreen,
    isSelected: Boolean,
    index: Int,
    onClick: () -> Unit,
) {
    val background =
        if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.1f) else Color.Transparent
    val contentColor =
        if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onBackground

    Box(
        modifier = Modifier
            .clip(CircleShape)
            .background(background)
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier.tapTarget(
                precedence = index,
                title = TextDefinition(
                    text = stringResource(screen.tap_target_coodinator_title),
                    textStyle = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSecondaryContainer
                ),
                description = TextDefinition(
                    text = stringResource(screen.tap_target_coodinator_description),
                    textStyle = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSecondaryContainer
                ),
                tapTargetStyle = TapTargetStyle(
                    backgroundColor = MaterialTheme.colorScheme.secondaryContainer,
                    tapTargetHighlightColor = MaterialTheme.colorScheme.onSecondaryContainer,
                    backgroundAlpha = 1f,
                ),
            ).padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {

            Icon(
                imageVector = ImageVector.vectorResource(id = screen.icon),
                contentDescription = stringResource(id = screen.title),
                tint = contentColor
            )

            AnimatedVisibility(visible = isSelected) {
                Text(
                    text = stringResource(id = screen.title),
                    color = contentColor,
                    fontFamily = poppinsFont,
                    fontSize = 14.sp,
                    modifier = Modifier.padding(start = 4.dp)
                )
            }
        }
    }
}


@Composable
private fun HandleShortcutIntent(intent: Intent, navController: NavController) {
    val data = intent.data
    if (data != null && data.scheme == MainViewModel.LAUNCHER_SHORTCUT_SCHEME) {
        val libraryItemId = intent.getIntExtra(MainViewModel.LC_SC_LIBRARY_ITEM_ID, -100)
        if (libraryItemId != -100) {
            navController.navigate(Screens.ReaderDetailScreen.withLibraryItemId(libraryItemId.toString()))
            return
        }
        if (intent.getBooleanExtra(MainViewModel.LC_SC_BOOK_LIBRARY, false)) {
            navController.navigate(BottomBarScreen.Library.route) {
                popUpTo(navController.graph.findStartDestination().id)
                launchSingleTop = true
            }
        }
    }
}