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
package me.proton.core.drive.files.presentation.component.files

import android.content.res.Configuration
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.flow.Flow
import me.proton.core.compose.flow.rememberFlowWithLifecycle
import me.proton.core.compose.theme.ProtonDimens.DefaultButtonMinHeight
import me.proton.core.compose.theme.ProtonDimens.DefaultCornerRadius
import me.proton.core.compose.theme.ProtonDimens.DefaultIconSize
import me.proton.core.compose.theme.ProtonDimens.DefaultSpacing
import me.proton.core.compose.theme.ProtonDimens.ExtraSmallSpacing
import me.proton.core.compose.theme.ProtonDimens.MediumSpacing
import me.proton.core.compose.theme.ProtonDimens.SmallSpacing
import me.proton.core.compose.theme.ProtonTheme
import me.proton.core.compose.theme.captionNorm
import me.proton.core.compose.theme.captionWeak
import me.proton.core.compose.theme.default
import me.proton.core.crypto.common.pgp.VerificationStatus
import me.proton.core.drive.base.domain.entity.CryptoProperty
import me.proton.core.drive.base.domain.entity.Percentage
import me.proton.core.drive.base.domain.extension.firstCodePointAsStringOrNull
import me.proton.core.drive.base.domain.extension.toPercentString
import me.proton.core.drive.base.presentation.component.CircleSelection
import me.proton.core.drive.base.presentation.component.EncryptedItem
import me.proton.core.drive.base.presentation.component.LetterBadge
import me.proton.core.drive.base.presentation.component.LinearProgressIndicator
import me.proton.core.drive.base.presentation.component.text.TextWithMiddleEllipsis
import me.proton.core.drive.base.presentation.extension.asHumanReadableString
import me.proton.core.drive.base.presentation.extension.currentLocale
import me.proton.core.drive.drivelink.domain.entity.DriveLink
import me.proton.core.drive.drivelink.domain.extension.isNameEncrypted
import me.proton.core.drive.drivelink.domain.extension.isSharedByLinkOrWithUsers
import me.proton.core.drive.files.presentation.component.FilesTestTag
import me.proton.core.drive.link.domain.entity.Folder
import me.proton.core.drive.link.domain.entity.SharingDetails
import me.proton.core.drive.link.domain.extension.isProtonCloudFile
import me.proton.core.drive.link.domain.extension.userId
import me.proton.core.drive.link.presentation.extension.getSize
import me.proton.core.drive.link.presentation.extension.lastModifiedRelative
import me.proton.core.drive.linkdownload.domain.entity.DownloadState
import me.proton.core.drive.share.domain.entity.ShareId
import me.proton.core.drive.share.user.domain.entity.ShareUser
import me.proton.core.drive.shareurl.base.domain.entity.ShareUrlId
import me.proton.core.drive.thumbnail.presentation.extension.thumbnailPainter
import me.proton.core.drive.base.presentation.R as BasePresentation
import me.proton.core.drive.i18n.R as I18N
import me.proton.core.presentation.R as CorePresentation

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun FilesListItem(
    link: DriveLink,
    onClick: ((DriveLink) -> Unit)?,
    onLongClick: (DriveLink) -> Unit,
    onMoreOptionsClick: (DriveLink) -> Unit,
    modifier: Modifier = Modifier,
    isSelectingDestination: Boolean = false,
    isClickEnabled: (DriveLink) -> Boolean,
    isTextEnabled: (DriveLink) -> Boolean,
    transferProgressFlow: Flow<Percentage>? = null,
    isSelected: Boolean = false,
    inMultiselect: Boolean = false,
    isMoreOptionsEnabled: Boolean = true,
) {
    val transferProgress = transferProgressFlow?.let {
        rememberFlowWithLifecycle(transferProgressFlow).collectAsState(initial = null)
    }?.value
    val showProgress = with(link) {
        (isMarkedAsOffline || isAnyAncestorMarkedAsOffline) && downloadState is DownloadState.Downloading
    }
    val haptic = LocalHapticFeedback.current
    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(color = if (isSelected) ProtonTheme.colors.backgroundSecondary else Color.Transparent)
            .combinedClickable(
                enabled = onClick != null && isClickEnabled(link),
                onClick = { onClick?.invoke(link) },
                onLongClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    onLongClick(link)
                },
            ),
        contentAlignment = Alignment.BottomStart
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = StartPadding, end = EndPadding)
                .padding(vertical = VerticalPadding),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            ListItemIcon(link = link)
            Details(
                modifier = Modifier
                    .weight(1f)
                    .padding(start = TitleStartPadding),
                link = link,
                isTextEnabled = isTextEnabled,
                progress = transferProgress
            )
            Crossfade(inMultiselect) { inMultiselect ->
                if (!inMultiselect) {
                    if (!isSelectingDestination) {
                        if (link.isProcessing.not()) {
                            if (isMoreOptionsEnabled) {
                                MoreOptions(
                                    modifier = Modifier.size(DefaultButtonMinHeight),
                                    link = link,
                                    onClick = onMoreOptionsClick,
                                )
                            } else {
                                Spacer(modifier = Modifier.size(SmallSpacing))
                            }
                        } else {
                            CircularProgressIndicator(
                                modifier = Modifier
                                    .padding(end = DefaultSpacing)
                                    .size(DefaultIconSize)
                                    .align(Alignment.CenterVertically),
                                strokeWidth = 1.dp,
                            )
                        }
                    }
                } else {
                    Crossfade(isSelected) { isSelected ->
                        Box(
                            modifier = Modifier.size(DefaultButtonMinHeight),
                            contentAlignment = Alignment.Center,
                        ) {
                            CircleSelection(isSelected)
                        }
                    }
                }
            }
        }
        if (!inMultiselect && showProgress) {
            LinearProgressIndicator(modifier = modifier, progress = transferProgress?.value)
        }
    }
}

