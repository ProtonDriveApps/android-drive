/*
 * Copyright (c) 2025 Proton AG.
 * This file is part of Proton Drive.
 *
 * Proton Drive is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Proton Drive is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Proton Drive.  If not, see <https://www.gnu.org/licenses/>.
 */

package me.proton.android.drive.photos.presentation.component

import android.content.Context
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.Icon
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import coil.ImageLoader
import coil.compose.LocalImageLoader
import coil.request.ImageRequest
import com.google.accompanist.placeholder.PlaceholderHighlight
import com.google.accompanist.placeholder.placeholder
import com.google.accompanist.placeholder.shimmer
import me.proton.android.drive.photos.presentation.BuildConfig
import me.proton.android.drive.photos.presentation.extension.thumbnailPainter
import me.proton.core.compose.theme.ProtonDimens
import me.proton.core.compose.theme.ProtonTheme
import me.proton.core.compose.theme.captionNorm
import me.proton.core.crypto.common.pgp.VerificationStatus
import me.proton.core.domain.entity.UserId
import me.proton.core.drive.base.domain.entity.Attributes
import me.proton.core.drive.base.domain.entity.Bytes
import me.proton.core.drive.base.domain.entity.CryptoProperty
import me.proton.core.drive.base.domain.entity.FileTypeCategory
import me.proton.core.drive.base.domain.entity.Permissions
import me.proton.core.drive.base.domain.entity.TimestampS
import me.proton.core.drive.base.domain.entity.toFileTypeCategory
import me.proton.core.drive.base.presentation.component.CircleSelection
import me.proton.core.drive.drivelink.domain.entity.DriveLink
import me.proton.core.drive.drivelink.domain.extension.getThumbnailId
import me.proton.core.drive.drivelink.domain.extension.isSharedByLinkOrWithUsers
import me.proton.core.drive.file.base.domain.entity.ThumbnailType
import me.proton.core.drive.file.base.domain.extension.mediaDuration
import me.proton.core.drive.file.base.domain.extension.toXAttr
import me.proton.core.drive.files.presentation.extension.LayoutType
import me.proton.core.drive.files.presentation.extension.driveLinkSemantics
import me.proton.core.drive.link.domain.entity.FileId
import me.proton.core.drive.link.domain.entity.FolderId
import me.proton.core.drive.link.domain.entity.Link
import me.proton.core.drive.link.domain.entity.SharingDetails
import me.proton.core.drive.linkdownload.domain.entity.DownloadState
import me.proton.core.drive.share.domain.entity.ShareId
import me.proton.core.drive.shareurl.base.domain.entity.ShareUrlId
import me.proton.core.drive.thumbnail.presentation.extension.cacheKey
import me.proton.core.drive.thumbnail.presentation.extension.thumbnailVO
import me.proton.core.drive.volume.domain.entity.VolumeId
import me.proton.core.presentation.R
import java.util.concurrent.TimeUnit.MINUTES

