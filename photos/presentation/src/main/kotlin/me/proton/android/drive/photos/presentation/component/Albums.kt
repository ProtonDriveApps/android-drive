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

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Card
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.flow.Flow
import me.proton.android.drive.photos.presentation.extension.details
import me.proton.android.drive.photos.presentation.extension.isSelected
import me.proton.android.drive.photos.presentation.extension.thumbnailPainter
import me.proton.android.drive.photos.presentation.state.AlbumsItem
import me.proton.android.drive.photos.presentation.viewevent.AlbumsViewEvent
import me.proton.android.drive.photos.presentation.viewstate.AlbumsViewState
import me.proton.core.compose.theme.ProtonDimens
import me.proton.core.compose.theme.ProtonDimens.ExtraSmallSpacing
import me.proton.core.compose.theme.ProtonTheme
import me.proton.core.crypto.common.pgp.VerificationStatus
import me.proton.core.domain.entity.UserId
import me.proton.core.drive.base.domain.entity.Attributes
import me.proton.core.drive.base.domain.entity.CryptoProperty
import me.proton.core.drive.base.domain.entity.Permissions
import me.proton.core.drive.base.domain.entity.TimestampS
import me.proton.core.drive.base.domain.extension.bytes
import me.proton.core.drive.base.presentation.component.Deferred
import me.proton.core.drive.base.presentation.component.EncryptedItem
import me.proton.core.drive.base.presentation.component.ProtonPullToRefresh
import me.proton.core.drive.base.presentation.component.list.ListEmpty
import me.proton.core.drive.base.presentation.component.list.ListError
import me.proton.core.drive.base.presentation.extension.isPortrait
import me.proton.core.drive.base.presentation.extension.onContent
import me.proton.core.drive.base.presentation.extension.onEmpty
import me.proton.core.drive.base.presentation.extension.onError
import me.proton.core.drive.base.presentation.extension.onLoading
import me.proton.core.drive.drivelink.domain.entity.DriveLink
import me.proton.core.drive.files.presentation.component.files.OfflineIcon
import me.proton.core.drive.files.presentation.extension.LayoutType
import me.proton.core.drive.files.presentation.extension.driveLinkSemantics
import me.proton.core.drive.link.domain.entity.AlbumId
import me.proton.core.drive.link.domain.entity.Link
import me.proton.core.drive.link.domain.entity.LinkId
import me.proton.core.drive.photo.domain.entity.AlbumListing
import me.proton.core.drive.share.domain.entity.ShareId
import me.proton.core.drive.volume.domain.entity.VolumeId
import me.proton.core.drive.base.presentation.R as BasePresentation
import me.proton.core.drive.i18n.R as I18N

@Composable
fun Albums(
    viewState: AlbumsViewState,
    viewEvent: AlbumsViewEvent,
    items: Flow<List<AlbumsItem>>,
    modifier: Modifier = Modifier,
    headerContent: @Composable () -> Unit,
) {
    val albumItems by items.collectAsStateWithLifecycle(
        initialValue = emptyList()
    )
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(ProtonDimens.DefaultSpacing),
    ) {
        if (albumItems.isNotEmpty() || !viewState.filters.isSelected(AlbumListing.Filter.ALL)) {
            AlbumsFilter(viewState.filters, viewEvent.onFilterSelected)
        }
        viewState.listContentState
            .onLoading {
                AlbumsLoading(
                    headerContent = headerContent,
                )
            }
            .onEmpty { state ->
                AlbumsEmpty(
                    isRefreshEnabled = viewState.isRefreshEnabled,
                    isRefreshing = viewState.listContentState.isRefreshing,
                    imageResId = state.imageResId,
                    titleResId = state.titleId,
                    descriptionResId = state.descriptionResId,
                    actionResId = state.actionResId,
                    headerContent = headerContent,
                    onRefresh = viewEvent.onRefresh,
                    onAction = viewEvent.onCreateNewAlbum
                )
            }
            .onError { error ->
                AlbumsError(
                    message = error.message,
                    actionResId = error.actionResId,
                    headerContent = headerContent,
                    onAction = viewEvent.onErrorAction,
                )
            }
            .onContent {
                AlbumsContent(
                    items = albumItems,
                    isRefreshEnabled = viewState.isRefreshEnabled,
                    isRefreshing = viewState.listContentState.isRefreshing,
                    headerContent = headerContent,
                    placeholderImageResId = viewState.placeholderImageResId,
                    onRefresh = viewEvent.onRefresh,
                    onScroll = viewEvent.onScroll,
                    onClick = viewEvent.onDriveLinkAlbum,
                )
            }
    }
}

