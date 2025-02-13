/*
 * Copyright (c) 2025 Proton AG.
 * This file is part of Proton Core.
 *
 * Proton Core is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Proton Core is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Proton Core.  If not, see <https://www.gnu.org/licenses/>.
 */

package me.proton.core.drive.drivelink.shared.presentation.component

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import me.proton.core.compose.theme.ProtonTheme
import me.proton.core.crypto.common.pgp.VerificationStatus
import me.proton.core.domain.entity.UserId
import me.proton.core.drive.base.domain.entity.CryptoProperty
import me.proton.core.drive.base.domain.entity.Permissions
import me.proton.core.drive.base.domain.entity.TimestampS
import me.proton.core.drive.base.presentation.common.getThemeDrawableId
import me.proton.core.drive.base.presentation.component.list.ListEmpty
import me.proton.core.drive.base.presentation.component.list.ListError
import me.proton.core.drive.base.presentation.component.list.ListLoading
import me.proton.core.drive.base.presentation.extension.onContent
import me.proton.core.drive.base.presentation.extension.onEmpty
import me.proton.core.drive.base.presentation.extension.onError
import me.proton.core.drive.base.presentation.extension.onLoading
import me.proton.core.drive.base.presentation.state.ListContentState
import me.proton.core.drive.drivelink.shared.presentation.viewevent.UserInvitationViewEvent
import me.proton.core.drive.drivelink.shared.presentation.viewstate.UserInvitationViewState
import me.proton.core.drive.i18n.R
import me.proton.core.drive.share.domain.entity.ShareId
import me.proton.core.drive.share.user.domain.entity.UserInvitation
import me.proton.core.drive.share.user.domain.entity.UserInvitationDetails
import me.proton.core.drive.share.user.domain.entity.UserInvitationId
import me.proton.core.drive.volume.domain.entity.VolumeId
import me.proton.core.drive.i18n.R as I18N
import me.proton.core.presentation.R as CorePresentation


@Composable
fun UserInvitation(
    viewState: UserInvitationViewState,
    viewEvent: UserInvitationViewEvent,
    invitations: List<UserInvitation>,
    modifier: Modifier = Modifier,
) {
    Column(modifier) {
        TopAppBar(
            viewState = viewState,
            viewEvent = viewEvent,
        )
        UserInvitation(
            listContentState = viewState.listContentState,
            invitations = invitations,
            modifier = Modifier.fillMaxSize(),
            onError = {},
            onAccept = { id ->
                viewEvent.onAccept(id)
            },
            onDecline = { id ->
                viewEvent.onDecline(id)
            },
        )
    }
}

@Composable
fun UserInvitation(
    listContentState: ListContentState,
    invitations: List<UserInvitation>,
    modifier: Modifier = Modifier,
    onError: () -> Unit,
    onAccept: (UserInvitationId) -> Unit,
    onDecline: (UserInvitationId) -> Unit,
) {
    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center,
    ) {
        listContentState
            .onLoading {
                ListLoading()
            }
            .onEmpty { state ->
                ListEmpty(
                    imageResId = state.imageResId,
                    titleResId = state.titleId,
                    descriptionResId = state.descriptionResId,
                    actionResId = state.actionResId,
                    onAction = {},
                )
            }
            .onError { state ->
                ListError(
                    message = state.message,
                    actionResId = state.actionResId,
                    onAction = onError,
                )
            }
            .onContent {
                UserInvitationContent(
                    invitations = invitations,
                    onAccept = onAccept,
                    onDecline = onDecline,
                )
            }
    }
}

@Composable
private fun TopAppBar(
    viewState: UserInvitationViewState,
    viewEvent: UserInvitationViewEvent,
) {
    me.proton.core.drive.base.presentation.component.TopAppBar(
        navigationIcon = if (viewState.navigationIconResId != 0) {
            painterResource(id = viewState.navigationIconResId)
        } else null,
        onNavigationIcon = viewEvent.onTopAppBarNavigation,
        title = viewState.title,
    )
}

private val viewEvent = object : UserInvitationViewEvent {
    override val onTopAppBarNavigation: () -> Unit = {}
    override val onAccept: (UserInvitationId) -> Unit = {}
    override val onDecline: (UserInvitationId) -> Unit = {}
}

@Preview
@Composable
fun UserInvitationContentPreview() {
    ProtonTheme {
        val id = UserInvitationId(VolumeId(""), ShareId(UserId(""), ""), "")
        UserInvitation(
            viewState = UserInvitationViewState(
                title = stringResource(I18N.string.shared_user_invitations_title, 2),
                navigationIconResId = CorePresentation.drawable.ic_arrow_back,
                listContentState = ListContentState.Content(isRefreshing = false)
            ),
            viewEvent = viewEvent,
            invitations = listOf(
                UserInvitation(
                    id = id,
                    details = UserInvitationDetails(
                        id = id,
                        inviterEmail = "inviterEmail",
                        inviteeEmail = "inviteeEmail",
                        permissions = Permissions.viewer,
                        keyPacket = "",
                        keyPacketSignature = "",
                        createTime = TimestampS(1720000800),
                        passphrase = "",
                        shareKey = "",
                        creatorEmail = "creatorEmail",
                        type = 1L,
                        linkId = "linkId",
                        cryptoName = CryptoProperty.Decrypted("name", VerificationStatus.Unknown),
                        mimeType = null,
                    )
                )
            )
        )
    }
}

@Preview
@Composable
fun UserInvitationEmptyPreview() {
    ProtonTheme {
        UserInvitation(
            viewState = UserInvitationViewState(
                title = stringResource(I18N.string.shared_user_invitations_title, 0),
                navigationIconResId = CorePresentation.drawable.ic_arrow_back,
                listContentState = ListContentState.Empty(
                    imageResId = getThemeDrawableId(
                        light = me.proton.core.drive.base.presentation.R.drawable.empty_shared_with_me_light,
                        dark = me.proton.core.drive.base.presentation.R.drawable.empty_shared_with_me_dark,
                        dayNight = me.proton.core.drive.base.presentation.R.drawable.empty_shared_with_me_daynight,
                    ),
                    titleId = I18N.string.shared_user_invitations_title_empty,
                    descriptionResId = I18N.string.shared_user_invitations_description_empty
                )
            ),
            viewEvent = viewEvent,
            invitations = emptyList(),
        )
    }
}

@Preview
@Composable
fun UserInvitationErrorPreview() {
    ProtonTheme {
        UserInvitation(
            viewState = UserInvitationViewState(
                title = stringResource(I18N.string.shared_user_invitations_title, 0),
                navigationIconResId = CorePresentation.drawable.ic_arrow_back,
                listContentState = ListContentState.Error(
                    message = "error"
                )
            ),
            viewEvent = viewEvent,
            invitations = emptyList(),
        )
    }
}

@Preview
@Composable
fun UserInvitationLoadingPreview() {
    ProtonTheme {
        UserInvitation(
            viewState = UserInvitationViewState(
                title = stringResource(I18N.string.shared_user_invitations_title, 0),
                navigationIconResId = CorePresentation.drawable.ic_arrow_back,
                listContentState = ListContentState.Loading,
            ),
            viewEvent = viewEvent,
            invitations = emptyList(),
        )
    }
}
