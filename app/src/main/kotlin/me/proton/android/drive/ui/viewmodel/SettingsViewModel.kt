/*
 * Copyright (c) 2023 Proton AG.
 * This file is part of Proton Drive.
 *
 * Proton Drive is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Proton Drive is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Proton Drive.  If not, see <https://www.gnu.org/licenses/>.
 */

package me.proton.android.drive.ui.viewmodel

import android.annotation.SuppressLint
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.launch
import me.proton.android.drive.BuildConfig
import me.proton.android.drive.R
import me.proton.core.presentation.R as CorePresentation
import me.proton.android.drive.settings.DebugSettings
import me.proton.core.drive.base.presentation.viewmodel.UserViewModel
import me.proton.core.drive.settings.presentation.component.DebugSettingsStateAndEvent
import me.proton.core.drive.settings.presentation.event.DebugSettingsViewEvent
import me.proton.core.drive.settings.presentation.event.SettingsViewEvent
import me.proton.core.drive.settings.presentation.state.DebugSettingsViewState
import me.proton.core.drive.settings.presentation.state.LegalLink
import me.proton.core.drive.settings.presentation.state.SettingsViewState
import me.proton.drive.android.settings.domain.usecase.GetThemeStyle
import me.proton.drive.android.settings.domain.usecase.UpdateThemeStyle
import me.proton.drive.android.settings.domain.entity.ThemeStyle
import javax.inject.Inject

@HiltViewModel
@SuppressLint("StaticFieldLeak")
class SettingsViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val debugSettings: DebugSettings,
    getThemeStyle: GetThemeStyle,
    private val updateThemeStyle: UpdateThemeStyle,
    savedStateHandle: SavedStateHandle,
) : ViewModel(), UserViewModel by UserViewModel(savedStateHandle) {

    private val _errorMessage = MutableSharedFlow<String>()
    val errorMessage: SharedFlow<String> = _errorMessage

    val viewState: Flow<SettingsViewState> = combine(
        debugSettings.baseUrlFlow,
        debugSettings.hostFlow,
        debugSettings.appVersionHeaderFlow,
        debugSettings.useExceptionMessageFlow,
        getThemeStyle(userId),
    ) { baseUrl, host, appVersionHeader, useExceptionMessage, themeStyle ->
        SettingsViewState(
            navigationIcon = CorePresentation.drawable.ic_arrow_back,
            appNameResId = R.string.app_name,
            appVersion = BuildConfig.VERSION_NAME,
            legalLinks = listOf(
                LegalLink.External(
                    text = R.string.settings_about_links_privacy_policy_title,
                    url = R.string.settings_about_links_privacy_policy_url,
                ),
                LegalLink.External(
                    text = R.string.settings_about_links_terms_of_service_title,
                    url = R.string.settings_about_links_terms_of_service_url,
                ),
            ),
            availableStyles = enumValues<ThemeStyle>().map { style -> style.resId },
            currentStyle = themeStyle.resId,
            debugSettingsStateAndEvent = getDebugSettings(
                host = host,
                baseUrl = baseUrl,
                appVersionHeader = appVersionHeader,
                useExceptionMessage = useExceptionMessage,
            )
        )
    }.shareIn(viewModelScope, SharingStarted.Eagerly, replay = 1)

    fun viewEvent(navigateBack: () -> Unit) = SettingsViewEvent(
        navigateBack = navigateBack,
        onLinkClicked = { link ->
            when (link) {
                is LegalLink.External -> onExternalLinkClicked(link)
            }
        },
        onThemeStyleChanged = { newStyle ->
            viewModelScope.launch {
                updateThemeStyle(userId, enumValues<ThemeStyle>().first { style -> style.resId == newStyle })
            }
        }
    )

    private fun onExternalLinkClicked(link: LegalLink.External) {
        try {
            context.startActivity(
                Intent(Intent.ACTION_VIEW, Uri.parse(context.getString(link.url)))
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            )
        } catch (ignored: ActivityNotFoundException) {
            viewModelScope.launch {
                _errorMessage.emit(context.getString(R.string.common_error_no_browser_available))
            }
        }
    }

    private fun getDebugSettings(
        host: String,
        baseUrl: String,
        appVersionHeader: String,
        useExceptionMessage: Boolean,
    ): DebugSettingsStateAndEvent? =
        BuildConfig.DEBUG
            .takeIf { isDebug -> isDebug }
            ?.let {
                DebugSettingsStateAndEvent(
                    viewState = DebugSettingsViewState(host, baseUrl, appVersionHeader, useExceptionMessage),
                    viewEvent = debugSettingsViewEvent
                )
            }

    private val debugSettingsViewEvent = object : DebugSettingsViewEvent {
        override val onUpdateHost = { host: String -> debugSettings.host = host }
        override val onUpdateBaseUrl = { baseUrl: String -> debugSettings.baseUrl = baseUrl }
        override val onUpdateAppVersionHeader = { header: String -> debugSettings.appVersionHeader = header }
        override val onToggleUseExceptionMessage = { useExceptionMessage: Boolean -> debugSettings.useExceptionMessage = useExceptionMessage }
        override val onReset = { debugSettings.reset(viewModelScope) }
    }

    private val ThemeStyle.resId: Int get() = when (this) {
        ThemeStyle.SYSTEM -> R.string.settings_theme_system_default
        ThemeStyle.DARK -> R.string.settings_theme_dark
        ThemeStyle.LIGHT -> R.string.settings_theme_light
    }
}
