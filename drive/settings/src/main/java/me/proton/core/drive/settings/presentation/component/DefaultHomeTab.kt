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

package me.proton.core.drive.settings.presentation.component

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.RadioButton
import androidx.compose.material.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import me.proton.core.compose.theme.ProtonDimens
import me.proton.core.compose.theme.ProtonTheme
import me.proton.core.compose.theme.defaultNorm
import me.proton.core.compose.theme.defaultWeak
import me.proton.core.drive.base.presentation.component.ProtonListItem
import me.proton.core.drive.base.presentation.component.TopAppBar
import me.proton.core.drive.settings.presentation.entity.TabItem
import me.proton.core.presentation.R as CorePresentation
import me.proton.core.drive.i18n.R as I18N

@Composable
fun DefaultHomeTab(
    title: String,
    tabItemsFlow: Flow<Set<TabItem>>,
    modifier: Modifier = Modifier,
    navigateBack: () -> Unit,
    onClick: (TabItem) -> Unit,
) {
    val tabItems = tabItemsFlow.collectAsStateWithLifecycle(initialValue = emptySet())
    Column(modifier = modifier) {
        TopAppBar(
            navigationIcon = painterResource(id = CorePresentation.drawable.ic_arrow_back),
            onNavigationIcon = navigateBack,
            title = title,
            modifier = Modifier.statusBarsPadding(),
        )
        HomeTabs(
            tabItems = tabItems.value,
            onClick = onClick,
            modifier = Modifier.navigationBarsPadding(),
        )
    }
}

@Composable
fun HomeTabs(
    tabItems: Set<TabItem>,
    modifier: Modifier = Modifier,
    onClick: (TabItem) -> Unit,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
    ) {
        tabItems.forEach { tabItem ->
            HomeTabItem(
                tabItem = tabItem,
                onClick = onClick,
            )
        }
    }
}

@Composable
fun HomeTabItem(
    tabItem: TabItem,
    modifier: Modifier = Modifier,
    onClick: (TabItem) -> Unit,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(enabled = tabItem.isEnabled) {
                onClick(tabItem)
            },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        ProtonListItem(
            icon = painterResource(id = tabItem.iconResId),
            iconTintColor = if (tabItem.isEnabled) ProtonTheme.colors.iconNorm else ProtonTheme.colors.iconDisabled,
            title = stringResource(id = tabItem.titleResId),
            textStyle = if (tabItem.isEnabled) ProtonTheme.typography.defaultNorm else ProtonTheme.typography.defaultWeak,
            modifier = modifier
                .weight(1f)
                .padding(start = ProtonDimens.DefaultSpacing),
        )
        RadioButton(
            selected = tabItem.isSelected,
            enabled = tabItem.isEnabled,
            onClick = { onClick(tabItem) },
        )
    }
}

@Preview
@Composable
fun DefaultHomeTabPreview() {
    ProtonTheme {
        Surface {
            DefaultHomeTab(
                title = stringResource(id = I18N.string.title_default_home_screen_tab),
                tabItemsFlow = flowOf(
                    setOf(
                        TabItem(
                            iconResId = CorePresentation.drawable.ic_proton_folder,
                            titleResId = I18N.string.title_files,
                            route = "files",
                            isSelected = false,
                            isEnabled = true,
                        ),
                        TabItem(
                            iconResId = CorePresentation.drawable.ic_proton_image,
                            titleResId = I18N.string.photos_title,
                            route = "photos",
                            isSelected = true,
                            isEnabled = false,
                        ),
                        TabItem(
                            iconResId = CorePresentation.drawable.ic_proton_tv,
                            titleResId = I18N.string.computers_title,
                            route = "computers",
                            isSelected = false,
                            isEnabled = true,
                        ),
                        TabItem(
                            iconResId = CorePresentation.drawable.ic_proton_link,
                            titleResId = I18N.string.title_shared,
                            route = "shared",
                            isSelected = false,
                            isEnabled = true,
                        ),
                    )
                ),
                navigateBack = {},
                onClick = { _ -> },
            )
        }
    }
}