@Composable
fun AlbumsLoading(
    modifier: Modifier = Modifier,
    headerContent: @Composable () -> Unit,
) {
    Column {
        headerContent()
        Box(
            modifier = modifier.fillMaxSize(),
            contentAlignment = Alignment.Center,
        ) {
            Deferred {
                CircularProgressIndicator()
            }
        }
    }
}

@Composable
fun AlbumsEmpty(
    isRefreshEnabled: Boolean,
    isRefreshing: Boolean,
    @DrawableRes imageResId: Int,
    modifier: Modifier = Modifier,
    @StringRes titleResId: Int = I18N.string.albums_empty_albums_list_screen_title,
    @StringRes descriptionResId: Int? =I18N.string.albums_empty_albums_list_screen_description,
    @StringRes actionResId: Int? = I18N.string.common_create_album_action,
    headerContent: @Composable () -> Unit,
    onRefresh: () -> Unit,
    onAction: () -> Unit,
) {
    ProtonPullToRefresh(
        isPullToRefreshEnabled = isRefreshEnabled,
        isRefreshing = isRefreshing,
        onRefresh = onRefresh,
    ) {
        Column {
            headerContent()
            ListEmpty(
                imageResId = imageResId,
                titleResId = titleResId,
                descriptionResId = descriptionResId,
                actionResId = actionResId,
                modifier = modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState()),
                onAction = onAction,
            )
        }
    }
}

@Composable
fun AlbumsError(
    message: String,
    @StringRes actionResId: Int?,
    modifier: Modifier = Modifier,
    headerContent: @Composable () -> Unit,
    onAction: () -> Unit,
) {
    Column {
        headerContent()
        ListError(
            message = message,
            actionResId = actionResId,
            modifier = modifier,
            onAction = onAction,
        )
    }
}

@Composable
fun AlbumsContent(
    items: List<AlbumsItem>,
    placeholderImageResId: Int,
    isRefreshEnabled: Boolean,
    isRefreshing: Boolean,
    modifier: Modifier = Modifier,
    headerContent: @Composable () -> Unit,
    onScroll: (Set<LinkId>) -> Unit,
    onRefresh: () -> Unit,
    onClick: (DriveLink.Album) -> Unit,
) {
    ProtonPullToRefresh(
        isPullToRefreshEnabled = isRefreshEnabled,
        isRefreshing = isRefreshing,
        onRefresh = onRefresh,
    ) {
        AlbumsContent(
            items = items,
            placeholderImageResId = placeholderImageResId,
            modifier = modifier,
            headerContent = headerContent,
            onScroll = onScroll,
            onClick = onClick,
        )
    }
}

@Composable
fun AlbumsContent(
    items: List<AlbumsItem>,
    placeholderImageResId: Int,
    modifier: Modifier = Modifier,
    headerContent: @Composable () -> Unit,
    onScroll: (Set<LinkId>) -> Unit,
    onClick: (DriveLink.Album) -> Unit,
) {
    val gridState = rememberLazyGridState()
    val firstVisibleItemIndex by remember(gridState) { derivedStateOf { gridState.firstVisibleItemIndex } }
    LaunchedEffect(firstVisibleItemIndex, items) {
        onScroll(
            items
                .takeIf { list -> list.isNotEmpty() && list.size > firstVisibleItemIndex }
                ?.let { list ->
                    val sizeRange = IntRange(0, list.size - 1)
                    val fromIndex = (firstVisibleItemIndex - 10).coerceIn(sizeRange)
                    val toIndex = (firstVisibleItemIndex + 20).coerceIn(sizeRange)
                    list.subList(fromIndex, toIndex + 1)
                        .filterIsInstance<AlbumsItem.Listing>()
                        .flatMap { albumListing ->
                            listOfNotNull(albumListing.id, albumListing.album?.coverLinkId)
                        }
                        .toSet()
                } ?: emptySet(),
        )
    }
    LazyVerticalGrid(
        modifier = modifier.fillMaxSize(),
        columns = PhotosGridCells(minSize = minCoverSize, minCount = 2),
        verticalArrangement = Arrangement.spacedBy(2.dp),
        horizontalArrangement = Arrangement.spacedBy(2.dp),
        state = gridState,
    ) {
        item(
            span = { GridItemSpan(maxLineSpan) }
        ) {
            headerContent()
        }
        items(
            count = items.size,
            key = { index ->
                when (val albumItem = items[index]) {
                    is AlbumsItem.Listing -> albumItem.id.id
                }
            },
            span = { _ ->
                GridItemSpan(1)
            },
        ) { index ->
            AlbumItem(
                albumsItem = items[index],
                placeholderImageResId = placeholderImageResId,
                onClick = onClick,
            )
        }
    }
}

