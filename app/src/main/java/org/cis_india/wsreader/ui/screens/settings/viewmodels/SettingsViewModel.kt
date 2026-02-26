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

package org.cis_india.wsreader.ui.screens.settings.viewmodels

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import org.cis_india.wsreader.helpers.PreferenceUtil
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

enum class ThemeMode {
    Light, Dark, Auto
}

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val preferenceUtil: PreferenceUtil
) : ViewModel() {

    private val _theme = MutableLiveData(ThemeMode.Auto)
    private val _amoledTheme = MutableLiveData(false)
    private val _materialYou = MutableLiveData(Build.VERSION.SDK_INT >= Build.VERSION_CODES.S)

    val theme: LiveData<ThemeMode> = _theme
    val amoledTheme: LiveData<Boolean> = _amoledTheme
    val materialYou: LiveData<Boolean> = _materialYou

    private val _showHomeOnboardingTapTargets: MutableState<Boolean> = mutableStateOf(
        value = preferenceUtil.getBoolean(PreferenceUtil.HOME_ONBOARDING_BOOL, true)
    )

    val showHomeOnboardingTapTargets: State<Boolean> = _showHomeOnboardingTapTargets

    private val _showNavOnboardingTapTargets: MutableState<Boolean> = mutableStateOf(
        value = preferenceUtil.getBoolean(PreferenceUtil.NAV_ONBOARDING_BOOL, false)
    )

    val showNavOnboardingTapTargets: State<Boolean> = _showNavOnboardingTapTargets

    private val _firstOnboardingTapTargets: MutableState<Boolean> = mutableStateOf(
        value = preferenceUtil.getBoolean(PreferenceUtil.FIRST_ONBOARDING_BOOL, true)
    )

    val firstOnboardingTapTargets: State<Boolean> = _firstOnboardingTapTargets


    init {
        _theme.value = ThemeMode.entries.toTypedArray()[getThemeValue()]
        _amoledTheme.value = getAmoledThemeValue()
        _materialYou.value = getMaterialYouValue()
    }

    // Getters ================================================================================
    fun setTheme(newTheme: ThemeMode) {
        _theme.postValue(newTheme)
        preferenceUtil.putInt(PreferenceUtil.APP_THEME_INT, newTheme.ordinal)
    }

    fun setAmoledTheme(newValue: Boolean) {
        _amoledTheme.postValue(newValue)
        preferenceUtil.putBoolean(PreferenceUtil.AMOLED_THEME_BOOL, newValue)
    }

    fun setOnboardingGuide(newValue: Boolean) {
        preferenceUtil.putBoolean(PreferenceUtil.HOME_ONBOARDING_BOOL, newValue)

        if (newValue) {
            _showHomeOnboardingTapTargets.value = true
            _showNavOnboardingTapTargets.value = false
            preferenceUtil.putBoolean(PreferenceUtil.NAV_ONBOARDING_BOOL, false)
        } else {
            preferenceUtil.putBoolean(PreferenceUtil.HOME_ONBOARDING_BOOL, newValue)
            preferenceUtil.putBoolean(PreferenceUtil.NAV_ONBOARDING_BOOL, false)

            _showHomeOnboardingTapTargets.value = false
            _showNavOnboardingTapTargets.value = false
        }
    }

    fun setMaterialYou(newValue: Boolean) {
        _materialYou.postValue(newValue)
        preferenceUtil.putBoolean(PreferenceUtil.MATERIAL_YOU_BOOL, newValue)
    }

    fun setInternalReaderValue(newValue: Boolean) {
        preferenceUtil.putBoolean(PreferenceUtil.INTERNAL_READER_BOOL, newValue)
    }

    // Getters ================================================================================

    fun getThemeValue() = preferenceUtil.getInt(
        PreferenceUtil.APP_THEME_INT, ThemeMode.Auto.ordinal
    )

    fun getAmoledThemeValue() = preferenceUtil.getBoolean(
        PreferenceUtil.AMOLED_THEME_BOOL, false
    )

    fun getOnboardingGuideValue() = preferenceUtil.getBoolean(
        PreferenceUtil.HOME_ONBOARDING_BOOL, false
    )

    fun getMaterialYouValue() = preferenceUtil.getBoolean(
        PreferenceUtil.MATERIAL_YOU_BOOL, Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
    )

    fun getInternalReaderValue() = preferenceUtil.getBoolean(
        PreferenceUtil.INTERNAL_READER_BOOL, true
    )

    fun homeOnboardingComplete() {
        preferenceUtil.putBoolean(PreferenceUtil.HOME_ONBOARDING_BOOL, false)
        _showHomeOnboardingTapTargets.value = false

        if(firstOnboardingTapTargets.value) {
            preferenceUtil.putBoolean(PreferenceUtil.NAV_ONBOARDING_BOOL, true)
            preferenceUtil.putBoolean(PreferenceUtil.FIRST_ONBOARDING_BOOL, false)
            _showNavOnboardingTapTargets.value = true
            _firstOnboardingTapTargets.value = false
        }

    }


    fun navOnboardingComplete() {
        preferenceUtil.putBoolean(PreferenceUtil.NAV_ONBOARDING_BOOL, false)
        _showNavOnboardingTapTargets.value = false
    }

    @Composable
    fun getCurrentTheme(): ThemeMode {
        return if (theme.value == ThemeMode.Auto) {
            if (isSystemInDarkTheme()) ThemeMode.Dark else ThemeMode.Light
        } else theme.value!!
    }
}