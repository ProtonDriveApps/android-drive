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

import androidx.compose.animation.Crossfade
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import me.proton.android.drive.photos.presentation.extension.details
import me.proton.android.drive.photos.presentation.extension.thumbnailPainter
import me.proton.android.drive.photos.presentation.state.AlbumsItem
import me.proton.core.compose.theme.ProtonDimens
import me.proton.core.compose.theme.ProtonDimens.DefaultCornerRadius
import me.proton.core.compose.theme.ProtonTheme
import me.proton.core.compose.theme.captionWeak
import me.proton.core.compose.theme.defaultSmallStrongNorm
import me.proton.core.drive.base.domain.entity.CryptoProperty
import me.proton.core.drive.base.presentation.component.EncryptedItem
import me.proton.core.drive.drivelink.domain.entity.DriveLink
import me.proton.core.drive.files.presentation.extension.LayoutType
import me.proton.core.drive.files.presentation.extension.driveLinkSemantics
import me.proton.core.drive.link.domain.entity.AlbumId
import me.proton.core.drive.base.presentation.R as BasePresentation
import me.proton.core.drive.i18n.R as I18N

@Composable
fun AlbumListItem(
    albumsItem: AlbumsItem.Listing,
    modifier: Modifier = Modifier,
    onClick: (AlbumId) -> Unit,
) {
    albumsItem.album?.let { album ->
        album.shareMemberCount
        AlbumListItem(
            album = album,
            cover = albumsItem.coverLink,
            modifier = modifier,
            albumDetails = albumsItem.albumDetails,
            onClick = onClick,
        )
    } ?: AlbumListItem(
        albumName = "",
        albumDetails = albumsItem.albumDetails ?: "",
        isAlbumNameEncrypted = true,
        albumPhotoCount = albumsItem.photoCount,
        modifier = modifier,
        coverDriveLink = albumsItem.coverLink,
    )
}

@Composable
fun AlbumListItem(
    album: DriveLink.Album,
    cover: DriveLink.File?,
    modifier: Modifier = Modifier,
    albumDetails: String? = null,
    onClick: (AlbumId) -> Unit,
) {
    val localContext = LocalContext.current
    AlbumListItem(
        albumName = album.name,
        albumDetails = albumDetails,
        isAlbumNameEncrypted = album.cryptoName is CryptoProperty.Encrypted,
        albumPhotoCount = album.photoCount,
        modifier = modifier
            .driveLinkSemantics(album, LayoutType.List)
            .clickable(onClick = { onClick(album.id) }),
        coverDriveLink = cover,
    )
}

@Composable
fun AlbumListItem(
    albumName: String,
    albumDetails: String?,
    isAlbumNameEncrypted: Boolean,
    albumPhotoCount: Long,
    modifier: Modifier = Modifier,
    coverDriveLink: DriveLink.File? = null,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = ProtonDimens.DefaultSpacing)
            .heightIn(min = 52.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Crossfade(
            targetState = coverDriveLink == null,
            modifier = modifier,
        ) { showPlaceholder ->
            if (showPlaceholder) {
                Image(
                    painter = painterResource(BasePresentation.drawable.ic_folder_album),
                    contentDescription = null,
                    modifier = Modifier
                        .size(HeaderIconSize)
                        .clip(RoundedCornerShape(DefaultCornerRadius))
                )
            } else {
                coverDriveLink?.let { link ->
                    val painterWrapper = link.thumbnailPainter()
                    Image(
                        modifier = Modifier
                            .size(HeaderIconSize)
                            .clip(RoundedCornerShape(DefaultCornerRadius)),
                        painter = painterWrapper.painter,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                    )
                }
            }
        }
        Column(
            modifier = Modifier
                .weight(1F)
                .fillMaxWidth(),
        ) {
            if (isAlbumNameEncrypted) {
                EncryptedItem()
            } else {
                Text(
                    text = albumName,
                    style = ProtonTheme.typography.defaultSmallStrongNorm,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            if (albumDetails != null) {
                Text(
                    text = albumDetails,
                    style = ProtonTheme.typography.captionWeak,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        Text(
            text = albumPhotoCount.toString(),
            style = ProtonTheme.typography.captionWeak,
        )
    }
}

private val HeaderIconSize = 40.dp

@Preview
@Composable
fun AlbumBottomSheetEntryLightPreview() {
    ProtonTheme(isDark = false) {
        Albums(Modifier.background(Color.White))
    }
}

@Preview
@Composable
fun AlbumBottomSheetEntryDarkPreview() {
    ProtonTheme(isDark = true) {
        Albums(Modifier.background(Color.Black))
    }
}

@Composable
private fun Albums(modifier: Modifier = Modifier) {
    Column(modifier = modifier) {
        AlbumListItem(
            albumName = "Islands",
            albumDetails = stringResource(I18N.string.albums_share_multiple_photos_options_album_details),
            isAlbumNameEncrypted = false,
            albumPhotoCount = 10000,
            coverDriveLink = null,
        )
        AlbumListItem(
            albumName = "Forests",
            albumDetails = stringResource(I18N.string.albums_share_multiple_photos_options_album_details),
            isAlbumNameEncrypted = true,
            albumPhotoCount = 2,
            coverDriveLink = null,
        )
    }
}
