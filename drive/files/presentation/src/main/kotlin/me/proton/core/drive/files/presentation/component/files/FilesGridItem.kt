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
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.material.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.flow.Flow
import me.proton.core.compose.flow.rememberFlowWithLifecycle
import me.proton.core.compose.theme.ProtonDimens.DefaultButtonMinHeight
import me.proton.core.compose.theme.ProtonDimens.DefaultIconSize
import me.proton.core.compose.theme.ProtonDimens.ExtraSmallSpacing
import me.proton.core.compose.theme.ProtonDimens.SmallIconSize
import me.proton.core.compose.theme.ProtonDimens.SmallSpacing
import me.proton.core.compose.theme.ProtonTheme
import me.proton.core.compose.theme.defaultSmall
import me.proton.core.drive.base.domain.entity.Percentage
import me.proton.core.drive.base.presentation.component.CircleSelection
import me.proton.core.drive.base.presentation.component.EncryptedItem
import me.proton.core.drive.base.presentation.component.LinearProgressIndicator
import me.proton.core.drive.base.presentation.component.text.TextWithMiddleEllipsis
import me.proton.core.drive.drivelink.domain.entity.DriveLink
import me.proton.core.drive.drivelink.domain.extension.isNameEncrypted
import me.proton.core.drive.files.presentation.component.FilesTestTag
import me.proton.core.drive.linkdownload.domain.entity.DownloadState
import me.proton.core.drive.thumbnail.presentation.extension.thumbnailPainter
import me.proton.core.presentation.R as CorePresentation

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun FilesGridItem(
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
    onRenderThumbnail: (DriveLink) -> Unit,
) {
    val transferProgress = transferProgressFlow?.run {
        rememberFlowWithLifecycle(this).collectAsState(initial = null)
    }?.value
    val showProgress = with(link) {
        isMarkedAsOffline && downloadState is DownloadState.Downloading
    }
    val haptic = LocalHapticFeedback.current
    Box(
        modifier = modifier
            .combinedClickable(
                enabled = onClick != null && isClickEnabled(link),
                onClick = { onClick?.invoke(link) },
                onLongClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    onLongClick(link)
                },
            )
            .background(
                color = if (isSelected) ProtonTheme.colors.backgroundSecondary else Color.Transparent,
                shape = ProtonTheme.shapes.medium,
            )
            .padding(SmallSpacing),
    ) {
        Column(
            modifier = Modifier.align(Alignment.Center)
        ) {
            GridItemImage(
                link = link,
                onRenderThumbnail = onRenderThumbnail,
            ) {
                Crossfade(inMultiselect) { inMultiselect ->
                    if (inMultiselect) {
                        Box(
                            modifier = Modifier.size(DefaultButtonMinHeight),
                            contentAlignment = Alignment.Center,
                        ) {
                            CircleSelection(
                                isSelected = isSelected,
                            )
                        }
                    } else {
                        if (isMoreOptionsEnabled) {
                            GridItemMoreButton(
                                link = link,
                                isSelectingDestination = isSelectingDestination,
                                onMoreOptionsClick = onMoreOptionsClick,
                            )
                        }
                    }
                }
            }
            Row(
                modifier = Modifier.padding(top = SmallSpacing),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                GridDetailsTitle(
                    modifier = Modifier
                        .alignByBaseline()
                        .weight(1f)
                        .padding(end = ExtraSmallSpacing),
                    title = link.name,
                    isTitleEncrypted = link.isNameEncrypted,
                    isEnabled = isTextEnabled(link),
                )
                FavoriteIcon(modifier = Modifier.padding(start = ExtraSmallSpacing), link = link)
                OfflineIcon(modifier = Modifier.padding(start = ExtraSmallSpacing), link = link)
                SharedIcon(modifier = Modifier.padding(start = ExtraSmallSpacing), link = link)
            }
        }
        if (!inMultiselect && showProgress) {
            LinearProgressIndicator(modifier = modifier, progress = transferProgress?.value)
        }
    }
}

@Composable
fun GridDetailsTitle(
    title: String,
    isTitleEncrypted: Boolean,
    modifier: Modifier = Modifier,
    isEnabled: Boolean = true,
) {
    Crossfade(
        targetState = isTitleEncrypted,
        modifier = modifier
    ) { isEncrypted ->
        if (isEncrypted) {
            EncryptedItem()
        } else {
            TextWithMiddleEllipsis(
                text = title,
                style = ProtonTheme.typography.defaultSmall(enabled = isEnabled),
                maxLines = 1,
            )
        }
    }
}

