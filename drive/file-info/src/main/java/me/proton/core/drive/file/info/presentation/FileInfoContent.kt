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

package me.proton.core.drive.file.info.presentation

import androidx.compose.animation.Crossfade
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Icon
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.SemanticsPropertyKey
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import me.proton.core.compose.theme.ProtonDimens.DefaultCornerRadius
import me.proton.core.compose.theme.ProtonDimens.DefaultSpacing
import me.proton.core.compose.theme.ProtonDimens.SmallSpacing
import me.proton.core.compose.theme.ProtonTheme
import me.proton.core.compose.theme.defaultSmallNorm
import me.proton.core.compose.theme.defaultSmallWeak
import me.proton.core.compose.theme.subheadlineNorm
import me.proton.core.crypto.common.pgp.VerificationStatus
import me.proton.core.domain.entity.UserId
import me.proton.core.drive.base.domain.entity.Attributes
import me.proton.core.drive.base.domain.entity.Bytes
import me.proton.core.drive.base.domain.entity.CryptoProperty
import me.proton.core.drive.base.domain.entity.Permissions
import me.proton.core.drive.base.domain.entity.TimestampS
import me.proton.core.drive.base.presentation.component.EncryptedItem
import me.proton.core.drive.base.presentation.component.LARGE_HEIGHT
import me.proton.core.drive.drivelink.domain.entity.DriveLink
import me.proton.core.drive.drivelink.domain.extension.isNameEncrypted
import me.proton.core.drive.file.info.presentation.entity.Item
import me.proton.core.drive.file.info.presentation.extension.headerSemantics
import me.proton.core.drive.file.info.presentation.extension.toItems
import me.proton.core.drive.link.domain.entity.FileId
import me.proton.core.drive.link.domain.entity.FolderId
import me.proton.core.drive.link.domain.entity.Link
import me.proton.core.drive.link.domain.entity.OwnedBy
import me.proton.core.drive.share.domain.entity.ShareId
import me.proton.core.drive.thumbnail.presentation.extension.thumbnailPainter
import me.proton.core.drive.volume.domain.entity.VolumeId
import me.proton.core.drive.i18n.R as I18N
import me.proton.core.presentation.R as CorePresentation

@Composable
fun FileInfoContent(
    driveLink: DriveLink,
    items: List<Item>,
    advancedItems: List<Item>,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier
            .verticalScroll(rememberScrollState())
            .testTag(FileInfoTestTag.screen)
    ) {
        val painter = driveLink.thumbnailPainter().painter
        Header(
            painter = painter,
            title = driveLink.name,
            isTitleEncrypted = driveLink.isNameEncrypted,
            modifier = Modifier.headerSemantics(driveLink, painter),
        )
        items.forEach { item ->
            EncryptedInfoItem(
                name = item.name,
                value = item.value,
                isValueEncrypted = item.isValueEncrypted,
            )
        }

        if (advancedItems.isNotEmpty()) {
            AdvancedSection(
                title = I18N.string.file_info_title_advanced_details,
                items = advancedItems,
            )
        }
    }
}

@Composable
private fun Header(
    painter: Painter,
    title: String,
    isTitleEncrypted: Boolean,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.padding(bottom = DefaultSpacing),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Image(
            modifier = Modifier
                .size(HeaderIconSize)
                .clip(RoundedCornerShape(DefaultCornerRadius)),
            painter = painter,
            contentDescription = null
        )
        Crossfade(targetState = isTitleEncrypted) { isEncrypted ->
            if (isEncrypted) {
                EncryptedItem(
                    modifier = Modifier
                        .height(LARGE_HEIGHT)
                        .padding(start = DefaultSpacing)
                )
            } else {
                Text(
                    text = title,
                    modifier = Modifier.padding(start = DefaultSpacing),
                    style = ProtonTheme.typography.subheadlineNorm,
                    overflow = TextOverflow.Ellipsis,
                    maxLines = 1,
                )
            }
        }
    }
}

@Composable
private fun InfoItem(
    name: String,
    value: String,
    modifier: Modifier = Modifier,
) {
    EncryptedInfoItem(
        name = name,
        value = value,
        isValueEncrypted = false,
        modifier = modifier,
    )
}