@Composable
fun AlbumItem(
    albumsItem: AlbumsItem,
    placeholderImageResId: Int,
    modifier: Modifier = Modifier,
    onClick: (DriveLink.Album) -> Unit,
) {
    when (albumsItem) {
        is AlbumsItem.Listing -> albumsItem.album?.let { album ->
            AlbumItem(
                album = album,
                cover = albumsItem.coverLink,
                placeholderImageResId = placeholderImageResId,
                modifier = modifier,
                onClick = onClick,
            )
        } ?: AlbumItem(
            albumName = "",
            albumDetails = {},
            isAlbumNameEncrypted = true,
            albumPhotoCount = albumsItem.photoCount,
            modifier = modifier,
            coverDriveLink = albumsItem.coverLink,
            placeholderImageResId = placeholderImageResId,
        )
    }
}

@Composable
fun AlbumItem(
    album: DriveLink.Album,
    placeholderImageResId: Int,
    cover: DriveLink.File?,
    modifier: Modifier = Modifier,
    onClick: (DriveLink.Album) -> Unit,
) {
    val localContext = LocalContext.current
    AlbumItem(
        albumName = album.name,
        albumDetails = {
            AlbumDetails(
                album = album,
                details = album.details(appContext = localContext, useCreationTime = false),
            )
        },
        isAlbumNameEncrypted = album.cryptoName is CryptoProperty.Encrypted,
        albumPhotoCount = album.photoCount,
        modifier = modifier
            .driveLinkSemantics(album, LayoutType.Grid)
            .clickable(onClick = { onClick(album) }),
        coverDriveLink = cover,
        placeholderImageResId = placeholderImageResId,
    )
}

@Composable
fun AlbumItem(
    albumName: String,
    albumDetails: @Composable () -> Unit,
    isAlbumNameEncrypted: Boolean,
    albumPhotoCount: Long,
    placeholderImageResId: Int,
    modifier: Modifier = Modifier,
    coverDriveLink: DriveLink.File? = null,
) {
    val even = albumPhotoCount % 2 == 0L
    val rotation = if (even) 1f else -1f
    Column(
        modifier = modifier
            .padding(
                top = ExtraSmallSpacing,
                bottom = ProtonDimens.DefaultSpacing,
                start = ProtonDimens.DefaultSpacing,
                end = ProtonDimens.DefaultSpacing,
            )
            .fillMaxWidth()
    ) {
        AlbumItemCard(
            coverDriveLink = coverDriveLink,
            rotation = rotation,
            placeholderImageResId = placeholderImageResId,
        )
        Spacer(modifier = Modifier.height(ProtonDimens.SmallSpacing))
        Column(
            modifier = Modifier.padding(ProtonDimens.ExtraSmallSpacing)
        ) {
            Crossfade(
                targetState = isAlbumNameEncrypted,
                modifier = Modifier,
            ) { isEncrypted ->
                if (isEncrypted) {
                    EncryptedItem()
                } else {
                    Text(
                        text = albumName,
                        overflow = TextOverflow.Ellipsis,
                        maxLines = 2,
                        color = ProtonTheme.colors.textNorm,
                        style = ProtonTheme.typography.body2Medium,
                        modifier = Modifier.testTag(ProtonPreviewAlbumItemTestTags.albumName),
                        )
                }
            }
            Spacer(modifier = Modifier.height(ProtonDimens.ExtraSmallSpacing))
            albumDetails()
        }
    }
}

@Composable
fun AlbumDetails(
    album: DriveLink.Album,
    details: String,
    modifier: Modifier = Modifier,
) {
    Row(modifier = modifier) {
        OfflineIcon(modifier = Modifier.padding(end = ExtraSmallSpacing), link = album)
        Text(
            text = details,
            overflow = TextOverflow.Ellipsis,
            maxLines = 1,
            color = ProtonTheme.colors.textWeak,
            style = ProtonTheme.typography.captionRegular,
            modifier = Modifier.testTag(ProtonPreviewAlbumItemTestTags.albumDetails),
        )
    }
}

