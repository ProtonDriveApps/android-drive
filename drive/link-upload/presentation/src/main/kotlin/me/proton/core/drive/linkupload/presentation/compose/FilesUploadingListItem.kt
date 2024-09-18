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
package me.proton.core.drive.linkupload.presentation.compose

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import me.proton.core.compose.theme.ProtonDimens.DefaultSpacing
import me.proton.core.compose.theme.ProtonDimens.ExtraSmallSpacing
import me.proton.core.compose.theme.ProtonTheme
import me.proton.core.compose.theme.captionWeak
import me.proton.core.compose.theme.defaultWeak
import me.proton.core.domain.entity.UserId
import me.proton.core.drive.base.domain.entity.Percentage
import me.proton.core.drive.base.domain.entity.toFileTypeCategory
import me.proton.core.drive.base.domain.extension.bytes
import me.proton.core.drive.base.domain.extension.toPercentString
import me.proton.core.drive.base.presentation.component.LinearProgressIndicator
import me.proton.core.drive.base.presentation.component.text.TextWithMiddleEllipsis
import me.proton.core.drive.base.presentation.extension.currentLocale
import me.proton.core.drive.base.presentation.extension.iconResId
import me.proton.core.drive.link.domain.entity.FolderId
import me.proton.core.drive.linkupload.domain.entity.NetworkTypeProviderType
import me.proton.core.drive.linkupload.domain.entity.UploadFileLink
import me.proton.core.drive.linkupload.domain.entity.UploadState
import me.proton.core.drive.share.domain.entity.ShareId
import me.proton.core.drive.volume.domain.entity.VolumeId
import me.proton.core.util.kotlin.exhaustive
import me.proton.core.drive.i18n.R as I18N
import me.proton.core.presentation.R as CorePresentation

@Composable
fun FilesUploadingListItem(
    uploadFileLink: UploadFileLink,
    onCancelClick: (UploadFileLink) -> Unit,
    modifier: Modifier = Modifier,
    uploadProgress: Percentage? = null,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = ListItemHeight),
        contentAlignment = Alignment.BottomStart,
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(start = StartPadding, end = EndPadding)
                .padding(vertical = VerticalPadding),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Image(
                modifier = Modifier.size(IconSize),
                painter = painterResource(id = uploadFileLink.mimeType.toFileTypeCategory().iconResId),
                contentDescription = null
            )
            Details(
                modifier = Modifier
                    .weight(1f)
                    .padding(start = StartPadding, end = EndPadding),
                uploadFileLink = uploadFileLink,
                uploadProgress = uploadProgress,
            )
            IconButton(
                modifier = Modifier.size(IconSize),
                onClick = { onCancelClick(uploadFileLink) },
            ) {
                Icon(
                    painter = painterResource(id = CorePresentation.drawable.ic_proton_cross),
                    contentDescription = stringResource(
                        id = I18N.string.files_upload_content_description_cancel_upload
                    ),
                    tint = ProtonTheme.colors.iconNorm
                )
            }
        }
        if (uploadProgress != null) {
            LinearProgressIndicator(
                modifier = modifier
                    .padding(start = ProgressStartPadding, end = ProgressEndPadding),
                height = ProgressHeight,
                backgroundColor = ProtonTheme.colors.interactionWeakNorm,
                progress = uploadProgress.value,
            )
        }
    }
}

@Composable
fun Details(
    uploadFileLink: UploadFileLink,
    uploadProgress: Percentage?,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth()
    ) {
        DetailsTitle(title = uploadFileLink.name)
        DetailsSubtitle(state = uploadFileLink.state, uploadProgress = uploadProgress)
    }
}

@Composable
fun DetailsTitle(
    title: String,
    modifier: Modifier = Modifier,
    isEnabled: Boolean = true,
) {
    TextWithMiddleEllipsis(
        text = title,
        style = ProtonTheme.typography.defaultWeak(enabled = isEnabled),
        maxLines = 1,
        modifier = modifier,
    )
}

@Composable
fun DetailsSubtitle(
    state: UploadState,
    modifier: Modifier = Modifier,
    uploadProgress: Percentage? = null,
    isEnabled: Boolean = true,
) {
    Row(
        modifier = modifier
            .fillMaxSize()
            .padding(top = SubtitleTopPadding),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (uploadProgress == null) {
            CircularProgressIndicator(
                modifier = Modifier
                    .align(alignment = Alignment.Bottom)
                    .size(size = SubtitleProgressSize)
                    .padding(end = SubtitleProgressEndPadding),
                strokeWidth = 1.dp
            )
        }
        Text(
            text = state.title(uploadProgress),
            style = ProtonTheme.typography.captionWeak(enabled = isEnabled),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun UploadState.title(progress: Percentage? = null): String = when (this) {
    UploadState.UNPROCESSED,
    UploadState.IDLE,
    UploadState.CREATING_NEW_FILE -> stringResource(id = I18N.string.files_upload_stage_waiting)
    UploadState.SPLITTING_URI_TO_BLOCKS,
    UploadState.ENCRYPTING_BLOCKS -> stringResource(id = I18N.string.files_upload_stage_encrypting)
    UploadState.GETTING_UPLOAD_LINKS -> stringResource(id = I18N.string.files_upload_stage_uploading)
    UploadState.UPLOADING_BLOCKS -> progress?.let {
        stringResource(
            id = I18N.string.files_upload_stage_uploading_with_progress, progress.toPercentString(
                locale = LocalContext.current.currentLocale
            )
        )
    } ?: stringResource(id = I18N.string.files_upload_stage_uploading)
    UploadState.UPDATING_REVISION,
    UploadState.CLEANUP -> stringResource(
        id = I18N.string.files_upload_stage_uploading_with_progress, Percentage(100).toPercentString(
            locale = LocalContext.current.currentLocale
        )
    )
}.exhaustive

@Preview
@Composable
fun FilesUploadListItemPreview() {
    ProtonTheme {
        Surface {
            FilesUploadingListItem(
                modifier = Modifier.height(ListItemHeight),
                uploadFileLink = DEFAULT_UPLOAD_FILE_LINK,
                onCancelClick = {},
            )
        }
    }
}


val ListItemHeight = 64.dp
val IconSize = 40.dp
val StartPadding = DefaultSpacing
val EndPadding = 12.dp
val VerticalPadding = 10.dp
val SubtitleTopPadding = ExtraSmallSpacing
val SubtitleProgressEndPadding = ExtraSmallSpacing
val SubtitleProgressSize = 14.dp
val ProgressStartPadding = 72.dp
val ProgressEndPadding = 20.dp
val ProgressHeight = 3.dp

private val DEFAULT_UPLOAD_FILE_LINK = UploadFileLink(
    id = 0L,
    userId = UserId("1"),
    volumeId = VolumeId("volume_id"),
    shareId = ShareId(UserId("1"), "share_id"),
    parentLinkId = FolderId(ShareId(UserId("1"),"share_id"), "folder_id"),
    linkId = "file_id",
    draftRevisionId = "revision_id",
    name = "IMG-1234567890.jpg",
    mimeType = "image/jpeg",
    nodeKey = "",
    nodePassphrase = "",
    nodePassphraseSignature = "",
    contentKeyPacket = "",
    contentKeyPacketSignature = "",
    manifestSignature = "",
    state = UploadState.IDLE,
    size = 123.bytes,
    lastModified = null,
    networkTypeProviderType = NetworkTypeProviderType.DEFAULT,
    priority = UploadFileLink.USER_PRIORITY,
)
