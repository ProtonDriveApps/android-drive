/*
 * Copyright (c) 2022-2023 Proton AG.
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

import android.content.Intent
import androidx.activity.compose.BackHandler
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Divider
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.app.ShareCompat
import me.proton.core.compose.theme.ProtonDimens.DefaultSpacing
import me.proton.core.compose.theme.ProtonDimens.MediumSpacing
import me.proton.core.compose.theme.ProtonTheme
import me.proton.core.compose.theme.default
import me.proton.core.compose.theme.defaultSmall
import me.proton.core.compose.theme.defaultSmallStrong
import me.proton.core.compose.theme.headlineSmall
import me.proton.core.drive.base.presentation.component.EncryptedItem
import me.proton.core.drive.base.presentation.component.ProtonListItem
import me.proton.core.drive.drivelink.shared.presentation.viewevent.ActionViewEvent
import me.proton.core.drive.drivelink.shared.presentation.viewevent.SharedDriveLinkViewEvent
import me.proton.core.drive.drivelink.shared.presentation.viewstate.SharedDriveLinkViewState
import me.proton.core.drive.link.domain.entity.LinkId
import me.proton.core.drive.i18n.R as I18N
import me.proton.core.presentation.R as CorePresentation

@Composable
fun SharedDriveLink(
    viewState: SharedDriveLinkViewState,
    viewEvent: SharedDriveLinkViewEvent,
    driveLinkId: LinkId,
    modifier: Modifier = Modifier,
) {
    BackHandler { viewEvent.onBackPressed() }
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(top = LinkSpacing)
            .verticalScroll(rememberScrollState())
            .testTag(SharedDriveLinkTestTag.content)
    ) {
        Link(
            publicUrl = viewState.publicUrl,
            accessibilityDescription = viewState.accessibilityDescription,
            linkName =  viewState.linkName,
            isLinkNameEncrypted = viewState.isLinkNameEncrypted,
            modifier = Modifier.padding(bottom = MediumSpacing)
        ) { viewEvent.onCopyLink(viewState.publicUrl) }
        Divider(
            color = ProtonTheme.colors.separatorNorm,
            modifier = Modifier.padding(bottom = MediumSpacing)
        )
        PrivacySettings(
            viewState = viewState.privacySettingsViewState,
            viewEvent = viewEvent,
            modifier = Modifier.padding(bottom = MediumSpacing)
        )
        Divider(
            color = ProtonTheme.colors.separatorNorm,
            modifier = Modifier.padding(bottom = MediumSpacing)
        )
        ShareDriveLinkActions(
            viewEvent = viewEvent,
            publicUrl = viewState.publicUrl,
            linkId = driveLinkId,
            password = viewState.privacySettingsViewState.password,
            modifier = Modifier.padding(bottom = MediumSpacing)
        )
    }
}

@Composable
private fun Link(
    publicUrl: String,
    accessibilityDescription: String,
    linkName: String,
    isLinkNameEncrypted: Boolean,
    modifier: Modifier = Modifier,
    onCopyLink: (String) -> Unit,
) {
    Column(
        modifier = modifier
            .padding(horizontal = DefaultSpacing)
    ) {
        Text(
            text = stringResource(id = I18N.string.common_link),
            style = ProtonTheme.typography.headlineSmall,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(bottom = MediumSpacing),
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = MediumSpacing),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = publicUrl,
                style = ProtonTheme.typography.default,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            IconButton(
                onClick = { onCopyLink(publicUrl) }
            ) {
                Icon(
                    painter = painterResource(id = CorePresentation.drawable.ic_proton_link),
                    tint = ProtonTheme.colors.iconNorm,
                    contentDescription = null,
                )
            }
        }
        Text(
            text = accessibilityDescription,
            style = ProtonTheme.typography.defaultSmall,
            modifier = Modifier.testTag(SharedDriveLinkTestTag.accessibilityDescription)
        )
        Crossfade(targetState = isLinkNameEncrypted) { isEncrypted ->
            if (isEncrypted) {
                EncryptedItem()
            } else {
                Text(
                    text = linkName,
                    style = ProtonTheme.typography.defaultSmallStrong,
                )
            }
        }
    }
}

@Composable
fun ShareDriveLinkActions(
    viewEvent: ActionViewEvent,
    publicUrl: String,
    password: String?,
    linkId: LinkId,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    Column(
        modifier = modifier
    ) {
        ShareDriveLinkAction(
            icon = CorePresentation.drawable.ic_proton_squares,
            title = I18N.string.shared_link_action_copy_link,
            onClick = { viewEvent.onCopyLink(publicUrl) }
        )
        password
            ?.takeIf { password.isNotEmpty() }
            ?.let {
                ShareDriveLinkAction(
                    icon = CorePresentation.drawable.ic_proton_squares,
                    title = I18N.string.shared_link_action_copy_password,
                    onClick = { viewEvent.onCopyPassword(password) }
                )
            }
        ShareDriveLinkAction(
            icon = CorePresentation.drawable.ic_proton_arrow_up_from_square,
            title = I18N.string.shared_link_action_share_link,
            onClick = {
                context.startActivity(
                    Intent.createChooser(
                        ShareCompat.IntentBuilder(context)
                            .setType("text/plain")
                            .setText(publicUrl)
                            .intent,
                        null
                    )
                )
            }
        )
        ShareDriveLinkAction(
            icon = CorePresentation.drawable.ic_proton_link_slash,
            title = I18N.string.shared_link_action_stop_sharing,
            onClick = { viewEvent.onStopSharing(linkId) }
        )
    }
}

@Composable
fun ShareDriveLinkAction(
    @DrawableRes icon: Int,
    @StringRes title: Int,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    ProtonListItem(
        icon = icon,
        title = title,
        iconTitlePadding = LinkSpacing,
        modifier = modifier
            .clickable { onClick() }
            .padding(horizontal = DefaultSpacing),
    )
}

private val LinkSpacing = 20.dp

object SharedDriveLinkTestTag {
    const val content = "content"
    const val accessibilityDescription = "accessibility description"
}
