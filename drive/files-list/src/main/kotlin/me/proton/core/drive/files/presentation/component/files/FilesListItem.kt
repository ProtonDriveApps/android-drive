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
import androidx.compose.material.Checkbox
import androidx.compose.material.CheckboxDefaults
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
import androidx.compose.ui.draw.scale
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
import me.proton.core.domain.entity.UserId
import me.proton.core.drive.base.domain.entity.Attributes
import me.proton.core.drive.base.domain.entity.Bytes
import me.proton.core.drive.base.domain.entity.Percentage
import me.proton.core.drive.base.domain.entity.Permissions
import me.proton.core.drive.base.domain.entity.TimestampS
import me.proton.core.drive.base.domain.extension.toPercentString
import me.proton.core.drive.base.presentation.component.EncryptedItem
import me.proton.core.drive.base.presentation.component.LinearProgressIndicator
import me.proton.core.drive.base.presentation.component.protonColors
import me.proton.core.drive.base.presentation.component.text.TextWithMiddleEllipsis
import me.proton.core.drive.base.presentation.extension.currentLocale
import me.proton.core.drive.drivelink.domain.entity.DriveLink
import me.proton.core.drive.drivelink.domain.extension.isNameEncrypted
import me.proton.core.drive.link.domain.entity.FileId
import me.proton.core.drive.link.domain.entity.Folder
import me.proton.core.drive.link.domain.entity.FolderId
import me.proton.core.drive.link.domain.entity.Link
import me.proton.core.drive.link.domain.extension.isSharedUrlExpired
import me.proton.core.drive.link.presentation.extension.getSize
import me.proton.core.drive.link.presentation.extension.lastModifiedRelative
import me.proton.core.drive.linkdownload.domain.entity.DownloadState
import me.proton.core.drive.share.domain.entity.ShareId
import me.proton.core.drive.thumbnail.presentation.extension.thumbnailPainter
import me.proton.core.drive.volume.domain.entity.VolumeId
import me.proton.core.drive.base.presentation.R as BasePresentation
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
            .background(color = if (isSelected) ProtonTheme.colors.interactionWeakNorm else Color.Transparent)
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
            Crossfade(isSelected) { isSelected ->
                if (isSelected) {
                    Box(
                        modifier = Modifier
                            .size(IconSize),
                        contentAlignment = Alignment.Center,
                    ) {
                        Checkbox(
                            checked = true,
                            onCheckedChange = null,
                            modifier = Modifier.scale(1.75f),
                            colors = CheckboxDefaults.protonColors(),
                        )
                    }
                } else {
                    Image(
                        modifier = Modifier
                            .size(IconSize)
                            .clip(RoundedCornerShape(DefaultCornerRadius)),
                        painter = link.thumbnailPainter().painter,
                        contentDescription = null,
                    )
                }
            }
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
        modifier = modifier.size(ExtraSmallIconSize),
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
            stringResource(BasePresentation.string.title_modified_with_date, link.lastModifiedRelative(context))
        } else {
            link.downloadStateDescription(progress) ?: "%s, %s".format(
                link.getSize(context),
                stringResource(BasePresentation.string.title_modified_with_date, link.lastModifiedRelative(context)),
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
            stringResource(id = BasePresentation.string.title_downloading)
        } else {
            null
        }
    } else {
        if (downloadState is DownloadState.Downloading) {
            progress?.let {
                stringResource(
                    id = BasePresentation.string.title_percent_downloaded,
                    progress.toPercentString(LocalContext.current.currentLocale)
                )
            } ?: stringResource(id = BasePresentation.string.title_downloading)
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

private val PREVIEW_LINK = Link.File(
    id = FileId(ShareId(UserId("USER_ID"), "SHARE_ID"), "FILE_ID"),
    parentId = FolderId(ShareId(UserId("USER_ID"), "SHARE_ID"), "PARENT_ID"),
    name = "revision_id",
    size = Bytes(0L),
    lastModified = TimestampS(0),
    mimeType = "text/plain",
    isShared = false,
    key = "",
    passphrase = "",
    passphraseSignature = "",
    numberOfAccesses = 0L,
    uploadedBy = "He-Who-Must-Not-Be-Named",
    isFavorite = false,
    hasThumbnail = false,
    activeRevisionId = "",
    xAttr = null,
    contentKeyPacket = "",
    contentKeyPacketSignature = "",
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
    shareUrlId = null,
)
private val PREVIEW_DRIVELINK = DriveLink.File(
    link = PREVIEW_LINK,
    volumeId = VolumeId("VOLUME_ID"),
    isMarkedAsOffline = false,
    isAnyAncestorMarkedAsOffline = false,
    downloadState = null,
    trashState = null,
)

object FilesListItemComponentTestTag {
    const val item = "file list item"
    const val folder = "folder item"
    const val file = "file item"

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
}