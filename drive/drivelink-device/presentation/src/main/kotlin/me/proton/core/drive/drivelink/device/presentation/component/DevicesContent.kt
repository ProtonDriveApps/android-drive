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

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import me.proton.core.compose.theme.ProtonTheme
import me.proton.core.crypto.common.pgp.VerificationStatus
import me.proton.core.domain.entity.UserId
import me.proton.core.drive.base.domain.entity.CryptoProperty
import me.proton.core.drive.base.domain.entity.TimestampS
import me.proton.core.drive.device.domain.entity.Device
import me.proton.core.drive.device.domain.entity.DeviceId
import me.proton.core.drive.link.domain.entity.FolderId
import me.proton.core.drive.share.domain.entity.ShareId
import me.proton.core.drive.volume.domain.entity.VolumeId

@Composable
fun DevicesContent(
    devices: List<Device>,
    modifier: Modifier = Modifier,
    onClick: (Device) -> Unit,
    onMoreOptions: (Device) -> Unit,
    onRenderThumbnail: (Device) -> Unit,
) {
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
    ) {
        items(
            count = devices.size,
            key = { index -> devices[index].id.id },
        ) {index ->
            val device = devices[index]
            DeviceListItem(
                device = device,
                onClick = onClick,
                onMoreOptionsClick = onMoreOptions,
                onRenderThumbnail = onRenderThumbnail,
            )
        }
    }
}

@Preview
@Composable
fun PreviewDevicesContent() {
    ProtonTheme {
        Surface {
            DevicesContent(
                devices = listOf(
                    Device(
                        id = DeviceId("device-id-1"),
                        volumeId = VolumeId("volume-id"),
                        rootLinkId = FolderId(ShareId(UserId("user-id"), "share-id-1"), "folder-id-1"),
                        type = Device.Type.WINDOWS,
                        syncState = Device.SyncState.ON,
                        creationTime = TimestampS(),
                        cryptoName = CryptoProperty.Decrypted("HP zBook", VerificationStatus.Unknown),
                    ),
                    Device(
                        id = DeviceId("device-id-2"),
                        volumeId = VolumeId("volume-id"),
                        rootLinkId = FolderId(ShareId(UserId("user-id"), "share-id-2"), "folder-id-2"),
                        type = Device.Type.MAC_OS,
                        syncState = Device.SyncState.ON,
                        creationTime = TimestampS(),
                        cryptoName = CryptoProperty.Decrypted("MacBook Air", VerificationStatus.Unknown),
                    ),
                    Device(
                        id = DeviceId("device-id-3"),
                        volumeId = VolumeId("volume-id"),
                        rootLinkId = FolderId(ShareId(UserId("user-id"), "share-id-3"), "folder-id-3"),
                        type = Device.Type.LINUX,
                        syncState = Device.SyncState.ON,
                        creationTime = TimestampS(),
                        cryptoName = CryptoProperty.Decrypted("QNAP", VerificationStatus.Unknown),
                    )
                ),
                onClick = {},
                onMoreOptions = {},
                onRenderThumbnail = {},
            )
        }
    }
}
