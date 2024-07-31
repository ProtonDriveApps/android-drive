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

import androidx.annotation.DrawableRes
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import me.proton.core.compose.component.bottomsheet.BottomSheetContent
import me.proton.core.compose.component.bottomsheet.BottomSheetEntry
import me.proton.core.compose.theme.ProtonDimens
import me.proton.core.compose.theme.ProtonTheme
import me.proton.core.compose.theme.defaultSmallStrong
import me.proton.core.crypto.common.pgp.VerificationStatus
import me.proton.core.domain.entity.UserId
import me.proton.core.drive.base.domain.entity.CryptoProperty
import me.proton.core.drive.base.domain.entity.TimestampS
import me.proton.core.drive.base.presentation.component.EncryptedItem
import me.proton.core.drive.base.presentation.component.text.TextWithMiddleEllipsis
import me.proton.core.drive.device.domain.entity.Device
import me.proton.core.drive.device.domain.entity.DeviceId
import me.proton.core.drive.device.domain.extension.isNameEncrypted
import me.proton.core.drive.device.domain.extension.name
import me.proton.core.drive.drivelink.device.presentation.options.DeviceOptionEntry
import me.proton.core.drive.drivelink.device.presentation.options.RenameDeviceOption
import me.proton.core.drive.link.domain.entity.FolderId
import me.proton.core.drive.share.domain.entity.ShareId
import me.proton.core.drive.volume.domain.entity.VolumeId
import me.proton.core.presentation.R as CorePresentation

@Composable
fun DeviceOptions(
    device: Device,
    entries: List<DeviceOptionEntry>,
    modifier: Modifier = Modifier,
) {
    BottomSheetContent(
        modifier = modifier,
        header = {
            DeviceOptionsHeader(
                device = device,
                iconResId = CorePresentation.drawable.ic_proton_tv,
            )
        },
        content = {
            entries.forEach { entry ->
                BottomSheetEntry(
                    icon = entry.icon,
                    title = stringResource(id = entry.label),
                    onClick = { entry.onClick(device) }
                )
            }
        }
    )
}

@Composable
fun DeviceOptionsHeader(
    device: Device,
    @DrawableRes iconResId: Int,
    modifier: Modifier = Modifier,
) {
    OptionsHeader(
        painter = painterResource(id = iconResId),
        title = device.name,
        isTitleEncrypted = device.isNameEncrypted,
        modifier = modifier
    )
}

@Composable
internal fun OptionsHeader(
    painter: Painter,
    title: String,
    isTitleEncrypted: Boolean,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Image(
            modifier = Modifier
                .size(HeaderIconSize)
                .clip(RoundedCornerShape(ProtonDimens.DefaultCornerRadius)),
            painter = painter,
            contentDescription = null
        )
        Column(
            modifier = Modifier
                .defaultMinSize(minHeight = ProtonDimens.DefaultBottomSheetHeaderMinHeight)
                .padding(start = HeaderSpacing),
            verticalArrangement = Arrangement.Center
        ) {
            Crossfade(targetState = isTitleEncrypted) { isEncrypted ->
                if (isEncrypted) {
                    EncryptedItem()
                } else {
                    TextWithMiddleEllipsis(
                        text = title,
                        style = ProtonTheme.typography.defaultSmallStrong,
                        maxLines = 1,
                    )
                }
            }
        }
    }
}

private val HeaderIconSize = 24.dp
private val HeaderSpacing = 12.dp

@Preview
@Composable
fun PreviewDeviceOptions() {
    ProtonTheme {
        Surface {
            DeviceOptions(
                device = Device(
                    id = DeviceId("device-id"),
                    volumeId = VolumeId("volume-id"),
                    rootLinkId = FolderId(ShareId(UserId("user-id"), "share-id"), "folder-id"),
                    type = Device.Type.WINDOWS,
                    syncState = Device.SyncState.ON,
                    cryptoName = CryptoProperty.Decrypted("DESKTOP", VerificationStatus.Unknown),
                    creationTime = TimestampS(),
                ),
                entries = listOf(
                    RenameDeviceOption {},
                ),
            )
        }
    }
}