@Composable
fun FilesListItemPlaceholder(
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(start = StartPadding, end = EndPadding)
            .padding(vertical = VerticalPadding),
    ) {
        Box(
            modifier = Modifier
                .size(IconSize)
                .clip(RoundedCornerShape(DefaultCornerRadius))
                .background(ProtonTheme.colors.backgroundSecondary)
        )
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(start = TitleStartPadding)
                .align(Alignment.CenterVertically),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(fraction = 0.8f)
                    .height(18.dp)
                    .background(
                        color = ProtonTheme.colors.backgroundSecondary,
                        shape = ProtonTheme.shapes.small,
                    )
            )
            Box(
                modifier = Modifier
                    .padding(top = SmallSpacing)
                    .fillMaxWidth(fraction = 0.6f)
                    .height(14.dp)
                    .background(
                        color = ProtonTheme.colors.backgroundSecondary,
                        shape = ProtonTheme.shapes.small,
                    )
            )
        }
    }
}

@Composable
fun ListItemIcon(
    link: DriveLink,
    modifier: Modifier = Modifier,
) {
    val member = link.shareUser?.takeUnless { it.permissions.isAdmin }
    Box(
        modifier = modifier,
    ) {
        Image(
            modifier = Modifier
                .size(IconSize)
                .clip(RoundedCornerShape(DefaultCornerRadius)),
            painter = link.thumbnailPainter().painter,
            contentDescription = null,
        )
        if (member != null) {
            LetterBadge(member.inviter)
        }
    }
}

@Composable
fun Details(
    link: DriveLink,
    progress: Percentage?,
    isTextEnabled: (DriveLink) -> Boolean,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth()
    ) {
        DetailsTitle(
            title = link.name,
            isTitleEncrypted = link.isNameEncrypted,
            isEnabled = isTextEnabled(link),
        )
        DetailsSubtitle(
            modifier = Modifier.padding(top = ExtraSmallSpacing),
            link = link,
            progress = progress,
        )
    }
}

@Composable
fun DetailsTitle(
    title: String,
    isTitleEncrypted: Boolean,
    modifier: Modifier = Modifier,
    isEnabled: Boolean = true,
) {
    Crossfade(
        targetState = isTitleEncrypted,
        modifier = modifier,
    ) { isEncrypted ->
        if (isEncrypted) {
            EncryptedItem()
        } else {
            TextWithMiddleEllipsis(
                text = title,
                style = ProtonTheme.typography.default(enabled = isEnabled),
                maxLines = 1,
                modifier = Modifier.testTag(FilesTestTag.listDetailsTitle)
            )
        }
    }
}

