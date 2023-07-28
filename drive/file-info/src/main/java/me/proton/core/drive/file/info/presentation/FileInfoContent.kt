/*
 * Copyright (c) 2021-2023 Proton AG.
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

import android.os.Build
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
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
import me.proton.core.drive.base.presentation.entity.toFileTypeCategory
import me.proton.core.drive.base.presentation.extension.asHumanReadableString
import me.proton.core.drive.base.presentation.extension.toReadableDate
import me.proton.core.drive.drivelink.domain.entity.DriveLink
import me.proton.core.drive.drivelink.domain.extension.isNameEncrypted
import me.proton.core.drive.file.info.presentation.extension.headerSemantics
import me.proton.core.drive.link.domain.entity.BaseLink
import me.proton.core.drive.link.domain.entity.FileId
import me.proton.core.drive.link.domain.entity.Folder
import me.proton.core.drive.link.domain.entity.FolderId
import me.proton.core.drive.link.domain.entity.Link
import me.proton.core.drive.link.presentation.extension.lastModified
import me.proton.core.drive.share.domain.entity.ShareId
import me.proton.core.drive.thumbnail.presentation.extension.thumbnailPainter
import me.proton.core.drive.volume.domain.entity.VolumeId
import me.proton.core.drive.i18n.R as I18N

@Composable
fun FileInfoContent(
    driveLink: DriveLink,
    pathToFileNode: String,
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
        EncryptedInfoItem(
            name = stringResource(id = I18N.string.file_info_name_entry),
            value = driveLink.name,
            isValueEncrypted = driveLink.isNameEncrypted,
        )
        InfoItem(
            name = stringResource(id = I18N.string.file_info_uploaded_by_entry),
            value = driveLink.uploadedBy,
        )
        InfoItem(
            name = stringResource(id = I18N.string.file_info_location_entry),
            value = pathToFileNode,
        )
        InfoItem(
            name = stringResource(id = I18N.string.file_info_last_modified_entry),
            value = driveLink.lastModified(),
        )
        if (driveLink !is Folder) {
            InfoItem(
                name = stringResource(id = I18N.string.file_info_type_entry),
                value = driveLink.getType(),
            )
            InfoItem(
                name = stringResource(id = I18N.string.file_info_mime_type_entry),
                value = driveLink.mimeType,
            )
            InfoItem(
                name = stringResource(id = I18N.string.file_info_size_entry),
                value = driveLink.size.asHumanReadableString(LocalContext.current),
            )
        }
        InfoItem(
            name = stringResource(id = I18N.string.file_info_shared_entry),
            value = stringResource(id = if (driveLink.isShared) {
                I18N.string.common_yes
            } else {
                I18N.string.common_no
            }),
        )
        if (driveLink.isShared) {
            InfoItem(
                name = stringResource(id = I18N.string.file_info_number_of_accesses_entry),
                value = driveLink.numberOfAccesses.toString(),
            )
            InfoItem(
                name = stringResource(id = I18N.string.file_info_share_url_expiration_time),
                value = driveLink.shareUrlExpirationTime?.toReadableDate()
                    ?: stringResource(id = I18N.string.file_info_share_url_no_expiration_time),
            )
        }
    }
}

@Composable
private fun BaseLink.getType(): String {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        return LocalContext.current.contentResolver.getTypeInfo(mimeType).label.toString()
    }
    return stringResource(id = mimeType.toFileTypeCategory().labelResId)
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

@Preview
@Composable
@Suppress("unused")
private fun PreviewFileInfoContent() {
    FileInfoContent(
        driveLink = DriveLink.File(
            link = Link.File(
                id = FileId(ShareId(UserId("USER_ID"), "SHARE_ID"), "ID"),
                parentId = FolderId(ShareId(UserId("USER_ID"), "SHARE_ID"), "PARENT_ID"),
                activeRevisionId = "revision",
                size = Bytes(123),
                lastModified = TimestampS(System.currentTimeMillis() / 1000),
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
                isFavorite = false,
                attributes = Attributes(0),
                permissions = Permissions(0),
                state = Link.State.ACTIVE,
                nameSignatureEmail = "",
                hash = "",
                expirationTime = null,
                nodeKey = "",
                nodePassphrase = "",
                nodePassphraseSignature = "",
                signatureAddress = "",
                creationTime = TimestampS(0),
                trashedTime = null,
                shareUrlExpirationTime = null,
                xAttr = null,
                shareUrlId = null,
            ),
            volumeId = VolumeId("VOLUME_ID"),
            isMarkedAsOffline = false,
            isAnyAncestorMarkedAsOffline = false,
            downloadState = null,
            trashState = null,
            cryptoName = CryptoProperty.Decrypted("Link name", VerificationStatus.Success),
        ),
        pathToFileNode = "/My files/deep nested/Aaaa/3/",
    )
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
