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

package me.proton.core.drive.drivelink.device.presentation.component

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import me.proton.core.compose.theme.ProtonDimens.DefaultSpacing
import me.proton.core.compose.theme.ProtonTheme
import me.proton.core.compose.theme.captionWeak
import me.proton.core.compose.theme.defaultNorm
import me.proton.core.crypto.common.pgp.VerificationStatus
import me.proton.core.domain.entity.UserId
import me.proton.core.drive.base.domain.entity.CryptoProperty
import me.proton.core.drive.base.domain.entity.TimestampS
import me.proton.core.drive.base.presentation.component.EncryptedItem
import me.proton.core.drive.base.presentation.component.text.TextWithMiddleEllipsis
import me.proton.core.drive.device.domain.entity.Device
import me.proton.core.drive.device.domain.entity.DeviceId
import me.proton.core.drive.drivelink.device.presentation.extension.getTypeName
import me.proton.core.drive.drivelink.device.presentation.extension.icon
import me.proton.core.drive.link.domain.entity.FolderId
import me.proton.core.drive.share.domain.entity.ShareId
import me.proton.core.drive.volume.domain.entity.VolumeId
import me.proton.core.presentation.R

@Composable
fun DeviceListItem(
    device: Device,
    onClick: (Device) -> Unit,
    onMoreOptionsClick: (Device) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable {
                onClick(device)
            }
            .padding(horizontal = DefaultSpacing, vertical = VerticalSpacing),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            painter = painterResource(id = device.icon),
            contentDescription = null,
            tint = ProtonTheme.colors.iconNorm,
            modifier = Modifier
                .size(LargeIconSize),
        )
        Details(
            device,
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = DefaultSpacing),
        )/*
        MoreOptions(device) {
            onMoreOptionsClick(device)
        }*/
    }
}

@Composable
fun Details(
    device: Device,
    modifier: Modifier = Modifier,
) {
    Column(modifier) {
        DetailsTitle(device = device)
        DetailsSubtitle(device = device)
    }
}

@Composable
fun DetailsTitle(
    device: Device,
    modifier: Modifier = Modifier,
) {
    if (device.cryptoName is CryptoProperty.Decrypted) {
        TextWithMiddleEllipsis(
            text = device.name,
            style = ProtonTheme.typography.defaultNorm(),
            maxLines = 1,
            modifier = modifier,
        )
    } else {
        EncryptedItem()
    }
}

@Composable
fun DetailsSubtitle(
    device: Device,
    modifier: Modifier = Modifier,
) {
    val localContext = LocalContext.current
    TextWithMiddleEllipsis(
        text = device.getTypeName(localContext),
        style = ProtonTheme.typography.captionWeak(),
        maxLines = 1,
        modifier = modifier,
    )
}

@Composable
fun MoreOptions(
    device: Device,
    modifier: Modifier = Modifier,
    onClick: (Device) -> Unit,
) {
    IconButton(
        modifier = modifier.testTag(DeviceTestTag.moreButton),
        onClick = { onClick(device) }
    ) {
        Icon(
            painter = painterResource(id = R.drawable.ic_proton_three_dots_vertical),
            contentDescription = null,
            tint = ProtonTheme.colors.interactionStrongNorm
        )
    }
}

@Preview
@Composable
fun PreviewDeviceListItem() {
    ProtonTheme {
        Column {
            DeviceListItem(
                device = device.copy(
                    cryptoName = CryptoProperty.Decrypted("HP zBook", VerificationStatus.Unknown),
                ),
                onClick = {},
                onMoreOptionsClick = {},
            )
            DeviceListItem(
                device = device.copy(
                    type = Device.Type.MAC_OS,
                    cryptoName = CryptoProperty.Decrypted("MacBook Air", VerificationStatus.Unknown),
                ),
                onClick = {},
                onMoreOptionsClick = {},
            )
            DeviceListItem(
                device = device.copy(
                    type = Device.Type.LINUX,
                    cryptoName = CryptoProperty.Decrypted("NAS", VerificationStatus.Unknown),
                ),
                onClick = {},
                onMoreOptionsClick = {},
            )
            DeviceListItem(
                device = device,
                onClick = {},
                onMoreOptionsClick = {},
            )
        }
    }
}

private val device = Device(
    id = DeviceId("deviceId"),
    volumeId = VolumeId("volumeId"),
    rootLinkId = FolderId(ShareId(UserId("userId"), "shareId"), "linkId"),
    type = Device.Type.WINDOWS,
    syncState = Device.SyncState.ON,
    creationTime = TimestampS(),
    cryptoName = CryptoProperty.Encrypted("e_n_c_r_y_p_t_e_d"),
)

object DeviceTestTag {
    const val moreButton = "three dots button"
}

private val VerticalSpacing = 10.dp
private val LargeIconSize = 32.dp
