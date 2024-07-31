/*
 * Copyright (c) 2024 Proton AG.
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

package me.proton.core.drive.log.presentation.component

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.Icon
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import me.proton.core.compose.theme.ProtonDimens.DefaultSpacing
import me.proton.core.compose.theme.ProtonDimens.MediumSpacing
import me.proton.core.compose.theme.ProtonDimens.SmallSpacing
import me.proton.core.compose.theme.ProtonTheme
import me.proton.core.compose.theme.captionNorm
import me.proton.core.compose.theme.captionStrongNorm
import me.proton.core.compose.theme.captionWeak
import me.proton.core.drive.base.presentation.extension.conditional
import me.proton.core.presentation.R as CorePresentation

@Composable
fun LogItem(
    time: String,
    message: String,
    modifier: Modifier = Modifier,
    content: String? = null,
    messageStyle: TextStyle = ProtonTheme.typography.captionStrongNorm,
) {
    var isMoreContentExpanded by rememberSaveable { mutableStateOf(false) }
    Column(
        modifier = modifier
            .fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .conditional(content != null) {
                    clickable {
                        isMoreContentExpanded = !isMoreContentExpanded
                    }
                }
                .heightIn(min = MediumSpacing)
                .padding(horizontal = SmallSpacing),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = time,
                style = ProtonTheme.typography.captionNorm,
            )
            Spacer(modifier = Modifier.width(DefaultSpacing))
            Text(
                text = message,
                style = messageStyle,
                modifier = Modifier.weight(1f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (content != null) {
                MoreIndicator(isMoreContentExpanded)
            }
        }
        if (content != null && isMoreContentExpanded) {
            Text(
                text = content,
                style = ProtonTheme.typography.captionWeak,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = DefaultSpacing),
            )
        }
    }
}

@Composable
private fun MoreIndicator(
    isExpanded: Boolean,
    modifier: Modifier = Modifier,
) {
    val painter = painterResource(
        id = if (isExpanded) {
            CorePresentation.drawable.ic_proton_chevron_up
        } else {
            CorePresentation.drawable.ic_proton_chevron_down
        }
    )
    Icon(
        painter = painter,
        contentDescription = null,
        modifier = modifier
            .scale(0.5f),
    )
}

@Preview
@Composable
fun PreviewLogItem() {
    ProtonTheme {
        Surface {
            LogItem(
                time = "13:28:24.321",
                message = "Backup started",
                content = "Photos will soon be ready for backup",
            )
        }
    }
}
