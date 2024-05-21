/*
 * Copyright (c) 2024 Proton AG. 
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

import android.content.res.Configuration.UI_MODE_NIGHT_NO
import android.content.res.Configuration.UI_MODE_NIGHT_YES
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.material.Icon
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import me.proton.core.compose.theme.ProtonDimens
import me.proton.core.compose.theme.ProtonTheme
import me.proton.core.compose.theme.defaultNorm
import me.proton.core.compose.theme.defaultSmallNorm
import me.proton.core.drive.base.presentation.component.UserThumbnail
import me.proton.core.drive.base.presentation.component.text.TextWithMiddleEllipsis
import me.proton.core.drive.drivelink.shared.presentation.viewstate.ShareUserViewState
import me.proton.core.drive.i18n.R as I18N
import me.proton.core.presentation.R as CorePresentation

@Composable
fun ShareUsers(
    shareUsers: List<ShareUserViewState>,
    modifier: Modifier = Modifier,
    onMore: ((ShareUserViewState) -> Unit)?,
) {
    Column(
        modifier = modifier
    ) {
        shareUsers.forEach { user ->
            ShareUser(
                email = user.email,
                role = user.permissionLabel,
                displayName = user.displayName,
                onMore = onMore?.let { action ->
                    { action(user) }
                },
                firstLetter = user.firstLetter
            )
        }
    }
}

@Composable
private fun ShareUser(
    email: String,
    role: String,
    modifier: Modifier = Modifier,
    displayName: String? = null,
    onMore: (() -> Unit)? = null,
    firstLetter: String,
) {
    Row(
        modifier = modifier
            .clickable(enabled = onMore != null) {
                onMore?.invoke()
            }
            .padding(
                horizontal = ProtonDimens.DefaultSpacing,
                vertical = ProtonDimens.SmallSpacing
            ),
        horizontalArrangement = Arrangement.spacedBy(ProtonDimens.ListItemTextStartPadding),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        UserThumbnail(firstLetter)
        Column(Modifier.weight(1F)) {
            if (displayName != null) {
                TextWithMiddleEllipsis(
                    text = displayName,
                    maxLines = 1,
                    style = ProtonTheme.typography.defaultNorm,
                )
            }
            Text(
                text = email,
                style = ProtonTheme.typography.defaultSmallNorm,
            )
            Text(
                text = role,
                style = ProtonTheme.typography.defaultSmallNorm,
            )
        }
        if (onMore != null) {
            Icon(

                painter = painterResource(id = CorePresentation.drawable.ic_proton_chevron_down_filled),
                contentDescription = stringResource(id = I18N.string.common_more)
            )
        }
    }
}
@Preview(name = "dark", uiMode = UI_MODE_NIGHT_YES)
@Preview(name = "light", uiMode = UI_MODE_NIGHT_NO)
@Composable
fun ShareUsersPreview() {
    ProtonTheme {
        ShareUsers(
            modifier = Modifier
                .sizeIn(maxWidth = 320.dp)
                .background(ProtonTheme.colors.backgroundNorm),
            onMore = {},
            shareUsers = listOf(
                ShareUserViewState(
                    id = "",
                    email = "verylongaddressemail@protonmail.com",
                    permissionLabel = "Editor",
                    displayName = "Very very very Long Display Name",
                    firstLetter = "V",
                    isInvitation = false,
                ),
                ShareUserViewState(
                    id = "",
                    email = "me@pm.me",
                    permissionLabel = "Editor",
                    displayName = "Me",
                    firstLetter = "M",
                    isInvitation = false,
                ),
                ShareUserViewState(
                    id = "",
                    email = "invitee@pm.me",
                    permissionLabel = "Editor",
                    displayName = "Invitee",
                    firstLetter = "I",
                    isInvitation = true,
                ),
                ShareUserViewState(
                    id = "",
                    email = "external@other.com",
                    permissionLabel = "Viewer",
                    displayName = null,
                    firstLetter = "E",
                    isInvitation = false,
                ),
            ),
        )
    }
}
