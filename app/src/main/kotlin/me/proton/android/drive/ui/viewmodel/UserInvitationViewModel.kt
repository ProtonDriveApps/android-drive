/*
 * Copyright (c) 2024-2025 Proton AG.
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
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.last
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.transform
import kotlinx.coroutines.launch
import me.proton.core.domain.arch.DataResult
import me.proton.core.domain.arch.onSuccess
import me.proton.core.drive.base.data.extension.getDefaultMessage
import me.proton.core.drive.base.data.extension.log
import me.proton.core.drive.base.data.extension.logDefaultMessage
import me.proton.core.drive.base.domain.extension.filterSuccessOrError
import me.proton.core.drive.base.domain.extension.onFailure
import me.proton.core.drive.base.domain.log.LogTag.SHARING
import me.proton.core.drive.base.domain.log.LogTag.VIEW_MODEL
import me.proton.core.drive.base.domain.provider.ConfigurationProvider
import me.proton.core.drive.base.domain.usecase.BroadcastMessages
import me.proton.core.drive.base.presentation.R
import me.proton.core.drive.base.presentation.common.getThemeDrawableId
import me.proton.core.drive.base.presentation.state.ListContentState
import me.proton.core.drive.base.presentation.viewmodel.UserViewModel
import me.proton.core.drive.drivelink.shared.presentation.viewevent.UserInvitationViewEvent
import me.proton.core.drive.drivelink.shared.presentation.viewstate.UserInvitationViewState
import me.proton.core.drive.messagequeue.domain.entity.BroadcastMessage
import me.proton.core.drive.share.user.domain.entity.UserInvitation
import me.proton.core.drive.share.user.domain.entity.UserInvitationId
import me.proton.core.drive.share.user.domain.usecase.AcceptUserInvitation
import me.proton.core.drive.share.user.domain.usecase.GetDecryptedUserInvitationsFlow
import me.proton.core.drive.share.user.domain.usecase.RejectUserInvitation
import javax.inject.Inject
import me.proton.core.drive.i18n.R as I18N
import me.proton.core.presentation.R as CorePresentation

@HiltViewModel
class UserInvitationViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    @ApplicationContext private val appContext: Context,
    private val acceptUserInvitation: AcceptUserInvitation,
    private val rejectUserInvitation: RejectUserInvitation,
    getDecryptedUserInvitationsFlow: GetDecryptedUserInvitationsFlow,
    private val broadcastMessages: BroadcastMessages,
    private val configurationProvider: ConfigurationProvider,
) : ViewModel(), UserViewModel by UserViewModel(savedStateHandle) {

    private val emptyState = ListContentState.Empty(
        imageResId = getThemeDrawableId(
            light = R.drawable.empty_shared_with_me_light,
            dark = R.drawable.empty_shared_with_me_dark,
            dayNight = R.drawable.empty_shared_with_me_daynight,
        ),
        titleId = I18N.string.shared_user_invitations_title_empty,
        descriptionResId = I18N.string.shared_user_invitations_description_empty
    )

    private val listContentState: MutableStateFlow<ListContentState> =
        MutableStateFlow(ListContentState.Loading)

    val userInvitations: StateFlow<List<UserInvitation>?> =
        getDecryptedUserInvitationsFlow(userId).transform { result ->
            when (result) {
                is DataResult.Processing -> listContentState.value = ListContentState.Loading
                is DataResult.Error -> {
                    result.log(VIEW_MODEL)
                    listContentState.value = ListContentState.Error(result.logDefaultMessage(
                        context = appContext,
                        useExceptionMessage = configurationProvider.useExceptionMessage,
                        tag = VIEW_MODEL,
                    ))
                }

                is DataResult.Success -> {
                    val invitations = result.value

                    listContentState.value = if (invitations.isEmpty()) {
                        emptyState
                    } else {
                        ListContentState.Content()
                    }
                    emit(invitations)
                }
            }
        }.stateIn(viewModelScope, SharingStarted.Eagerly, null)

    val initialViewState = UserInvitationViewState(
        title = appContext.getString(I18N.string.shared_user_invitations_title).format(0),
        navigationIconResId = CorePresentation.drawable.ic_proton_arrow_back,
        listContentState = listContentState.value,
    )

    val viewState: Flow<UserInvitationViewState> = combine(
        listContentState,
        userInvitations,
    ) { state, userInvitations ->
            initialViewState.copy(
                title = appContext.getString(I18N.string.shared_user_invitations_title)
                    .format(userInvitations.orEmpty().size),
                listContentState = state,
            )
        }

    fun viewEvent(onBack: () -> Unit): UserInvitationViewEvent =
        object : UserInvitationViewEvent {
            override val onTopAppBarNavigation: () -> Unit = onBack
            override val onAccept: (UserInvitationId) -> Unit = this@UserInvitationViewModel::onAccept
            override val onDecline: (UserInvitationId) -> Unit = this@UserInvitationViewModel::onDecline
        }

    private fun onAccept(invitationId: UserInvitationId) {
        viewModelScope.launch {
            acceptUserInvitation(invitationId).filterSuccessOrError()
                .last()
                .onFailure { error ->
                    error.log(SHARING, "Cannot accept invitation: $invitationId")
                    broadcastMessages(
                        userId,
                        error.getDefaultMessage(
                            appContext,
                            configurationProvider.useExceptionMessage
                        ),
                        BroadcastMessage.Type.ERROR
                    )
                }.onSuccess {
                    broadcastMessages(
                        userId,
                        appContext.getString(I18N.string.shared_user_invitations_accept_success),
                    )
                }
        }
    }

    private fun onDecline(invitationId: UserInvitationId) {
        viewModelScope.launch {
            rejectUserInvitation(invitationId)
                .filterSuccessOrError()
                .last()
                .onFailure { error ->
                    error.log(SHARING, "Cannot delete invitation: $invitationId")
                    broadcastMessages(
                        userId,
                        error.getDefaultMessage(
                            appContext,
                            configurationProvider.useExceptionMessage
                        ),
                        BroadcastMessage.Type.ERROR
                    )
                }.onSuccess {
                    broadcastMessages(
                        userId,
                        appContext.getString(I18N.string.shared_user_invitations_decline_success),
                    )
                }
        }
    }
}
