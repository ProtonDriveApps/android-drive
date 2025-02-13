/*
 * Copyright (c) 2024 Proton AG.
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

package me.proton.android.drive.ui.screen

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.Divider
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import me.proton.android.drive.ui.effect.HandleHomeEffect
import me.proton.android.drive.ui.viewevent.SharedTabsViewEvent
import me.proton.android.drive.ui.viewmodel.SharedTabsViewModel
import me.proton.android.drive.ui.viewstate.HomeScaffoldState
import me.proton.android.drive.ui.viewstate.SharedTab
import me.proton.android.drive.ui.viewstate.SharedTabsViewState
import me.proton.core.compose.component.ProtonButton
import me.proton.core.compose.component.protonTextButtonColors
import me.proton.core.compose.theme.ProtonDimens.DefaultButtonMinHeight
import me.proton.core.compose.theme.ProtonDimens.SmallSpacing
import me.proton.core.compose.theme.ProtonTheme
import me.proton.core.drive.link.domain.entity.FileId
import me.proton.core.drive.link.domain.entity.FolderId
import me.proton.core.drive.link.domain.entity.LinkId
import me.proton.core.drive.base.presentation.component.TopAppBar as BaseTopAppBar
import me.proton.core.drive.i18n.R as I18N

@Composable
fun SharedTabsScreen(
    homeScaffoldState: HomeScaffoldState,
    navigateToFiles: (FolderId, String?) -> Unit,
    navigateToPreview: (FileId) -> Unit,
    navigateToFileOrFolderOptions: (LinkId) -> Unit,
    navigateToUserInvitation: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val viewModel = hiltViewModel<SharedTabsViewModel>()
    val viewState by viewModel.viewState.collectAsStateWithLifecycle(initialValue = viewModel.initialViewState)
    val viewEvent = remember {
        viewModel.viewEvent()
    }
    viewModel.HandleHomeEffect(homeScaffoldState)
    LaunchedEffect(viewState) {
        homeScaffoldState.topAppBar.value = {
            TopAppBar(
                titleResId = viewState.titleResId,
                navigationIconResId = viewState.navigationIconResId,
                onTopAppBarNavigation = viewEvent.onTopAppBarNavigation,
            )
        }
    }
    SharedTabs(
        homeScaffoldState = homeScaffoldState,
        viewState = viewState,
        viewEvent = viewEvent,
        modifier = modifier.fillMaxSize(),
        navigateToFiles = navigateToFiles,
        navigateToPreview = navigateToPreview,
        navigateToFileOrFolderOptions = navigateToFileOrFolderOptions,
        navigateToUserInvitation = navigateToUserInvitation,
    )
}

@Composable
fun SharedTabs(
    homeScaffoldState: HomeScaffoldState,
    viewState: SharedTabsViewState,
    viewEvent: SharedTabsViewEvent,
    navigateToFiles: (FolderId, String?) -> Unit,
    navigateToPreview: (FileId) -> Unit,
    navigateToFileOrFolderOptions: (LinkId) -> Unit,
    navigateToUserInvitation: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
        ) {
            viewState.tabs.forEach { sharedTab ->
                SharedTab(
                    sharedTab = sharedTab,
                    isSelected = viewState.selectedTab == sharedTab,
                    onTab = { viewEvent.onTab(sharedTab) }
                )
            }
        }
        Box {
            Divider(
                color = ProtonTheme.colors.separatorNorm,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp),
            )
        }
        when (viewState.selectedTab.type) {
            SharedTab.Type.SHARED_WITH_ME -> SharedWithMeScreen(
                homeScaffoldState = homeScaffoldState,
                navigateToFiles = navigateToFiles,
                navigateToPreview = navigateToPreview,
                navigateToFileOrFolderOptions = navigateToFileOrFolderOptions,
                navigateToUserInvitation = navigateToUserInvitation,
            )
            SharedTab.Type.SHARED_BY_ME -> SharedByMeScreen(
                homeScaffoldState = homeScaffoldState,
                navigateToFiles = navigateToFiles,
                navigateToPreview = navigateToPreview,
                navigateToFileOrFolderOptions = navigateToFileOrFolderOptions,
            )
        }
    }
}

@Composable
private fun TopAppBar(
    @StringRes titleResId: Int,
    @DrawableRes navigationIconResId: Int,
    modifier: Modifier = Modifier,
    onTopAppBarNavigation: () -> Unit,
) {
    BaseTopAppBar(
        navigationIcon = painterResource(id = navigationIconResId),
        onNavigationIcon = onTopAppBarNavigation,
        title = stringResource(id = titleResId),
        modifier = modifier,
    )
}

@Composable
private fun SharedTab(
    sharedTab: SharedTab,
    isSelected: Boolean,
    modifier: Modifier = Modifier,
    onTab: (SharedTab) -> Unit,
) {
    val brandColor = ProtonTheme.colors.brandNorm
    val dividerColor = remember(isSelected) { if (isSelected) brandColor else Color.Transparent }
    ProtonButton(
        modifier = modifier,
        onClick = { onTab(sharedTab) },
        contentPadding = PaddingValues(horizontal = SmallSpacing),
        colors =  ButtonDefaults.protonTextButtonColors(false),
        shape = RoundedCornerShape(0.dp),
        border = null,
        elevation = null,
    ) {
        Box(
            modifier = Modifier
                .height(DefaultButtonMinHeight),
        ) {
            Text(
                text = stringResource(id = sharedTab.titleResId),
                style = ProtonTheme.typography.body2Regular.copy(
                    color = if (isSelected) brandColor else ProtonTheme.colors.textWeak
                ),
                modifier = Modifier
                    .align(Alignment.Center),
            )
            Box(
                modifier = Modifier
                    .matchParentSize(),
                contentAlignment = Alignment.BottomCenter,
            ) {
                Divider(
                    color = dividerColor,
                    thickness = 4.dp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(100.dp, 100.dp, 0.dp, 0.dp)),
                )
            }
        }
    }
}

@Preview
@Composable
fun PreviewSharedTab() {
    ProtonTheme {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
        ) {
            SharedTab(
                sharedTab = SharedTab(
                    SharedTab.Type.SHARED_WITH_ME,
                    I18N.string.shared_with_me_title,
                ),
                isSelected = true,
                onTab = { _ -> }
            )
            SharedTab(
                sharedTab = SharedTab(
                    SharedTab.Type.SHARED_BY_ME,
                    I18N.string.shared_by_me_title,
                ),
                isSelected = false,
                onTab = { _ -> }
            )
        }
    }
}