@Composable
fun DetailsSubtitle(
    link: DriveLink,
    progress: Percentage?,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        FavoriteIcon(modifier = Modifier.padding(end = ExtraSmallSpacing), link = link)
        OfflineIcon(modifier = Modifier.padding(end = ExtraSmallSpacing), link = link)
        SharedIcon(modifier = Modifier.padding(end = ExtraSmallSpacing), link = link)
        DetailsDescription(link = link, progress = progress)
    }
}

@Composable
fun FavoriteIcon(
    link: DriveLink,
    modifier: Modifier = Modifier,
) {
    if (!link.isFavorite) {
        return
    }
    Icon(
        modifier = modifier.size(ExtraSmallIconSize),
        painter = painterResource(id = CorePresentation.drawable.ic_proton_star_filled),
        tint = ProtonTheme.colors.iconNorm,
        contentDescription = null
    )
}

@Composable
fun OfflineIcon(
    link: DriveLink,
    modifier: Modifier = Modifier,
) {
    if (!(link.isMarkedAsOffline || link.isAnyAncestorMarkedAsOffline) || link.isProtonCloudFile) {
        return
    }
    when (link.downloadState) {
        is DownloadState.Downloaded -> Icon(
            modifier = modifier.size(ExtraSmallIconSize),
            painter = painterResource(id = BasePresentation.drawable.ic_status_downloaded),
            tint = ProtonTheme.colors.iconWeak,
            contentDescription = null
        )
        is DownloadState.Downloading -> CircularProgressIndicator(
            modifier = modifier.size(ExtraSmallIconSize),
            strokeWidth = 1.dp,
        )
        else -> Icon(
            modifier = modifier.size(ExtraSmallIconSize),
            painter = painterResource(id = BasePresentation.drawable.ic_status_waiting_for_download),
            tint = ProtonTheme.colors.iconWeak,
            contentDescription = null
        )
    }
}

@Composable
fun SharedIcon(
    link: DriveLink,
    modifier: Modifier = Modifier,
) {
    if (!link.isSharedByLinkOrWithUsers) {
        return
    }
    Icon(
        modifier = modifier
            .size(ExtraSmallIconSize)
            .testTag(FilesTestTag.itemWithSharedIcon),
        painter = painterResource(id = CorePresentation.drawable.ic_proton_users),
        tint = ProtonTheme.colors.iconNorm,
        contentDescription = null
    )
}

@Composable
fun RowScope.DetailsDescription(
    link: DriveLink,
    progress: Percentage?,
    modifier: Modifier = Modifier,
) {
    Text(
        modifier = modifier.alignByBaseline(),
        text = when (link) {
            is DriveLink.Folder -> link.detailsDescription()
            is DriveLink.File -> link.detailsDescription(progress)
            is DriveLink.Album -> link.detailsDescription()
        },
        style = ProtonTheme.typography.captionWeak,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
    )
}

@Composable
fun DriveLink.Folder.detailsDescription(): String =
    shareUser?.takeUnless { it.permissions.isAdmin }?.description
        ?: stringResource(I18N.string.title_modified_with_date, lastModifiedRelative(LocalContext.current))

@Composable
fun DriveLink.Album.detailsDescription(): String =
    shareUser?.takeUnless { it.permissions.isAdmin }?.description
        ?: stringResource(I18N.string.title_modified_with_date, lastModifiedRelative(LocalContext.current))

@Composable
fun DriveLink.File.detailsDescription(progress: Percentage?): String =
    downloadStateDescription(progress)
        ?: let {
            val context = LocalContext.current
            shareUser?.takeUnless { it.permissions.isAdmin }?.description
                ?: let {
                    val lastModified = stringResource(I18N.string.title_modified_with_date, lastModifiedRelative(context))
                    if (isProtonCloudFile) {
                        "%s".format(lastModified)
                    } else {
                        "%s, %s".format(
                            getSize(context),
                            lastModified,
                        )
                    }
                }
        }

