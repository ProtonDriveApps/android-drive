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

package me.proton.core.drive.base.presentation.component

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.datasource.CollectionPreviewParameterProvider
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import me.proton.core.compose.theme.ProtonTheme
import me.proton.core.presentation.R as CorePresentation

@Composable
fun BoxWithNotificationDot(
    modifier: Modifier = Modifier,
    notificationDotVisible: Boolean = false,
    horizontalOffset: Dp = 0.dp,
    verticalOffset: Dp = 0.dp,
    content: @Composable (Modifier) -> Unit
) {
    Box(modifier = modifier) {
        content(Modifier.align(Alignment.Center))
        if (notificationDotVisible) {
            NotificationDot(
                Modifier
                    .align(Alignment.TopEnd)
                    .offset(x = horizontalOffset, y = verticalOffset)
            )
        }
    }
}


@Preview
@Composable
fun PreviewIcon() {
    ProtonTheme {
        Surface(modifier = Modifier.fillMaxSize()) {
            Row {
                BoxWithNotificationDot(
                    notificationDotVisible = false,
                ) { modifier ->
                    Icon(
                        painter = painterResource(id = CorePresentation.drawable.ic_proton_file),
                        contentDescription = null,
                        modifier = modifier,
                    )
                }
                Spacer(modifier = Modifier.size(16.dp))
                BoxWithNotificationDot(
                    notificationDotVisible = true,
                ) { modifier ->
                    Icon(
                        painter = painterResource(id = CorePresentation.drawable.ic_proton_file),
                        contentDescription = null,
                        modifier = modifier,
                    )
                }
            }
        }
    }
}

@Preview
@Composable
fun PreviewIconButton(
    @PreviewParameter(IconResIdProvider::class) iconResId: Int
) {
    ProtonTheme {
        Surface(modifier = Modifier.fillMaxSize()) {
            Row {
                IconButton(onClick = {}) {
                    BoxWithNotificationDot(
                        notificationDotVisible = false
                    ) { modifier ->
                        Icon(
                            painter = painterResource(id = iconResId),
                            contentDescription = null,
                            modifier = modifier.padding(4.dp),
                        )
                    }
                }
                Spacer(modifier = Modifier.size(16.dp))
                IconButton(onClick = {}) {
                    BoxWithNotificationDot(
                        notificationDotVisible = true
                    ) { modifier ->
                        Icon(
                            painter = painterResource(id = iconResId),
                            contentDescription = null,
                            modifier = modifier.padding(4.dp),
                        )
                    }
                }
            }
        }
    }
}

class IconResIdProvider : CollectionPreviewParameterProvider<Int>(
    listOf(
        CorePresentation.drawable.ic_proton_plus,
        CorePresentation.drawable.ic_proton_hamburger,
    )
)
