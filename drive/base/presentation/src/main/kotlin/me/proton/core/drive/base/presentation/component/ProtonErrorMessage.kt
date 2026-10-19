/*
 * Copyright (c) 2026 Proton AG.
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

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import me.proton.core.compose.theme.ProtonTheme
import me.proton.core.compose.theme.defaultSmallInverted
import me.proton.core.compose.theme.defaultSmallStrongInverted

@Composable
fun ProtonErrorMessage(
    errorMessage: String,
    modifier: Modifier = Modifier,
    elevation: Dp = Elevation
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = MinHeight),
        colors = CardDefaults.cardColors(
            containerColor = ProtonTheme.colors.notificationError,
            contentColor = ProtonTheme.colors.textInverted,
        ),
        shape = ProtonTheme.shapes.medium,
        elevation = CardDefaults.cardElevation(defaultElevation = elevation),
    ) {
        Text(
            text = errorMessage,
            style = ProtonTheme.typography.defaultSmallInverted,
            modifier = Modifier
                .padding(
                    start = MessageHorizontalPadding,
                    top = MessageVerticalPadding,
                    end = Padding,
                    bottom = MessageHorizontalPadding
                ),
        )
    }
}

@Composable
fun ProtonErrorMessageWithAction(
    errorMessage: String,
    modifier: Modifier = Modifier,
    elevation: Dp = Elevation,
    action: String,
    onAction: () -> Unit,
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = MinHeight),
        colors = CardDefaults.cardColors(
            containerColor = ProtonTheme.colors.notificationError,
            contentColor = ProtonTheme.colors.textInverted,
        ),
        shape = ProtonTheme.shapes.medium,
        elevation = CardDefaults.cardElevation(defaultElevation = elevation),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(Padding),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = errorMessage,
                style = ProtonTheme.typography.defaultSmallInverted,
                modifier = Modifier
                    .padding(horizontal = MessageWithActionHorizontalPadding, vertical = MessageVerticalPadding)
                    .weight(1f),
            )
            ProtonTextButton(
                modifier = Modifier.padding(horizontal = ActionHorizontalPadding, vertical = ActionVerticalPadding),
                onClick = { onAction() }
            ) {
                Text(
                    text = action,
                    style = ProtonTheme.typography.defaultSmallStrongInverted,
                )
            }
        }
    }
}

private val Padding = 8.dp
private val MinHeight = 48.dp
private val Elevation = 6.dp
private val MessageHorizontalPadding = 16.dp
private val MessageWithActionHorizontalPadding = 8.dp
private val MessageVerticalPadding = 14.dp
private val ActionHorizontalPadding = 12.dp
private val ActionVerticalPadding = 8.dp
val ErrorPadding = 16.dp