@Composable
fun DriveLink.downloadStateDescription(progress: Percentage?): String? {
    if (!(isMarkedAsOffline || isAnyAncestorMarkedAsOffline)) {
        return null
    }
    return if (this is Folder) {
        if (downloadState == DownloadState.Downloading) {
            stringResource(id = I18N.string.common_downloading)
        } else {
            null
        }
    } else {
        if (downloadState is DownloadState.Downloading) {
            progress?.let {
                stringResource(
                    id = I18N.string.common_percent_downloaded,
                    progress.toPercentString(LocalContext.current.currentLocale)
                )
            } ?: stringResource(id = I18N.string.common_downloading)
        } else null
    }
}

val ShareUser.description: String get() = "${displayName ?: inviter} \u2022 ${createTime.asHumanReadableString()}"

@Composable
fun MoreOptions(
    link: DriveLink,
    modifier: Modifier = Modifier,
    onClick: (DriveLink) -> Unit,
) {
    IconButton(
        modifier = modifier.testTag(FilesTestTag.moreButton),
        onClick = { onClick(link) }
    ) {
        Icon(
            painter = painterResource(id = CorePresentation.drawable.ic_proton_three_dots_vertical),
            contentDescription = null,
            tint = ProtonTheme.colors.interactionStrongNorm
        )
    }
}

@Preview
@Composable
fun PreviewFilesListItemPlaceholder() {
    ProtonTheme {
        Column(modifier = Modifier.background(MaterialTheme.colors.background)) {
            FilesListItemPlaceholder()
            FilesListItem(
                link = PREVIEW_DRIVELINK_FOLDER_SHARED_WITH_ME,
                onClick = {},
                onLongClick = {},
                onMoreOptionsClick = {},
                isClickEnabled = { false },
                isTextEnabled = { true },
            )
        }
    }
}

@Preview(
    name = "ListItem not downloaded, not favorite, not shared in dark mode",
    group = "dark mode",
    uiMode = Configuration.UI_MODE_NIGHT_YES,
    showBackground = true,
)
@Suppress("unused")
@Composable
fun PreviewListItem() {
    ProtonTheme {
        Surface(modifier = Modifier.background(MaterialTheme.colors.background)) {
            FilesListItem(
                link = PREVIEW_DRIVELINK,
                onClick = {},
                onLongClick = {},
                onMoreOptionsClick = {},
                isClickEnabled = { false },
                isTextEnabled = { true },
            )
        }
    }
}

@Preview
@Composable
fun PreviewSharedWithMeListItems() {
    ProtonTheme {
        Surface(modifier = Modifier.background(MaterialTheme.colors.background)) {
            Column {
                FilesListItem(
                    link = PREVIEW_DRIVELINK_FOLDER_SHARED_WITH_ME,
                    onClick = {},
                    onLongClick = {},
                    onMoreOptionsClick = {},
                    isClickEnabled = { false },
                    isTextEnabled = { true },
                )
                FilesListItem(
                    link = PREVIEW_DRIVELINK_SHARED_WITH_ME,
                    onClick = {},
                    onLongClick = {},
                    onMoreOptionsClick = {},
                    isClickEnabled = { false },
                    isTextEnabled = { true },
                )
            }
        }
    }
}

@Preview(
    name = "ListItem in multiselect, selected,  in dark mode",
    group = "dark mode",
    uiMode = Configuration.UI_MODE_NIGHT_YES,
    showBackground = true,
)
@Suppress("unused")
@Composable
fun PreviewSelectedListItem() {
    ProtonTheme {
        Surface(modifier = Modifier.background(MaterialTheme.colors.background)) {
            FilesListItem(
                link = PREVIEW_DRIVELINK,
                onClick = {},
                onLongClick = {},
                onMoreOptionsClick = {},
                isClickEnabled = { false },
                isTextEnabled = { true },
                isSelected = true,
                inMultiselect = true,
            )
        }
    }
}

@Preview(
    name = "ListItem in multiselect, unselected, in dark mode",
    group = "dark mode",
    uiMode = Configuration.UI_MODE_NIGHT_YES,
    showBackground = true,
)
@Suppress("unused")
@Composable
fun PreviewUnselectedListItem() {
    ProtonTheme {
        Surface(modifier = Modifier.background(MaterialTheme.colors.background)) {
            FilesListItem(
                link = PREVIEW_DRIVELINK,
                onClick = {},
                onLongClick = {},
                onMoreOptionsClick = {},
                isClickEnabled = { false },
                isTextEnabled = { true },
                isSelected = false,
                inMultiselect = true,
            )
        }
    }
}

