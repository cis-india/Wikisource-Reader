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

package com.cis.wsreader

import android.content.pm.ShortcutManager
import android.os.Bundle
import android.util.Log
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.ViewModelProvider
import com.cis.wsreader.helpers.NetworkObserver
import com.cis.wsreader.ui.screens.main.MainScreen
import com.cis.wsreader.ui.screens.settings.viewmodels.SettingsViewModel
import com.cis.wsreader.ui.theme.AdjustEdgeToEdge
import com.cis.wsreader.ui.theme.MyneTheme
import dagger.hilt.android.AndroidEntryPoint
import androidx.navigation.NavController


@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    private lateinit var networkObserver: NetworkObserver
    lateinit var settingsViewModel: SettingsViewModel
    private lateinit var mainViewModel: MainViewModel
    private lateinit var navController: NavController

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        networkObserver = NetworkObserver(applicationContext)
        settingsViewModel = ViewModelProvider(this)[SettingsViewModel::class.java]
        mainViewModel = ViewModelProvider(this)[MainViewModel::class.java]

        // Install splash screen before setting content.
        installSplashScreen().setKeepOnScreenCondition {
            mainViewModel.isLoading.value
        }

        enableEdgeToEdge() // enable edge to edge for the activity.

        setContent {
            MyneTheme(settingsViewModel = settingsViewModel) {
                AdjustEdgeToEdge(
                    activity = this,
                    themeState = settingsViewModel.getCurrentTheme()
                )

                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val startDestination by mainViewModel.startDestination
                    val status by networkObserver.observe().collectAsState(
                        initial = NetworkObserver.Status.Unavailable
                    )

                    MainScreen(
                        intent = intent,
                        startDestination = startDestination,
                        networkStatus = status
                    )
                }
            }
        }
    }

    override fun onPause() {
        super.onPause()
    }
}