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

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.size
import androidx.compose.material.Icon
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import me.proton.core.compose.theme.ProtonTheme
import me.proton.core.compose.theme.captionNorm
import me.proton.core.drive.base.domain.extension.firstCodePointAsStringOrNull
import me.proton.core.drive.base.presentation.component.BadgeWithBorder
import me.proton.core.presentation.R as CorePresentation

@Composable
fun SharedUsers(
    users: List<String>,
    modifier: Modifier = Modifier,
    colors: Array<BadgeColor> = badgeColors,
    onClick: () -> Unit,
) {
    val hasTooManyUsers = users.size > colors.size
    val usersToShow = if (hasTooManyUsers) users.take(colors.size) else users
    Layout(
        modifier = modifier.clickable { onClick() },
        content = {
            usersToShow.forEachIndexed { index, user ->
                val badgeModifier = Modifier
                    .zIndex(index.toFloat())
                if (hasTooManyUsers && index == usersToShow.size - 1) {
                    HasMoreBadge(
                        badgeColor = colors[index],
                        modifier = badgeModifier,
                    )
                } else {
                    UserBadge(
                        text = user,
                        badgeColor = colors[index],
                        modifier = badgeModifier,
                    )
                }
            }
        }
    ) { measurables, constraints ->
        val badgePx = 24.dp.roundToPx()
        val overlapPx = 6.dp.roundToPx()
        val itemCount = measurables.size

        val totalWidth = badgePx + (itemCount - 1) * (badgePx - overlapPx)
        val placeables = measurables.map { it.measure(constraints) }

        layout(width = totalWidth, height = badgePx) {
            var x = 0
            placeables.forEachIndexed { index, placeable ->
                placeable.placeRelative(x = x, y = 0)
                x += badgePx - overlapPx
            }
        }
    }
}

@Composable
fun UserBadge(
    text: String,
    badgeColor: BadgeColor,
    modifier: Modifier = Modifier,
) {
    BadgeWithBorder(
        borderColor = badgeColor.borderColor,
        badgeColor = badgeColor.bgColor,
        modifier = modifier,
    ) {
        Text(
            text = text.firstCodePointAsStringOrNull?.uppercase() ?: "?",
            style = ProtonTheme.typography.captionNorm.copy(color = badgeColor.fgColor),
        )
    }
}

@Composable
fun HasMoreBadge(
    badgeColor: BadgeColor,
    modifier: Modifier = Modifier,
) {
    BadgeWithBorder(
        borderColor = badgeColor.borderColor,
        badgeColor = badgeColor.bgColor,
        modifier = modifier,
    ) {
        Icon(
            painter = painterResource(CorePresentation.drawable.ic_proton_three_dots_horizontal),
            contentDescription = null,
            tint = badgeColor.fgColor,
            modifier = Modifier.size(12.dp)
        )
    }
}

@Preview
@Composable
fun SharedUsersLightPreview() {
    ProtonTheme {
        Column {
            SharedUsers(
                users = listOf("Alice", "Bob", "Charlie", "Dave"),
                onClick = {},
            )
            SharedUsers(
                users = listOf("Alice", "Bob", "Charlie", "Dave", "Eve"),
                onClick = {},
            )
        }
    }
}

@Preview
@Composable
fun SharedUsersDarkPreview() {
    ProtonTheme(isDark = true) {
        Column {
            SharedUsers(
                users = listOf("Alice", "Bob", "Charlie", "Dave"),
                onClick = {},
            )
            SharedUsers(
                users = listOf("Alice", "Bob", "Charlie", "Dave", "Eve"),
                onClick = {},
            )
        }
    }
}

private val badgeBgBlue = Color(0xFFBFD5F3)
private val badgeFgBlue = Color(0xFF071355)
private val badgeBgPink = Color(0xFFECC6EA)
private val badgeFgPurple = Color(0xFF4B1148)
private val badgeBgOrange = Color(0xFFFFCCCC)

private val badgeColors: Array<BadgeColor> @Composable get() = arrayOf(
    BadgeColor(bgColor = badgeBgBlue, fgColor = badgeFgBlue, borderColor = ProtonTheme.colors.shade0),
    BadgeColor(bgColor = badgeBgPink, fgColor = badgeFgPurple, borderColor = ProtonTheme.colors.shade0),
    BadgeColor(bgColor = badgeBgOrange, fgColor = badgeFgPurple, borderColor = ProtonTheme.colors.shade0),
    BadgeColor(bgColor = ProtonTheme.colors.shade10, fgColor = ProtonTheme.colors.iconNorm, borderColor = ProtonTheme.colors.shade0),
)

data class BadgeColor(
    val bgColor: Color,
    val fgColor: Color,
    val borderColor: Color,
)