@Composable
fun GridItemImage(
    link: DriveLink,
    modifier: Modifier = Modifier,
    onRenderThumbnail: (DriveLink) -> Unit,
    overlayContent: @Composable () -> Unit,
) {
    Box(
        modifier = modifier
            .defaultMinSize(ImageWidth, ImageHeight)
            .fillMaxWidth()
            .aspectRatio(ImageRatio)
            .border(1.dp, ProtonTheme.colors.separatorNorm, ProtonTheme.shapes.medium)
            .clip(ProtonTheme.shapes.medium)
    ) {
        val painterWrapper = link.thumbnailPainter()
        Image(
            modifier = if (painterWrapper.isLoaded) {
                Modifier.fillMaxSize()
            } else {
                Modifier.align(Alignment.Center)
            }
                .drawWithContent {
                    drawContent()
                    onRenderThumbnail(link)
                },
            painter = painterWrapper.painter,
            contentDescription = null
        )
        Box(modifier = Modifier.align(Alignment.TopEnd)) {
            overlayContent()
        }
    }
}

@Composable
private fun GridItemMoreButton(
    link: DriveLink,
    isSelectingDestination: Boolean,
    modifier: Modifier = Modifier,
    onMoreOptionsClick: (DriveLink) -> Unit,
) {
    if (!isSelectingDestination) {
        if (link.isProcessing.not()) {
            Box(
                modifier = modifier
                    .testTag(FilesTestTag.moreButton)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = ripple(bounded = false, radius = DefaultIconSize / 2),
                        role = Role.Button,
                    ) { onMoreOptionsClick(link) }
                    .size(DefaultButtonMinHeight)
                    .padding(MoreButtonPadding)
                    .clip(CircleShape)
                    .background(ProtonTheme.colors.backgroundNorm.copy(alpha = MoreButtonAlpha)),
            ) {
                Icon(
                    modifier = Modifier
                        .size(SmallIconSize)
                        .align(Alignment.Center),
                    painter = painterResource(id = CorePresentation.drawable.ic_proton_three_dots_vertical),
                    contentDescription = null,
                    tint = ProtonTheme.colors.interactionStrongNorm
                )
            }
        } else {
            CircularProgressIndicator(
                modifier = modifier
                    .padding(MoreButtonPadding)
                    .size(DefaultIconSize),
                strokeWidth = 1.dp,
            )
        }
    }
}

private val ImageHeight = 96.dp
private val ImageWidth = 158.dp
private val ImageRatio = ImageWidth/ImageHeight
private val MoreButtonPadding = 12.dp
private const val MoreButtonAlpha = 0.7f

@Preview(
    name = "GridItem not downloaded, not favorite, not shared in dark mode",
    group = "dark mode",
    uiMode = Configuration.UI_MODE_NIGHT_YES,
    showBackground = true,
)
@Suppress("unused")
@Composable
fun PreviewGridItem() {
    ProtonTheme {
        Surface(modifier = Modifier.background(MaterialTheme.colors.background)) {
            FilesGridItem(
                link = PREVIEW_DRIVELINK,
                onClick = {},
                onLongClick = {},
                onMoreOptionsClick = {},
                isClickEnabled = { false },
                isTextEnabled = { true },
                onRenderThumbnail = {},
            )
        }
    }
}

@Preview(
    name = "GridItem in multi select, selected in dark mode",
    group = "dark mode",
    uiMode = Configuration.UI_MODE_NIGHT_YES,
    showBackground = true,
)
@Suppress("unused")
@Composable
fun PreviewSelectedGridItem() {
    ProtonTheme {
        Surface(modifier = Modifier.background(MaterialTheme.colors.background)) {
            FilesGridItem(
                link = PREVIEW_DRIVELINK,
                onClick = {},
                onLongClick = {},
                onMoreOptionsClick = {},
                isClickEnabled = { false },
                isTextEnabled = { true },
                isSelected = true,
                inMultiselect = true,
                onRenderThumbnail = {}
            )
        }
    }
}

@Preview(
    name = "GridItem in multi select, unselected in dark mode",
    group = "dark mode",
    uiMode = Configuration.UI_MODE_NIGHT_YES,
    showBackground = true,
)
@Suppress("unused")
@Composable
fun PreviewUnselectedGridItem() {
    ProtonTheme {
        Surface(modifier = Modifier.background(MaterialTheme.colors.background)) {
            FilesGridItem(
                link = PREVIEW_DRIVELINK,
                onClick = {},
                onLongClick = {},
                onMoreOptionsClick = {},
                isClickEnabled = { false },
                isTextEnabled = { true },
                isSelected = false,
                inMultiselect = true,
                onRenderThumbnail = {},
            )
        }
    }
}
