/*
 * Copyright (c) 2021-2024 Proton AG.
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

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import me.proton.core.compose.component.bottomsheet.BottomSheetContent
import me.proton.core.compose.theme.ProtonDimens
import me.proton.core.compose.theme.ProtonTheme
import me.proton.core.compose.theme.defaultSmallStrongNorm
import me.proton.core.drive.base.presentation.component.BottomSheetEntry
import me.proton.core.drive.base.presentation.component.UserThumbnail
import me.proton.core.drive.drivelink.shared.presentation.entry.CopyInviteLinkEntry
import me.proton.core.drive.drivelink.shared.presentation.entry.ShareUserOptionEntry
import me.proton.core.drive.drivelink.shared.presentation.viewstate.ShareUserType
import me.proton.core.drive.drivelink.shared.presentation.viewstate.ShareUserViewState

@Composable
fun ShareUserOptions(
    viewState: ShareUserViewState,
    entries: List<ShareUserOptionEntry>,
    modifier: Modifier = Modifier,
) {
    BottomSheetContent(
        modifier = modifier,
        header = {
            ShareUserOptionsHeader(viewState)
        },
        content = {
            entries.forEach { entry ->
                BottomSheetEntry(
                    leadingIcon = entry.leadingIcon,
                    trailingIcon = entry.trailingIcon,
                    title = stringResource(entry.label),
                    onClick = { entry.onClick() }
                )
            }
        },
    )
}

@Composable
private fun ShareUserOptionsHeader(
    viewState: ShareUserViewState,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.padding(
            vertical = ProtonDimens.SmallSpacing
        ),
        horizontalArrangement = Arrangement.spacedBy(ProtonDimens.ListItemTextStartPadding),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        UserThumbnail(firstLetter = viewState.firstLetter)

        Column {
            viewState.displayName?.let { displayName ->
                Text(
                    text = displayName,
                    style = ProtonTheme.typography.defaultSmallStrongNorm,
                )
            }
            Text(
                text = viewState.email,
                style = ProtonTheme.typography.captionRegular,
            )
        }
    }
}

@Preview
@Composable
fun ShareUserOptionsPreview() {
    ProtonTheme {
        Surface {
            ShareUserOptions(
                ShareUserViewState(
                    id = "id",
                    email = "invitee@proton.me",
                    displayName = "Invitee",
                    permissionLabel = "",
                    firstLetter = "I",
                    type = ShareUserType.INVITATION,
                ),
                entries = listOf(
                    CopyInviteLinkEntry {}
                )

            )
        }
    }
}