@Composable
fun MediaItem(
    link: DriveLink?,
    index: Int,
    modifier: Modifier = Modifier,
    isSelected: Boolean = false,
    inMultiselect: Boolean = false,
    onClick: (DriveLink) -> Unit,
    onLongClick: (DriveLink) -> Unit,
) {
    if (link != null) {
        MediaItem(
            link = link,
            modifier = modifier,
            isSelected = isSelected,
            inMultiselect = inMultiselect,
            onClick = onClick,
            onLongClick = onLongClick,
        )
        if (BuildConfig.DEBUG) {
            Text(text = "index $index")
        }
    } else {
        Box(
            modifier = modifier
                .aspectRatio(0.75F)
                .background(ProtonTheme.colors.backgroundSecondary)
                .placeholder(
                    visible = true,
                    color = ProtonTheme.colors.backgroundSecondary,
                    highlight = PlaceholderHighlight.shimmer(ProtonTheme.colors.backgroundNorm)
                ),
        )
        if (BuildConfig.DEBUG) {
            Text(text = "index $index")
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun MediaItem(
    link: DriveLink,
    modifier: Modifier = Modifier,
    isSelected: Boolean = false,
    inMultiselect: Boolean = false,
    onClick: (DriveLink) -> Unit,
    onLongClick: (DriveLink) -> Unit,
) {
    val haptic = LocalHapticFeedback.current
    Box(
        modifier = modifier
            .aspectRatio(0.75F)
            .driveLinkSemantics(link, LayoutType.Grid)
            .combinedClickable(
                enabled = onClick != null,
                onClick = { onClick(link) },
                onLongClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    onLongClick(link)
                },
            )
            .background(ProtonTheme.colors.backgroundSecondary)
    ) {
        val localContext = LocalContext.current
        val imageLoader = LocalImageLoader.current
        LaunchedEffect(localContext) {
            link.preCachePhotoThumbnail(localContext, imageLoader)
        }
        val painterWrapper = link.thumbnailPainter()
        Image(
            modifier = Modifier
                .fillMaxSize()
                .placeholder(
                    visible = painterWrapper.isLoading,
                    color = ProtonTheme.colors.backgroundSecondary,
                    highlight = PlaceholderHighlight.shimmer(ProtonTheme.colors.backgroundNorm)
                ),
            painter = painterWrapper.painter,
            contentDescription = null,
            contentScale = ContentScale.Crop,
        )
        Crossfade(
            targetState = inMultiselect,
        ) { inMultiselect ->
            if (inMultiselect) {
                MultiselectOverlay(isSelected = isSelected)
            } else {
                FileOverlay(file = link)
            }
        }
    }
}

@Composable
fun MultiselectOverlay(
    isSelected: Boolean,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.BottomEnd,
    ) {
        Box(
            modifier = Modifier
                .size(ProtonDimens.DefaultButtonMinHeight),
            contentAlignment = Alignment.Center,
        ) {
            CircleSelection(
                isSelected = isSelected,
            )
        }
    }
}

@Composable
fun FileOverlay(
    file: DriveLink,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.BottomEnd,
    ) {
        Row(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(ProtonDimens.ExtraSmallSpacing),
            horizontalArrangement = Arrangement.spacedBy(ProtonDimens.ExtraSmallSpacing),
        ) {
            if (file.isMarkedAsOffline) {
                PhotoDownloadIcon(file)
            }
            if (file.isSharedByLinkOrWithUsers) {
                PhotoIcon(R.drawable.ic_proton_users, null)
            }
        }
        when (file.mimeType.toFileTypeCategory()) {
            FileTypeCategory.Video -> VideoFileOverlay(
                file = file,
                modifier = Modifier
                    .padding(ProtonDimens.ExtraSmallSpacing),
            )

            else -> Unit
        }
    }
}

@Composable
private fun PhotoDownloadIcon(file: DriveLink) {
    when (file.downloadState) {
        is DownloadState.Downloaded -> PhotoIcon(R.drawable.ic_proton_arrow_down, null)
        DownloadState.Downloading -> PhotoIconContainer {
            CircularProgressIndicator(
                modifier = Modifier
                    .size(iconSize)
                    .align(Alignment.Center),
                color = ProtonTheme.colors.shade0,
                strokeWidth = 1.dp
            )
        }

        DownloadState.Error -> PhotoIcon(R.drawable.ic_proton_pause_filled, null)
        null -> Unit
    }
}

