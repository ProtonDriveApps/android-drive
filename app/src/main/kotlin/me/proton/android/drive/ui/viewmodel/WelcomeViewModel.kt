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

import android.content.Context
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import me.proton.android.drive.ui.viewevent.WelcomeViewEvent
import me.proton.android.drive.ui.viewstate.WelcomeDescriptionAction
import me.proton.android.drive.ui.viewstate.WelcomeViewState
import me.proton.core.drive.base.presentation.viewmodel.UserViewModel
import me.proton.core.user.domain.extension.hasSubscription
import me.proton.core.user.domain.repository.UserRepository
import me.proton.drive.android.settings.domain.usecase.UpdateWelcomeShown
import javax.inject.Inject
import me.proton.android.drive.R as AppPresentation
import me.proton.core.drive.base.presentation.R as BasePresentation
import me.proton.core.presentation.R as CorePresentation

@HiltViewModel
class WelcomeViewModel @Inject constructor(
    @ApplicationContext appContext: Context,
    private val updateWelcomeShown: UpdateWelcomeShown,
    userRepository: UserRepository,
    private val savedStateHandle: SavedStateHandle,
) : ViewModel(), UserViewModel by UserViewModel(savedStateHandle) {

    private val commonItems = listOf(
        WelcomeViewState(
            hasSkip = true,
            graphicResId = BasePresentation.drawable.img_welcome,
            title = appContext.getString(
                BasePresentation.string.title_welcome_to,
                appContext.getString(AppPresentation.string.app_name)
            ),
            descriptionResId = BasePresentation.string.welcome_to_description,
            actionTitleResId = BasePresentation.string.common_next_action,
        ),
        WelcomeViewState(
            hasSkip = true,
            graphicResId = BasePresentation.drawable.img_welcome_files,
            title = appContext.getString(BasePresentation.string.title_welcome_files),
            descriptionResId = BasePresentation.string.welcome_files_description,
            actionTitleResId = BasePresentation.string.common_next_action,
        ),
        WelcomeViewState(
            hasSkip = true,
            graphicResId = BasePresentation.drawable.img_welcome_sharing,
            title = appContext.getString(BasePresentation.string.title_welcome_sharing),
            descriptionResId = BasePresentation.string.welcome_sharing_description,
            actionTitleResId = BasePresentation.string.common_next_action,
        ),
    )
    val items: Flow<List<WelcomeViewState>> = userRepository.observeUser(userId)
        .filterNotNull()
        .map { user ->
            commonItems + if (user.hasSubscription()) {
                WelcomeViewState(
                    hasSkip = false,
                    graphicResId = BasePresentation.drawable.img_welcome_private_space,
                    title = appContext.getString(BasePresentation.string.title_welcome_private_space),
                    descriptionResId = BasePresentation.string.welcome_private_space_description,
                    actionTitleResId = BasePresentation.string.welcome_get_started_action,
                )
            } else {
                WelcomeViewState(
                    hasSkip = false,
                    graphicResId = BasePresentation.drawable.img_welcome_bonus,
                    title = appContext.getString(BasePresentation.string.title_welcome_bonus),
                    descriptionResId = BasePresentation.string.welcome_bonus_description,
                    actionTitleResId = BasePresentation.string.welcome_get_started_action,
                    descriptionActions = listOf(
                        WelcomeDescriptionAction(
                            CorePresentation.drawable.ic_proton_checkmark_circle,
                            BasePresentation.string.welcome_bonus_description_action_upload_file,
                        ),
                        WelcomeDescriptionAction(
                            CorePresentation.drawable.ic_proton_checkmark_circle,
                            BasePresentation.string.welcome_bonus_description_action_create_share_url,
                        ),
                        WelcomeDescriptionAction(
                            CorePresentation.drawable.ic_proton_checkmark_circle,
                            BasePresentation.string.welcome_bonus_description_action_recovery_method,
                        )
                    )
                )
            }
        }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    fun viewEvent(
        navigateToLauncher: () -> Unit,
        nextPage: () -> Unit,
    ) = object : WelcomeViewEvent {
        override val onSkip: () -> Unit = { welcomeComplete(navigateToLauncher) }
        override val onAction: (page: Int) -> Unit = { page ->
            viewModelScope.launch {
                val itemsCount = items.first { list -> list.isNotEmpty() }.size
                if (page < itemsCount - 1) {
                    nextPage()
                } else {
                    welcomeComplete(navigateToLauncher)
                }
            }
        }
    }

    private fun welcomeComplete(navigateToLauncher: () -> Unit) {
        viewModelScope.launch {
            updateWelcomeShown(true)
            navigateToLauncher()
        }
    }
}