@Preview(
    name = "ListItem downloaded but not favorite, not shared in dark mode",
    group = "dark mode",
    uiMode = Configuration.UI_MODE_NIGHT_YES,
    showBackground = true,
)
@Suppress("unused")
@Composable
fun PreviewDownloadedListItem() {
    ProtonTheme {
        Surface(modifier = Modifier.background(MaterialTheme.colors.background)) {
            FilesListItem(
                link = PREVIEW_DRIVELINK.copy(
                    isMarkedAsOffline = true,
                    downloadState = DownloadState.Downloaded(),
                ),
                onClick = {},
                onLongClick = {},
                onMoreOptionsClick = {},
                isClickEnabled = { false },
                isTextEnabled = { true },
            )
        }
    }
}


@Preview
@Composable
fun PreviewDownloadingListItem() {
    ProtonTheme {
        Surface(modifier = Modifier.background(MaterialTheme.colors.background)) {
            FilesListItem(
                link = PREVIEW_DRIVELINK.copy(
                    isMarkedAsOffline = true,
                    downloadState = DownloadState.Downloading,
                ),
                onClick = {},
                onLongClick = {},
                onMoreOptionsClick = {},
                isClickEnabled = { false },
                isTextEnabled = { true },
            )
        }
    }
}

@Preview
@Composable
fun PreviewDownloadedAndFavoriteListItem() {
    ProtonTheme {
        Surface(modifier = Modifier.background(MaterialTheme.colors.background)) {
            FilesListItem(
                link = PREVIEW_DRIVELINK.copy(
                    isMarkedAsOffline = true,
                    downloadState = DownloadState.Downloaded(),
                    link = PREVIEW_LINK.copy(
                        isFavorite = true,
                    )
                ),
                onClick = {},
                onLongClick = {},
                onMoreOptionsClick = {},
                isClickEnabled = { false },
                isTextEnabled = { true },
            )
        }
    }
}

@Preview
@Composable
fun PreviewDwnldAndFavShrdListItem() {
    ProtonTheme {
        Surface(modifier = Modifier.background(MaterialTheme.colors.background)) {
            FilesListItem(
                link = PREVIEW_DRIVELINK.copy(
                    isMarkedAsOffline = true,
                    downloadState = DownloadState.Downloaded(),
                    link = PREVIEW_LINK.copy(
                        isShared = true,
                        sharingDetails = SharingDetails(
                            ShareId(PREVIEW_LINK.userId, ""),
                            shareUrlId = ShareUrlId(ShareId(PREVIEW_LINK.userId, ""), "")
                        ),
                        isFavorite = true,
                    )
                ),
                onClick = {},
                onLongClick = {},
                onMoreOptionsClick = {},
                isClickEnabled = { false },
                isTextEnabled = { true },
            )
        }
    }
}

@Preview
@Composable
fun PreviewItemsWithoutMoreOptions() {
    ProtonTheme {
        Column {
            FilesListItem(
                link = PREVIEW_DRIVELINK_FOLDER.copy(
                    cryptoName = CryptoProperty.Decrypted("folder synced from Domantas machine - Copy", VerificationStatus.Success),
                ),
                onClick = {},
                onLongClick = {},
                onMoreOptionsClick = {},
                isClickEnabled = { false },
                isTextEnabled = { true },
                isMoreOptionsEnabled = false,
            )
            FilesListItem(
                link = PREVIEW_DRIVELINK_FOLDER.copy(
                    cryptoName = CryptoProperty.Decrypted("folder synced from Domantas machine", VerificationStatus.Success),
                ),
                onClick = {},
                onLongClick = {},
                onMoreOptionsClick = {},
                isClickEnabled = { false },
                isTextEnabled = { true },
                isMoreOptionsEnabled = false,
            )
        }
    }
}

val IconSize = DefaultButtonMinHeight
val StartPadding = DefaultSpacing
val EndPadding = SmallSpacing
val VerticalPadding = 14.dp
val Height = 68.dp
val TitleStartPadding = 20.dp
val ExtraSmallIconSize = 12.dp