@Composable
fun VideoFileOverlay(
    file: DriveLink,
    modifier: Modifier = Modifier,
) {

    val duration = remember(file.cryptoXAttr) {
        file.cryptoXAttr
            .takeIf { it.status == VerificationStatus.Success }
            ?.let { cryptoXAttr ->
                cryptoXAttr.value?.toXAttr()?.getOrNull()?.mediaDuration
            }
    }
    if (duration != null) {
        Row(
            modifier = modifier
                .background(
                    ProtonTheme.colors.shade80.copy(alpha = 0.7F),
                    CircleShape
                )
                .padding(start = ProtonDimens.ExtraSmallSpacing),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(ProtonDimens.ExtraSmallSpacing),
        ) {
            Text(
                text = "%01d:%02d".format(
                    duration.inWholeMinutes,
                    duration.inWholeSeconds % MINUTES.toSeconds(1),
                ),
                style = ProtonTheme.typography.captionNorm.copy(color = ProtonTheme.colors.shade0)
            )
            PhotoIcon(
                id = R.drawable.ic_proton_play_filled,
                contentDescription = null,
            )
        }
    } else {
        PhotoIcon(
            modifier = modifier,
            id = R.drawable.ic_proton_play_filled,
            contentDescription = null,
        )
    }
}

@Composable
private fun PhotoIconContainer(
    modifier: Modifier = Modifier,
    content: @Composable() (BoxScope.() -> Unit),
) {
    Box(
        modifier = modifier
            .size(ProtonDimens.DefaultSpacing)
            .background(
                ProtonTheme.colors.shade80.copy(alpha = 0.7F),
                CircleShape
            ),
        content = content,
    )
}

@Composable
private fun PhotoIcon(
    id: Int,
    contentDescription: String?,
    modifier: Modifier = Modifier,
) {
    PhotoIconContainer(modifier = modifier) {
        Icon(
            modifier = Modifier
                .size(iconSize)
                .align(Alignment.Center),
            painter = painterResource(id = id),
            contentDescription = contentDescription,
            tint = ProtonTheme.colors.shade0,
        )
    }
}

private fun DriveLink.preCachePhotoThumbnail(context: Context, imageLoader: ImageLoader) {
    (this as? DriveLink.File)
        ?.takeIf { it.mimeType.toFileTypeCategory() == FileTypeCategory.Image }
        ?.let { photoDriveLink ->
            photoDriveLink.getThumbnailId(ThumbnailType.PHOTO)?.let {
                photoDriveLink.thumbnailVO(ThumbnailType.PHOTO)
            }
        }
        ?.let { source ->
            imageLoader.enqueue(
                ImageRequest.Builder(context)
                    .data(source)
                    .memoryCacheKey(source.cacheKey)
                    .build()
            )
        }
}

private val iconSize = 12.dp

@Preview
@Composable
fun MediaItemPreview() {
    val userId = UserId("USER_ID")
    val driveLink = DriveLink.File(
        link = Link.File(
            id = FileId(ShareId(userId, "SHARE_ID"), "ID"),
            parentId = FolderId(ShareId(UserId("USER_ID"), "SHARE_ID"), "PARENT_ID"),
            activeRevisionId = "revision",
            size = Bytes(123),
            lastModified = TimestampS(System.currentTimeMillis() / 1000),
            mimeType = "video/mp4",
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
            signatureEmail = "",
            creationTime = TimestampS(0),
            trashedTime = null,
            shareUrlExpirationTime = null,
            xAttr = null,
            sharingDetails = SharingDetails(
                ShareId(userId, ""),
                shareUrlId = ShareUrlId(ShareId(userId, ""), "")
            ),
            photoCaptureTime = TimestampS(0),
            photoContentHash = "",
            mainPhotoLinkId = "MAIN_ID"
        ),
        volumeId = VolumeId("VOLUME_ID"),
        isMarkedAsOffline = true,
        isAnyAncestorMarkedAsOffline = false,
        downloadState = DownloadState.Downloaded(),
        trashState = null,
        cryptoName = CryptoProperty.Decrypted("Link name", VerificationStatus.Success),
        cryptoXAttr = CryptoProperty.Decrypted(
            """{"Common":{"ModificationTime":"2023-07-27T13:52:23.636Z"},"Media":{"Duration":46}}""",
            VerificationStatus.Success,
        ),
        shareInvitationCount = null,
        shareMemberCount = null,
        shareUser = null,
    )

    ProtonTheme {
        MediaItem(
            driveLink,
            modifier = Modifier.width(120.dp),
            onClick = {},
            onLongClick = {},
        )
    }
}
