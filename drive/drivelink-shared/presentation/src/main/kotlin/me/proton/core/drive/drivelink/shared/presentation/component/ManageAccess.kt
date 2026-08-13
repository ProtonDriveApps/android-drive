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
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Divider
import androidx.compose.material.Surface
import androidx.compose.material.Switch
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import me.proton.core.compose.component.DeferredCircularProgressIndicator
import me.proton.core.compose.flow.rememberFlowWithLifecycle
import me.proton.core.compose.theme.ProtonDimens.DefaultSpacing
import me.proton.core.compose.theme.ProtonDimens.SmallSpacing
import me.proton.core.compose.theme.ProtonTheme
import me.proton.core.compose.theme.defaultNorm
import me.proton.core.compose.theme.defaultSmallWeak
import me.proton.core.domain.entity.UserId
import me.proton.core.drive.base.presentation.component.ActionButton
import me.proton.core.drive.base.presentation.component.TopAppBar
import me.proton.core.drive.drivelink.shared.presentation.effect.ManageAccessEffect
import me.proton.core.drive.drivelink.shared.presentation.viewevent.ManageAccessViewEvent
import me.proton.core.drive.drivelink.shared.presentation.viewmodel.ManageAccessViewModel
import me.proton.core.drive.drivelink.shared.presentation.viewstate.LoadingViewState
import me.proton.core.drive.drivelink.shared.presentation.viewstate.ManageAccessViewState
import me.proton.core.drive.drivelink.shared.presentation.viewstate.ShareUserType.INVITATION
import me.proton.core.drive.drivelink.shared.presentation.viewstate.ShareUserViewState
import me.proton.core.drive.link.domain.entity.FileId
import me.proton.core.drive.link.domain.entity.LinkId
import me.proton.core.drive.share.domain.entity.ShareId
import me.proton.core.drive.i18n.R as I18N
import me.proton.core.presentation.R as CorePresentation

