/*
 * Copyright (c) 2022-2023 Proton AG.
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

package me.proton.core.drive.files.presentation.component.common

import androidx.compose.animation.Crossfade
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import me.proton.core.compose.theme.ProtonDimens.DefaultBottomSheetHeaderMinHeight
import me.proton.core.compose.theme.ProtonDimens.DefaultCornerRadius
import me.proton.core.compose.theme.ProtonTheme
import me.proton.core.compose.theme.captionWeak
import me.proton.core.compose.theme.defaultSmallStrong
import me.proton.core.drive.base.presentation.component.EncryptedItem
import me.proton.core.drive.base.presentation.component.text.TextWithMiddleEllipsis

@Composable
internal fun OptionsHeader(
    painter: Painter,
    title: String,
    isTitleEncrypted: Boolean,
    subtitle: String,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Image(
            modifier = Modifier
                .size(HeaderIconSize)
                .clip(RoundedCornerShape(DefaultCornerRadius)),
            painter = painter,
            contentDescription = null
        )
        Column(
            modifier = Modifier
                .defaultMinSize(minHeight = DefaultBottomSheetHeaderMinHeight)
                .padding(start = HeaderSpacing),
            verticalArrangement = Arrangement.Center
        ) {
            Crossfade(targetState = isTitleEncrypted) { isEncrypted ->
                if (isEncrypted) {
                    EncryptedItem()
                } else {
                    TextWithMiddleEllipsis(
                        text = title,
                        style = ProtonTheme.typography.defaultSmallStrong,
                        maxLines = 1,
                    )
                }
            }
            Text(
                text = subtitle,
                style = ProtonTheme.typography.captionWeak,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
internal fun OptionsHeader(
    title: String,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .defaultMinSize(minHeight = DefaultBottomSheetHeaderMinHeight)
            .padding(start = HeaderSpacing),
        contentAlignment = Alignment.CenterStart
    ) {
        TextWithMiddleEllipsis(
            text = title,
            style = ProtonTheme.typography.defaultSmallStrong,
            maxLines = 1,
        )
    }
}

private val HeaderIconSize = 40.dp
private val HeaderSpacing = 12.dp
