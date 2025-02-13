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


import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import me.proton.core.compose.component.ProtonSecondaryButton
import me.proton.core.compose.theme.ProtonDimens
import me.proton.core.compose.theme.ProtonDimens.DefaultButtonMinHeight
import me.proton.core.compose.theme.ProtonDimens.DefaultCornerRadius
import me.proton.core.compose.theme.ProtonTheme
import me.proton.core.compose.theme.captionWeak
import me.proton.core.compose.theme.defaultNorm
import me.proton.core.crypto.common.pgp.VerificationStatus
import me.proton.core.domain.entity.UserId
import me.proton.core.drive.base.domain.entity.CryptoProperty
import me.proton.core.drive.base.domain.entity.FileTypeCategory
import me.proton.core.drive.base.domain.entity.Permissions
import me.proton.core.drive.base.domain.entity.TimestampS
import me.proton.core.drive.base.domain.entity.toFileTypeCategory
import me.proton.core.drive.base.presentation.R
import me.proton.core.drive.base.presentation.component.EncryptedItem
import me.proton.core.drive.base.presentation.component.LetterBadge
import me.proton.core.drive.base.presentation.component.text.TextWithMiddleEllipsis
import me.proton.core.drive.base.presentation.extension.asHumanReadableString
import me.proton.core.drive.base.presentation.extension.iconResId
import me.proton.core.drive.drivelink.shared.presentation.extension.key
import me.proton.core.drive.share.domain.entity.ShareId
import me.proton.core.drive.share.user.domain.entity.UserInvitation
import me.proton.core.drive.share.user.domain.entity.UserInvitationDetails
import me.proton.core.drive.share.user.domain.entity.UserInvitationId
import me.proton.core.drive.volume.domain.entity.VolumeId
import me.proton.core.drive.i18n.R as I18N

@Composable
fun UserInvitationContent(
    invitations: List<UserInvitation>,
    modifier: Modifier = Modifier,
    onAccept: (UserInvitationId) -> Unit,
    onDecline: (UserInvitationId) -> Unit,
) {

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
    ) {
        items(
            count = invitations.size,
            key = { index -> invitations[index].id.key },
        ) { index ->
            val invitation = invitations[index]
            UserInvitationItem(
                modifier = Modifier.testTag(UserInvitationContentTestFlag.item),
                details = invitation.details,
                onAccept = { onAccept(invitation.id) },
                onDecline = { onDecline(invitation.id) },
            )
        }
    }
}

@Composable
fun UserInvitationItem(
    modifier: Modifier = Modifier,
    details: UserInvitationDetails? = null,
    onAccept: () -> Unit,
    onDecline: () -> Unit,
) {
    val enabled = details != null

    Row(
        modifier = modifier.padding(
            horizontal = ProtonDimens.DefaultSpacing,
            vertical = ProtonDimens.SmallSpacing
        ),
        horizontalArrangement = Arrangement.spacedBy(ProtonDimens.DefaultSpacing),
        verticalAlignment = Alignment.Top,
    ) {
        Box {
            if(details != null) {
                Image(
                    painter = painterResource(
                        if (details.type == 1L) {
                            R.drawable.ic_folder_48
                        } else {
                            (details.mimeType?.toFileTypeCategory()
                                ?: FileTypeCategory.Unknown).iconResId
                        }
                    ),
                    contentDescription = null,
                )
                LetterBadge(details.inviterEmail)
            } else {
                Box(
                    modifier = Modifier
                        .size(IconSize)
                        .clip(RoundedCornerShape(DefaultCornerRadius))
                        .background(ProtonTheme.colors.backgroundSecondary)
                )
            }
        }
        Column {
            if (details?.cryptoName is CryptoProperty.Decrypted) {
                TextWithMiddleEllipsis(
                    text = details.cryptoName.value,
                    style = ProtonTheme.typography.defaultNorm,
                )
            } else {
                EncryptedItem()
            }
            Text(
                text = details?.description.orEmpty(),
                style = ProtonTheme.typography.captionWeak,
            )
            Spacer(Modifier.height(ProtonDimens.DefaultSpacing))
            Row(
                horizontalArrangement = Arrangement.spacedBy(ProtonDimens.DefaultSpacing)
            )
            {
                ProtonSecondaryButton(
                    modifier = Modifier.weight(1F),
                    enabled = enabled,
                    onClick = onDecline,
                ) {
                    Text(text = stringResource(id = I18N.string.shared_user_invitations_decline_button))
                }
                ProtonSecondaryButton(
                    modifier = Modifier.weight(1F),
                    enabled = enabled,
                    onClick = onAccept,
                ) {
                    Text(text = stringResource(id = I18N.string.shared_user_invitations_accept_button))
                }
            }
        }
    }
}

val UserInvitationDetails.description: String get() = "$inviterEmail \u2022 ${createTime.asHumanReadableString()}"
val IconSize = DefaultButtonMinHeight

@Preview
@Composable
fun UserInvitationItemPendingPreview() {
    ProtonTheme {
        Surface(color = ProtonTheme.colors.backgroundNorm) {
            UserInvitationItem(
                onAccept = {},
                onDecline = {},
            )
        }
    }
}

@Preview
@Composable
fun UserInvitationItemPreview() {
    ProtonTheme {
        Surface(color = ProtonTheme.colors.backgroundNorm) {
            UserInvitationItem(
                details = UserInvitationDetails(
                    id = UserInvitationId(VolumeId(""), ShareId(UserId(""), ""), ""),
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
                ),
                onAccept = {},
                onDecline = {},
            )
        }
    }
}

object UserInvitationContentTestFlag {
    const val item = "user invitation item"
}
