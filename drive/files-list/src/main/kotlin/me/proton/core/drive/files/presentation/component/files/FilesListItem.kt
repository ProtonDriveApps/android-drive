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
import androidx.compose.foundation.layout.fillMaxWidth
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
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.flow.Flow
import me.proton.core.compose.flow.rememberFlowWithLifecycle
import me.proton.core.compose.theme.ProtonDimens.DefaultButtonMinHeight
import me.proton.core.compose.theme.ProtonDimens.DefaultCornerRadius
import me.proton.core.compose.theme.ProtonDimens.DefaultIconSize
import me.proton.core.compose.theme.ProtonDimens.DefaultSpacing
import me.proton.core.compose.theme.ProtonDimens.ExtraSmallSpacing
import me.proton.core.compose.theme.ProtonDimens.SmallSpacing
import me.proton.core.compose.theme.ProtonTheme
import me.proton.core.compose.theme.captionWeak
import me.proton.core.compose.theme.default
import me.proton.core.drive.base.domain.entity.Percentage
import me.proton.core.drive.base.domain.extension.toPercentString
import me.proton.core.drive.base.presentation.component.EncryptedItem
import me.proton.core.drive.base.presentation.component.LinearProgressIndicator
import me.proton.core.drive.base.presentation.component.text.TextWithMiddleEllipsis
import me.proton.core.drive.base.presentation.extension.currentLocale
import me.proton.core.drive.drivelink.domain.entity.DriveLink
import me.proton.core.drive.drivelink.domain.extension.isNameEncrypted
import me.proton.core.drive.link.domain.entity.Folder
import me.proton.core.drive.link.domain.extension.isSharedUrlExpired
import me.proton.core.drive.link.presentation.extension.getSize
import me.proton.core.drive.link.presentation.extension.lastModifiedRelative
import me.proton.core.drive.linkdownload.domain.entity.DownloadState
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
        ) {

            Image(
                modifier = Modifier
                    .testTag(FilesListItemComponentTestTag.thumbnail(link))
                    .size(IconSize)
                    .clip(RoundedCornerShape(DefaultCornerRadius)),
                painter = link.thumbnailPainter().painter,
                contentDescription = null,
            )
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
                            MoreOptions(
                                modifier = Modifier.size(DefaultButtonMinHeight),
                                link = link,
                                onClick = onMoreOptionsClick,
                            )
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
                modifier = Modifier.testTag(FilesListItemComponentTestTag.item)
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
    if (!(link.isMarkedAsOffline || link.isAnyAncestorMarkedAsOffline)) {
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
    if (!link.isShared || link.isSharedUrlExpired) {
        return
    }
    Icon(
        modifier = modifier
            .size(ExtraSmallIconSize)
            .testTag(FilesListItemComponentTestTag.itemWithSharedIcon),
        painter = painterResource(id = CorePresentation.drawable.ic_proton_link),
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
    val context = LocalContext.current
    Text(
        modifier = modifier.alignByBaseline(),
        text = if (link is Folder) {
            stringResource(I18N.string.title_modified_with_date, link.lastModifiedRelative(context))
        } else {
            link.downloadStateDescription(progress) ?: "%s, %s".format(
                link.getSize(context),
                stringResource(I18N.string.title_modified_with_date, link.lastModifiedRelative(context)),
            )
        },
        style = ProtonTheme.typography.captionWeak,
    )
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

@Composable
fun MoreOptions(
    link: DriveLink,
    modifier: Modifier = Modifier,
    onClick: (DriveLink) -> Unit,
) {
    IconButton(
        modifier = modifier.testTag(FilesListItemComponentTestTag.threeDotsButton(link)),
        onClick = { onClick(link) }
    ) {
        Icon(
            painter = painterResource(id = CorePresentation.drawable.ic_proton_three_dots_vertical),
            contentDescription = null,
            tint = ProtonTheme.colors.interactionStrongNorm
        )
    }
}

@Preview(
    name = "ListItem not downloaded, not favorite, not shared in light mode",
    group = "light mode",
    uiMode = Configuration.UI_MODE_NIGHT_NO,
    showBackground = true,
)
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
@Preview(
    name = "ListItem in multiselect, selected,  in light mode",
    group = "light mode",
    uiMode = Configuration.UI_MODE_NIGHT_NO,
    showBackground = true,
)
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
    name = "ListItem in multiselect, unselected, in light mode",
    group = "light mode",
    uiMode = Configuration.UI_MODE_NIGHT_NO,
    showBackground = true,
)
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
    name = "ListItem downloaded but not favorite, not shared in light mode",
    group = "light mode",
    uiMode = Configuration.UI_MODE_NIGHT_NO,
    showBackground = true,
)
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
                    downloadState = DownloadState.Downloaded(emptyList()),
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

@Preview(
    name = "ListItem downloading but not favorite, not shared in light mode",
    group = "light mode",
    uiMode = Configuration.UI_MODE_NIGHT_NO,
    showBackground = true,
)
@Preview(
    name = "ListItem downloading but not favorite, not shared in dark mode",
    group = "dark mode",
    uiMode = Configuration.UI_MODE_NIGHT_YES,
    showBackground = true,
)
@Suppress("unused")
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

@Preview(
    name = "ListItem downloaded and favorite but not shared in light mode",
    group = "light mode",
    uiMode = Configuration.UI_MODE_NIGHT_NO,
    showBackground = true,
)
@Preview(
    name = "ListItem downloaded and favorite but not shared in dark mode",
    group = "dark mode",
    uiMode = Configuration.UI_MODE_NIGHT_YES,
    showBackground = true,
)
@Suppress("unused")
@Composable
fun PreviewDownloadedAndFavoriteListItem() {
    ProtonTheme {
        Surface(modifier = Modifier.background(MaterialTheme.colors.background)) {
            FilesListItem(
                link = PREVIEW_DRIVELINK.copy(
                    isMarkedAsOffline = true,
                    downloadState = DownloadState.Downloaded(emptyList()),
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

@Preview(
    name = "ListItem downloaded, favorite and shared in light mode",
    group = "light mode",
    uiMode = Configuration.UI_MODE_NIGHT_NO,
    showBackground = true,
)
@Preview(
    name = "ListItem downloaded, favorite and shared in dark mode",
    group = "dark mode",
    uiMode = Configuration.UI_MODE_NIGHT_YES,
    showBackground = true,
)
@Suppress("unused", "FunctionMaxLength")
@Composable
fun PreviewDownloadedAndFavoriteSharedListItem() {
    ProtonTheme {
        Surface(modifier = Modifier.background(MaterialTheme.colors.background)) {
            FilesListItem(
                link = PREVIEW_DRIVELINK.copy(
                    isMarkedAsOffline = true,
                    downloadState = DownloadState.Downloaded(emptyList()),
                    link = PREVIEW_LINK.copy(
                        isShared = true,
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

val IconSize = DefaultButtonMinHeight
val StartPadding = DefaultSpacing
val EndPadding = SmallSpacing
val VerticalPadding = 14.dp
val Height = 68.dp
val TitleStartPadding = 20.dp
val ExtraSmallIconSize = 12.dp

object FilesListItemComponentTestTag {
    const val item = "file list item"
    const val folder = "folder item"
    const val file = "file item"
    const val imageWithThumbnail = "image with thumbnail"
    const val imageWithoutThumbnail = "image without thumbnail"
    const val itemWithSharedIcon = "item with shared icon"

    enum class ItemType {
        Folder,
        File
    }

    fun threeDotsButton(itemType: ItemType): String {
        val item = when (itemType) {
            ItemType.Folder -> folder
            ItemType.File -> file
        }
        return "three dots - $item"
    }

    fun threeDotsButton(link: DriveLink): String = threeDotsButton(
        when (link) {
            is DriveLink.File -> ItemType.File
            is DriveLink.Folder -> ItemType.Folder
        }
    )

    fun thumbnail(link: DriveLink) = when (link) {
        is DriveLink.File -> if (link.hasThumbnail) {
            imageWithThumbnail
        } else {
            imageWithoutThumbnail
        }
        else -> imageWithoutThumbnail
    }
}