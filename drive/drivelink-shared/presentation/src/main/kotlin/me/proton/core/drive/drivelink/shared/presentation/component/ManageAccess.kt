/*
 * Copyright (c) 2022-2024 Proton AG.
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

import androidx.compose.animation.Crossfade
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Divider
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.navigation.compose.hiltViewModel
import me.proton.core.compose.component.DeferredCircularProgressIndicator
import me.proton.core.compose.flow.rememberFlowWithLifecycle
import me.proton.core.compose.theme.ProtonDimens.DefaultSpacing
import me.proton.core.compose.theme.ProtonDimens.MediumSpacing
import me.proton.core.compose.theme.ProtonDimens.SmallSpacing
import me.proton.core.compose.theme.ProtonTheme
import me.proton.core.compose.theme.defaultStrongNorm
import me.proton.core.domain.entity.UserId
import me.proton.core.drive.base.presentation.component.ActionButton
import me.proton.core.drive.base.presentation.component.TopAppBar
import me.proton.core.drive.drivelink.shared.presentation.extension.isShared
import me.proton.core.drive.drivelink.shared.presentation.viewevent.ManageAccessViewEvent
import me.proton.core.drive.drivelink.shared.presentation.viewmodel.ManageAccessViewModel
import me.proton.core.drive.drivelink.shared.presentation.viewstate.LoadingViewState
import me.proton.core.drive.drivelink.shared.presentation.viewstate.ManageAccessViewState
import me.proton.core.drive.drivelink.shared.presentation.viewstate.ShareUserViewState
import me.proton.core.drive.link.domain.entity.FileId
import me.proton.core.drive.link.domain.entity.LinkId
import me.proton.core.drive.share.domain.entity.ShareId
import me.proton.core.drive.i18n.R as I18N
import me.proton.core.presentation.R as CorePresentation

@Composable
fun ManageAccess(
    navigateToShareViaInvitations: (LinkId) -> Unit,
    navigateToShareViaLink: (LinkId) -> Unit,
    navigateToStopLinkSharing: (LinkId) -> Unit,
    navigateToStopAllSharing: (ShareId) -> Unit,
    navigateToInvitationOptions: (LinkId, String) -> Unit,
    navigateToMemberOptions: (LinkId, String) -> Unit,
    navigateBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val viewModel = hiltViewModel<ManageAccessViewModel>()

    val manageAccessViewState by rememberFlowWithLifecycle(viewModel.viewState)
        .collectAsState(initial = null)
    ManageAccess(
        viewState = manageAccessViewState,
        viewEvent = viewModel.viewEvent(
            navigateToShareViaInvitations = navigateToShareViaInvitations,
            navigateToShareViaLink = navigateToShareViaLink,
            navigateToStopLinkSharing = navigateToStopLinkSharing,
            navigateToStopAllSharing = navigateToStopAllSharing,
            navigateToInvitationOptions = navigateToInvitationOptions,
            navigateToMemberOptions = navigateToMemberOptions,
            navigateBack = navigateBack,
        ),
        modifier = modifier
            .fillMaxSize()
            .navigationBarsPadding(),
    )
}

@Composable
private fun ManageAccess(
    viewState: ManageAccessViewState?,
    viewEvent: ManageAccessViewEvent,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxSize(),
    ) {
        TopAppBar(
            navigationIcon = painterResource(id = CorePresentation.drawable.ic_arrow_back),
            onNavigationIcon = viewEvent.onBackPressed,
            title = viewState?.title ?: "",
            modifier = Modifier.statusBarsPadding(),
            actions = {
                ActionButton(
                    icon = CorePresentation.drawable.ic_proton_user_plus,
                    contentDescription = I18N.string.common_share,
                    onClick = { viewEvent.onInvite() },
                )
            }
        )
        Crossfade(targetState = viewState, label = "manage-access-content") { viewState ->
            if (viewState != null) {
                ManageAccessContent(
                    viewState = viewState,
                    viewEvent = viewEvent,
                    modifier = Modifier.navigationBarsPadding(),
                )
            } else {
                DeferredCircularProgressIndicator(Modifier.fillMaxSize())
            }
        }
    }
}

@Composable
fun ManageAccessContent(
    viewState: ManageAccessViewState,
    viewEvent: ManageAccessViewEvent,
    modifier: Modifier = Modifier,
) {
    Column(modifier.verticalScroll(rememberScrollState())) {
        SectionTitle(stringResource(I18N.string.manage_access_share_with_anyone))
        ShareWithAnyone(
            viewState = viewState.loadingViewState,
            publicUrl = viewState.publicUrl,
            accessibilityDescription = viewState.accessibilityDescription,
            onRetry = viewEvent.onRetry,
            onStartSharing = viewEvent.onStartLinkSharing,
            onStopSharing = viewEvent.onStopLinkSharing,
            onCopyLink = viewEvent.onCopyLink,
            onConfigureSharing = viewEvent.onConfigureSharing,
        )
        if (viewState.shareUsers.isNotEmpty()) {
            SectionTitle(stringResource(I18N.string.manage_access_share_with))
            ShareUsers(
                shareUsers = viewState.shareUsers,
                onMore = viewState.takeIf { it.canEdit }?.let {
                    { user ->
                        if (user.isInvitation) {
                            viewEvent.onInvitationOptions(user.id)
                        } else {
                            viewEvent.onMemberOptions(user.id)
                        }
                    }
                },
            )
        }
        if (viewState.isShared) {
            Divider(color = ProtonTheme.colors.separatorNorm)
            StopSharingButton(onClick = { viewEvent.onStopAllSharing() })
        }
    }
}

@Composable
private fun SectionTitle(
    text: String,
    modifier: Modifier = Modifier,
) {
    Text(
        modifier = modifier.padding(
            top = MediumSpacing,
            start = DefaultSpacing,
            end = DefaultSpacing,
            bottom = SmallSpacing
        ),
        text = text,
        style = ProtonTheme.typography.defaultStrongNorm,
    )
}

@Preview
@Composable
private fun ManageAccessSharedPreview() {
    ProtonTheme {
        ManageAccess(
            viewState = ManageAccessViewState(
                title = stringResource(id = I18N.string.title_manage_access),
                linkId = FileId(ShareId(UserId(""), ""), ""),
                publicUrl = "",
                accessibilityDescription = "Anyone with this link",
                linkName = "name",
                isLinkNameEncrypted = false,
                canEdit = true,
                loadingViewState = LoadingViewState.Initial,
                shareUsers = listOf(
                    ShareUserViewState(
                        id = "",
                        email = "pm@proton.me",
                        permissionLabel = "Editor",
                        firstLetter = "P",
                        displayName = "Proton user",
                        isInvitation = true,
                    )
                )
            ),
            viewEvent = object : ManageAccessViewEvent {
                override val onBackPressed: () -> Unit = {}
                override val onRetry: () -> Unit = {}
                override val onInvite: () -> Unit = {}
                override val onInvitationOptions: (String) -> Unit = {}
                override val onMemberOptions: (String) -> Unit = {}
                override val onCopyLink: (String) -> Unit = {}
                override val onStartLinkSharing: () -> Unit = {}
                override val onStopLinkSharing: () -> Unit = {}
                override val onStopAllSharing: () -> Unit = {}
                override val onConfigureSharing: () -> Unit = {}
            }
        )
    }
}

@Preview
@Composable
private fun ManageAccessNotSharedPreview() {
    ProtonTheme {
        ManageAccess(
            viewState = ManageAccessViewState(
                title = stringResource(id = I18N.string.title_manage_access),
                linkId = FileId(ShareId(UserId(""), ""), ""),
                publicUrl = null,
                accessibilityDescription = "Anyone with this link",
                linkName = "name",
                isLinkNameEncrypted = false,
                canEdit = true,
                loadingViewState = LoadingViewState.Initial,
                shareUsers = emptyList()
            ),
            viewEvent = object : ManageAccessViewEvent {
                override val onBackPressed: () -> Unit = {}
                override val onRetry: () -> Unit = {}
                override val onInvite: () -> Unit = {}
                override val onInvitationOptions: (String) -> Unit = {}
                override val onMemberOptions: (String) -> Unit = {}
                override val onCopyLink: (String) -> Unit = {}
                override val onStartLinkSharing: () -> Unit = {}
                override val onStopLinkSharing: () -> Unit = {}
                override val onStopAllSharing: () -> Unit = {}
                override val onConfigureSharing: () -> Unit = {}
            }
        )
    }
}