@Composable
private fun EncryptedInfoItem(
    name: String,
    value: String,
    isValueEncrypted: Boolean,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier
            .fillMaxWidth()
            .padding(vertical = SmallSpacing)
            .semantics(mergeDescendants = true) {
                contentDescription = "$name: $value"
            }
    ) {
        Text(
            text = name,
            style = ProtonTheme.typography.defaultSmallWeak
        )
        Crossfade(targetState = isValueEncrypted) { isEncrypted ->
            if (isEncrypted) {
                EncryptedItem()
            } else {
                Text(
                    text = value,
                    style = ProtonTheme.typography.defaultSmallNorm
                )
            }
        }
    }
}

private val HeaderIconSize = 40.dp

@Composable
private fun AdvancedSection(
    title: Int,
    items: List<Item>,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val (expanded, setExpanded) = rememberSaveable { mutableStateOf(false) }
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable { setExpanded(!expanded) }
            .padding(vertical = SmallSpacing),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = context.getString(title),
            style = ProtonTheme.typography.defaultSmallWeak,
            modifier = Modifier.weight(1f)
        )
        val iconRes = if (expanded) CorePresentation.drawable.ic_proton_chevron_up else CorePresentation.drawable.ic_proton_chevron_down
        Icon(
            painter = painterResource(id = iconRes),
            contentDescription = null,
            modifier = Modifier.size(16.dp)
        )
    }
    if (expanded) {
        items.forEach { item ->
            EncryptedInfoItem(
                name = item.name,
                value = item.value,
                isValueEncrypted = item.isValueEncrypted,
            )
        }
    }
}

@Preview
@Composable
@Suppress("unused")
fun PreviewFileInfoContent() {
    val driveLink = DriveLink.File(
        link = Link.File(
            id = FileId(ShareId(UserId("USER_ID"), "SHARE_ID"), "ID"),
            parentId = FolderId(ShareId(UserId("USER_ID"), "SHARE_ID"), "PARENT_ID"),
            activeRevisionId = "revision",
            size = Bytes(123),
            lastModified = TimestampS(1720000800),
            mimeType = "image/jpg",
            numberOfAccesses = 2,
            isShared = true,
            uploadedBy = "m4@proton.black",
            hasThumbnail = false,
            name = "Link name",
            key = "key",
            passphrase = "passphrase",
            passphraseSignature = "signature",
            contentKeyPacket = "contentKeyPacket",
            contentKeyPacketSignature = null,
            attributes = Attributes(0),
            permissions = Permissions(0),
            state = Link.State.ACTIVE,
            nameSignatureEmail = "",
            hash = "",
            expirationTime = null,
            nodeKey = "",
            nodePassphrase = "",
            nodePassphraseSignature = "",
            signatureEmail = "",
            creationTime = TimestampS(0),
            trashedTime = null,
            shareUrlExpirationTime = null,
            xAttr = null,
            sharingDetails = null,
            ownedBy = OwnedBy("m4@proton.black"),
        ),
        volumeId = VolumeId("VOLUME_ID"),
        isMarkedAsOffline = false,
        isAnyAncestorMarkedAsOffline = false,
        downloadState = null,
        trashState = null,
        cryptoName = CryptoProperty.Decrypted("Link name", VerificationStatus.Success),
        shareInvitationCount = null,
        shareMemberCount = null,
        shareUser = null,
    )
    Surface {
        FileInfoContent(
            driveLink = driveLink,
            items = driveLink.toItems(
                context = LocalContext.current,
                parentPath = "/My files/deep nested/Aaaa/3/",
            ),
            advancedItems = emptyList(),
        )
    }
}

object FileInfoTestTag {
    const val screen = "file info screen"

    object Header {
        val Title = SemanticsPropertyKey<String>(name = "Title")
        val IconType = SemanticsPropertyKey<HeaderIconType>(name = "IconType")

        enum class HeaderIconType {
            PLACEHOLDER,
            THUMBNAIL,
            UNKNOWN,
        }
    }
}
