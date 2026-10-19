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
package me.proton.core.drive.base.presentation.component

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import me.proton.core.compose.theme.ProtonDimens
import me.proton.core.compose.theme.ProtonTheme

data class NavigationTab(
    val iconResId: Int,
    val titleResId: Int,
    val notificationDotVisible: Boolean = false
)

@Composable
fun BottomNavigation(
    selectedTab: NavigationTab?,
    tabs: List<NavigationTab>,
    onSelectedTab: (NavigationTab) -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(modifier) {
        NavigationBar(
            containerColor = ProtonTheme.colors.backgroundNorm,
            contentColor = ProtonTheme.colors.iconWeak,
            tonalElevation = 0.dp,
            windowInsets = WindowInsets(0, 0, 0, 0),
        ) {
            tabs.forEach { tab ->
                val isSelected = selectedTab == tab
                NavigationBarItem(
                    icon = {
                        BoxWithNotificationDot(
                            modifier = Modifier.padding(horizontal = ProtonDimens.SmallSpacing),
                            notificationDotVisible = tab.notificationDotVisible,
                            horizontalOffset = ProtonDimens.SmallSpacing,
                        ) {
                            Icon(
                                painter = painterResource(id = tab.iconResId),
                                contentDescription = stringResource(id = tab.titleResId)
                            )
                        }
                    },
                    label = {
                        Text(
                            text = stringResource(id = tab.titleResId),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    },
                    selected = isSelected,
                    onClick = { onSelectedTab(tab) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = ProtonTheme.colors.interactionNorm,
                        selectedTextColor = ProtonTheme.colors.interactionNorm,
                        indicatorColor = if (ProtonTheme.colors.isDark) {
                            ProtonTheme.colors.shade15
                        } else {
                            ProtonTheme.colors.interactionWeakNorm
                        },
                        unselectedIconColor = ProtonTheme.colors.iconWeak,
                        unselectedTextColor = ProtonTheme.colors.iconWeak,
                    ),
                    modifier = Modifier.testTag(BottomNavigationComponentTestTag.tab)
                )
            }
        }
        HorizontalDivider(color = ProtonTheme.colors.separatorNorm)
    }
}

val BottomNavigationHeight = 80.dp

object BottomNavigationComponentTestTag {
    const val tab = "bottom navigation tab"
}