@Composable
fun AlbumItemCard(
    coverDriveLink: DriveLink.File?,
    placeholderImageResId: Int,
    modifier: Modifier = Modifier,
    rotation: Float = 0f,
) {
    val borderColor = if (ProtonTheme.colors.isDark) {
        ProtonTheme.colors.shade15
    } else {
        ProtonTheme.colors.shade0
    }
    Card(
        shape = RoundedCornerShape(ProtonDimens.LargeCornerRadius),
        border = BorderStroke(3.dp, borderColor),
        elevation = ProtonDimens.SmallSpacing,
        backgroundColor = ProtonTheme.colors.backgroundSecondary,
        contentColor = ProtonTheme.colors.textNorm,
        modifier = modifier
            .aspectRatio(1f)
            .sizeIn(minHeight = minCoverSize, minWidth = minCoverSize)
            .rotate(rotation)
    ) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .testTag(ProtonPreviewAlbumItemTestTags.albumPreviewBox),
            contentAlignment = Alignment.Center,
        ) {
            Crossfade(
                targetState = coverDriveLink == null,
                modifier = modifier,
            ) { showPlaceholder ->
                if (showPlaceholder) {
                    val placeholderSize = if (isPortrait) 52.dp else 48.dp
                    Image(
                        painter = painterResource(placeholderImageResId),
                        contentDescription = null,
                        modifier = Modifier.size(placeholderSize)
                    )
                } else {
                    coverDriveLink?.let { link ->
                        val painterWrapper = link.thumbnailPainter()
                        Image(
                            modifier = Modifier
                                .fillMaxSize(),
                            painter = painterWrapper.painter,
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                        )
                    }
                }
            }
        }
    }
}

internal val minCoverSize: Dp = 150.dp

@Preview
@Composable
private fun PreviewAlbumsEmpty() {
    ProtonTheme {
        AlbumsEmpty(
            isRefreshEnabled = false,
            isRefreshing = false,
            imageResId = BasePresentation.drawable.empty_albums_daynight,
            headerContent = {},
            onRefresh = {},
            onAction = {},
        )
    }
}

@Preview
@Composable
private fun PreviewAlbumItem() {
    ProtonTheme {
        AlbumItem(
            album = DriveLink.Album(
                link = Link.Album(
                    id = AlbumId(ShareId(UserId("user-id"), "share-id"), "album-id"),
                    parentId = null,
                    name = "encrypted_album_name",
                    size = 0.bytes,
                    lastModified = TimestampS(0),
                    mimeType = "Album",
                    isShared = false,
                    key = "",
                    passphrase = "",
                    passphraseSignature = "",
                    numberOfAccesses = 0,
                    shareUrlExpirationTime = null,
                    uploadedBy = "",
                    attributes = Attributes(0),
                    permissions = Permissions(0),
                    state = Link.State.ACTIVE,
                    nameSignatureEmail = null,
                    hash = "",
                    expirationTime = null,
                    nodeKey = "",
                    nodePassphrase = "",
                    nodePassphraseSignature = "",
                    signatureEmail = "",
                    creationTime = TimestampS(0),
                    trashedTime = null,
                    xAttr = null,
                    sharingDetails = null,
                    nodeHashKey = "",
                    isLocked = false,
                    lastActivityTime = TimestampS(0),
                    photoCount = 42,
                    coverLinkId = null,
                ),
                volumeId = VolumeId("volume-id"),
                isMarkedAsOffline = false,
                isAnyAncestorMarkedAsOffline = false,
                downloadState = null,
                trashState = null,
                shareInvitationCount = null,
                shareMemberCount = null,
                cryptoName = CryptoProperty.Decrypted("My album", VerificationStatus.Success),
                cryptoXAttr = CryptoProperty.Decrypted("", VerificationStatus.Success),
                shareUser = null,
                sharePermissions = null,
            ),
            cover = null,
            placeholderImageResId = BasePresentation.drawable.empty_albums_daynight,
            modifier = Modifier,
            onClick = {},
        )
    }
}

object ProtonPreviewAlbumItemTestTags {
    const val albumPreviewBox = "album-preview-box"
    const val albumDetails = "album-details"
    const val albumName = "album-name"
}
