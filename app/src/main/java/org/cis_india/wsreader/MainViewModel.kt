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


package org.cis_india.wsreader


import androidx.compose.runtime.MutableState
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import org.cis_india.wsreader.ui.navigation.BottomBarScreen
import org.cis_india.wsreader.ui.navigation.Screens
import org.cis_india.wsreader.ui.screens.welcome.viewmodels.WelcomeDataStore
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    private val welcomeDataStore: WelcomeDataStore,
) :
    ViewModel() {
    private val _isLoading: MutableState<Boolean> = mutableStateOf(true)
    val isLoading: State<Boolean> = _isLoading

    private val _startDestination: MutableState<String> =
        mutableStateOf(Screens.WelcomeScreen.route)
    val startDestination: State<String> = _startDestination

    companion object {
        // Must be same as the one in AndroidManifest.xml
        const val LAUNCHER_SHORTCUT_SCHEME = "myne_lc_shortcut"

        // Key to get goalId from intent.
        const val LC_SC_LIBRARY_ITEM_ID = "lc_shortcut_library_item_id"

        // Key to detect new goal shortcut.
        const val LC_SC_BOOK_LIBRARY = "lc_shortcut_book_library"
    }

    init {
        viewModelScope.launch {
            _startDestination.value = BottomBarScreen.Home.route
            delay(150)
            _isLoading.value = false
            /*
            Need to update onboarding flow
            // Check if user has completed onboarding.
            welcomeDataStore.readOnBoardingState().collect { completed ->
                if (completed) {
                    _startDestination.value = BottomBarScreen.Home.route
                } else {
                    _startDestination.value = Screens.WelcomeScreen.route
                }

                delay(150)
                _isLoading.value = false
            }*/
        }
    }
}