@Composable
fun ManageAccess(
    navigateToShareViaInvitations: (LinkId) -> Unit,
    navigateToLinkSettings: (LinkId) -> Unit,
    navigateToStopLinkSharing: (LinkId) -> Unit,
    navigateToStopAllSharing: (ShareId) -> Unit,
    navigateToInvitationOptions: (LinkId, String) -> Unit,
    navigateToExternalInvitationOptions: (LinkId, String) -> Unit,
    navigateToMemberOptions: (LinkId, String) -> Unit,
    navigateToShareLinkPermissions: (LinkId) -> Unit,
    navigateBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val viewModel = hiltViewModel<ManageAccessViewModel>()

    val manageAccessViewState by rememberFlowWithLifecycle(viewModel.viewState)
        .collectAsState(initial = null)
    LaunchedEffect(viewModel, navigateBack) {
        viewModel.effect.onEach { effect ->
            when (effect) {
                ManageAccessEffect.Close -> navigateBack()
            }
        }.launchIn(this)
    }
    ManageAccess(
        viewState = manageAccessViewState,
        viewEvent = viewModel.viewEvent(
            navigateToShareViaInvitations = navigateToShareViaInvitations,
            navigateToLinkSettings = navigateToLinkSettings,
            navigateToStopLinkSharing = navigateToStopLinkSharing,
            navigateToStopAllSharing = navigateToStopAllSharing,
            navigateToInvitationOptions = navigateToInvitationOptions,
            navigateToExternalInvitationOptions = navigateToExternalInvitationOptions,
            navigateToMemberOptions = navigateToMemberOptions,
            navigateToShareLinkPermissions = navigateToShareLinkPermissions,
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
            navigationContentDescription = stringResource(I18N.string.common_back_action),
            onNavigationIcon = viewEvent.onBackPressed,
            title = viewState?.title ?: "",
            modifier = Modifier.statusBarsPadding(),
            actions = {
                if (viewState?.canEditMembers == true) {
                    ActionButton(
                        icon = CorePresentation.drawable.ic_proton_user_plus,
                        contentDescription = I18N.string.common_share,
                        onClick = { viewEvent.onInvite() },
                    )
                }
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
        if (viewState.showShareWithAnyone) {
            ShareWithAnyone(
                viewState = viewState.loadingViewState,
                publicUrl = viewState.publicUrl,
                accessibilityDescription = viewState.accessibilityDescription,
                permissionsDescription = viewState.permissionsDescription,
                onRetry = viewEvent.onRetry,
                onStartSharing = viewEvent.onStartLinkSharing,
                onStopSharing = viewEvent.onStopLinkSharing,
                onCopyLink = viewEvent.onCopyLink,
                onConfigureSharing = viewEvent.onConfigureSharing,
                onMore = viewState.takeIf { it.canEditLink }?.let {
                    viewEvent.onEditLinkPermissions
                },
            )
        }
        if (viewState.shareUsers.isNotEmpty()) {
            SectionTitle(stringResource(I18N.string.manage_access_who_has_access))
            ShareUsers(
                shareUsers = viewState.shareUsers,
                onMore = viewState.takeIf { it.canEditMembers }?.let {
                    { user ->
                        viewEvent.onOptions(user)
                    }
                },
            )
        }
        if (viewState.showStopSharing || viewState.showEditorsCanShare) {
            Divider(color = ProtonTheme.colors.separatorNorm)
            if (viewState.showEditorsCanShare) {
                LaunchedEffect(viewState.showNewEditorPermissions) {
                    if (viewState.showNewEditorPermissions) {
                        viewEvent.onNewEditorPermissionsShown()
                    }
                }
                NewEditorPermissionsBubble(
                    visible = viewState.showNewEditorPermissions,
                    onDismiss = viewEvent.onDismissNewEditorPermissions,
                )
                EditorsCanShareToggle(
                    checked = viewState.editorsCanShare,
                    onCheckedChange = { viewEvent.onToggleEditorsCanShare() },
                )
            }
            if (viewState.showStopSharing) {
                StopSharingButton(modifier = Modifier.heightIn(min = MinActionHeight)) {
                    viewEvent.onStopAllSharing()
                }
            }
        }
    }
}

@Composable
private fun EditorsCanShareToggle(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .toggleable(
                value = checked,
            ) {
                onCheckedChange(it)
            }
            .fillMaxWidth()
            .padding(horizontal = DefaultSpacing, vertical = SmallSpacing)
            .heightIn(min = MinActionHeight),
        horizontalArrangement = Arrangement.spacedBy(DefaultSpacing),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(
            modifier = Modifier.weight(1f),
        ) {
            Text(
                text = stringResource(I18N.string.manage_access_allow_editors_to_share_title),
                style = ProtonTheme.typography.defaultNorm,
            )
            Text(
                text = stringResource(I18N.string.manage_access_allow_editors_to_share_description),
                style = ProtonTheme.typography.defaultSmallWeak,
            )
        }
        Switch(
            checked = checked,
            onCheckedChange = null,
        )
    }
}

private val MinActionHeight = 56.dp

@Preview
@Composable
fun ManageAccessSharedPreview() {
    ProtonTheme {
        Surface {
            ManageAccess(
                viewState = ManageAccessViewState(
                    title = stringResource(id = I18N.string.title_manage_access),
                    linkId = FileId(ShareId(UserId(""), ""), ""),
                    publicUrl = "",
                    accessibilityDescription = stringResource(id = I18N.string.manage_access_link_description_public),
                    permissionsDescription = stringResource(id = I18N.string.manage_access_link_viewer_permission),
                    linkName = "name",
                    isLinkNameEncrypted = false,
                    canEditMembers = true,
                    canEditLink = true,
                    showStopSharing = true,
                    loadingViewState = LoadingViewState.Initial,
                    shareUsers = listOf(
                        ShareUserViewState(
                            id = "",
                            email = "pm@proton.me",
                            permissionLabel = "Editor",
                            firstLetter = "P",
                            displayName = "Proton user",
                            type = INVITATION,
                        )
                    ),
                    showShareWithAnyone = true,
                    showEditorsCanShare = true,
                    showNewEditorPermissions = true,
                    editorsCanShare = true,
                ),
                viewEvent = object : ManageAccessViewEvent {
                    override val onBackPressed: () -> Unit = {}
                    override val onRetry: () -> Unit = {}
                    override val onInvite: () -> Unit = {}
                    override val onOptions: (ShareUserViewState) -> Unit = {}
                    override val onCopyLink: (String) -> Unit = {}
                    override val onStartLinkSharing: () -> Unit = {}
                    override val onStopLinkSharing: () -> Unit = {}
                    override val onStopAllSharing: () -> Unit = {}
                    override val onConfigureSharing: () -> Unit = {}
                    override val onEditLinkPermissions: () -> Unit = {}
                    override val onToggleEditorsCanShare: () -> Unit = {}
                    override val onDismissNewEditorPermissions: () -> Unit = {}
                    override val onNewEditorPermissionsShown: () -> Unit = {}
                }
            )
        }
    }
}

@Preview
@Composable
fun ManageAccessNotSharedPreview() {
    ProtonTheme {
        Surface {
            ManageAccess(
                viewState = ManageAccessViewState(
                    title = stringResource(id = I18N.string.title_manage_access),
                    linkId = FileId(ShareId(UserId(""), ""), ""),
                    publicUrl = null,
                    accessibilityDescription = stringResource(id = I18N.string.manage_access_link_description_public),
                    permissionsDescription = stringResource(id = I18N.string.manage_access_link_viewer_permission),
                    linkName = "name",
                    isLinkNameEncrypted = false,
                    canEditMembers = true,
                    canEditLink = true,
                    showStopSharing = true,
                    loadingViewState = LoadingViewState.Initial,
                    shareUsers = emptyList(),
                    showShareWithAnyone = true,
                ),
                viewEvent = object : ManageAccessViewEvent {
                    override val onBackPressed: () -> Unit = {}
                    override val onRetry: () -> Unit = {}
                    override val onInvite: () -> Unit = {}
                    override val onOptions: (ShareUserViewState) -> Unit = {}
                    override val onCopyLink: (String) -> Unit = {}
                    override val onStartLinkSharing: () -> Unit = {}
                    override val onStopLinkSharing: () -> Unit = {}
                    override val onStopAllSharing: () -> Unit = {}
                    override val onConfigureSharing: () -> Unit = {}
                    override val onEditLinkPermissions: () -> Unit = {}
                    override val onToggleEditorsCanShare: () -> Unit = {}
                    override val onDismissNewEditorPermissions: () -> Unit = {}
                    override val onNewEditorPermissionsShown: () -> Unit = {}
                }
            )
        }
    }
}
