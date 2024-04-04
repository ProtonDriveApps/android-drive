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

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.ClickableText
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import me.proton.android.drive.ui.viewmodel.GetMoreFreeStorageViewModel
import me.proton.android.drive.ui.viewstate.GetMoreFreeStorageViewState
import me.proton.core.compose.flow.rememberFlowWithLifecycle
import me.proton.core.compose.theme.ProtonDimens
import me.proton.core.compose.theme.ProtonDimens.DefaultSpacing
import me.proton.core.compose.theme.ProtonDimens.ExtraSmallSpacing
import me.proton.core.compose.theme.ProtonDimens.MediumSpacing
import me.proton.core.compose.theme.ProtonDimens.SmallSpacing
import me.proton.core.compose.theme.ProtonTheme
import me.proton.core.compose.theme.defaultHighlightNorm
import me.proton.core.compose.theme.defaultSmallNorm
import me.proton.core.compose.theme.defaultWeak
import me.proton.core.compose.theme.headlineNorm
import me.proton.core.drive.base.domain.extension.GiB
import me.proton.core.drive.base.presentation.component.TopAppBar
import me.proton.core.drive.base.presentation.extension.asHumanReadableString
import me.proton.core.drive.base.presentation.extension.isLandscape
import me.proton.core.drive.base.presentation.R as BasePresentation
import me.proton.core.presentation.R as CorePresentation
import me.proton.core.drive.i18n.R as I18N

@Composable
fun GetMoreFreeStorageScreen(
    modifier: Modifier = Modifier,
    navigateBack: () -> Unit,
) {
    val viewModel = hiltViewModel<GetMoreFreeStorageViewModel>()
    val viewState by rememberFlowWithLifecycle(viewModel.viewState).collectAsState(
        initial = viewModel.initialViewState
    )
    GetMoreFreeStorage(
        viewState = viewState,
        navigateBack = navigateBack,
        modifier = modifier.fillMaxSize()
    )
}

@Composable
fun GetMoreFreeStorage(
    viewState: GetMoreFreeStorageViewState,
    modifier: Modifier = Modifier,
    navigateBack: () -> Unit,
) {
    Column(
        modifier = modifier
            .navigationBarsPadding()
            .fillMaxSize()
            .testTag(GetMoreFreeStorage.screen),
        verticalArrangement = Arrangement.Top,
    ) {
        TopAppBar(navigateBack = navigateBack)
        Content(
            viewState = viewState,
            modifier = Modifier
                .fillMaxSize(),
        )
    }
}

@Composable
private fun TopAppBar(
    modifier: Modifier = Modifier,
    navigateBack: () -> Unit,
) {
    TopAppBar(
        navigationIcon = painterResource(id = CorePresentation.drawable.ic_proton_close),
        onNavigationIcon = navigateBack,
        title = {},
        modifier = modifier.statusBarsPadding(),
    )
}

@Composable
private fun Content(
    viewState: GetMoreFreeStorageViewState,
    modifier: Modifier = Modifier,
) {
    if (isLandscape) {
        Row(
            modifier = modifier
                .verticalScroll(rememberScrollState())
                .fillMaxSize()
                .padding(DefaultSpacing),
        ) {
            TitlePart(
                viewState = viewState,
                modifier = Modifier
                    .padding(horizontal = SmallSpacing)
                    .weight(1f),
            )
            ActionsPart(
                actions = viewState.actions,
                modifier = Modifier
                    .padding(horizontal = SmallSpacing)
                    .weight(1f),
            )
        }
    } else {
        Column(
            modifier = modifier
                .verticalScroll(rememberScrollState())
                .fillMaxSize()
                .padding(DefaultSpacing),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            TitlePart(
                viewState = viewState,
            )
            ActionsPart(
                actions = viewState.actions,
            )
        }
    }
}

@Composable
private fun TitlePart(
    viewState: GetMoreFreeStorageViewState,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Image(painter = painterResource(id = viewState.imageResId), contentDescription = null)
        Text(
            text = viewState.title,
            style = ProtonTheme.typography.headlineNorm,
            modifier = Modifier.padding(top = MediumSpacing)
        )
        Text(
            text = stringResource(id = viewState.descriptionResId),
            style = ProtonTheme.typography.defaultWeak,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = SmallSpacing, bottom = DefaultSpacing)
        )
    }
}

@Composable
private fun ActionsPart(
    actions: List<GetMoreFreeStorageViewState.Action>,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        actions.forEach { action ->
            ListItem(action = action)
        }
    }
}

@Composable
private fun ListItem(
    action: GetMoreFreeStorageViewState.Action,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = SmallSpacing),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        ActionIcon(
            iconResId = action.iconResId,
            iconTintColor = if (action.isDone) ProtonTheme.colors.notificationSuccess else ProtonTheme.colors.iconAccent,
            bgColor = if (action.isDone) ProtonTheme.colors.notificationSuccess.copy(alpha = 0.06F) else ProtonTheme.colors.backgroundSecondary
        )
        ActionDetails(
            titleResId = action.titleResId,
            titleTextStyle = ProtonTheme.typography.defaultHighlightNorm.copy(
                color = if (action.isDone) ProtonTheme.colors.textWeak else ProtonTheme.colors.textNorm
            ),
            subtitle = action.getDescription(),
            subtitleTextStyle = ProtonTheme.typography.defaultSmallNorm,
            onSubtitleClick = action.onSubtitleClick,
        )
    }
}

@Composable
private fun ActionIcon(
    iconResId: Int,
    iconTintColor: Color,
    bgColor: Color,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .sizeIn(minHeight = 40.dp, minWidth = 40.dp)
            .background(
                color = bgColor,
                shape = RoundedCornerShape(ProtonDimens.ExtraLargeCornerRadius),
            ),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            painter = painterResource(id = iconResId),
            tint = iconTintColor,
            contentDescription = null,
        )
    }
}

@Composable
private fun ActionDetails(
    titleResId: Int,
    titleTextStyle: TextStyle,
    subtitle: AnnotatedString,
    subtitleTextStyle: TextStyle,
    modifier: Modifier = Modifier,
    onSubtitleClick: (Int) -> Unit,
) {
    Column(
        modifier = modifier
            .sizeIn(minHeight = 64.dp)
            .padding(start = DefaultSpacing),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.Start,
    ) {
        Text(
            text = stringResource(id = titleResId),
            style = titleTextStyle,
        )
        ClickableText(
            text = subtitle,
            style = subtitleTextStyle,
            onClick = onSubtitleClick,
        )
    }
}

@Preview
@Composable
fun PreviewGetMoreFreeStorage() {
    ProtonTheme {
        GetMoreFreeStorage(
            viewState = GetMoreFreeStorageViewState(
                imageResId = BasePresentation.drawable.img_free_storage,
                title = stringResource(
                    id = I18N.string.get_more_free_storage_title,
                    5.GiB.asHumanReadableString(LocalContext.current, numberOfDecimals = 0),
                ),
                descriptionResId = I18N.string.get_more_free_storage_description,
                actions = listOf(
                    GetMoreFreeStorageViewState.Action(
                        iconResId = CorePresentation.drawable.ic_proton_arrow_up_line,
                        titleResId = I18N.string.get_more_free_storage_action_upload_title,
                        getDescription = {
                            AnnotatedString(
                                stringResource(id = I18N.string.get_more_free_storage_action_upload_subtitle)
                            )
                        },
                        isDone = false,
                    ),
                    GetMoreFreeStorageViewState.Action(
                        iconResId = CorePresentation.drawable.ic_proton_checkmark,
                        titleResId = I18N.string.get_more_free_storage_action_link_title,
                        getDescription = {
                            AnnotatedString(
                                "Select any file of folder. Open the options menu and press Get link."
                            )
                        },
                        isDone = true,
                    ),
                    GetMoreFreeStorageViewState.Action(
                        iconResId = CorePresentation.drawable.ic_proton_key,
                        titleResId = I18N.string.get_more_free_storage_action_recovery_title,
                        getDescription = {
                            AnnotatedString(
                                text = "Sign in at account.proton.me, then go to Settings -> Recovery."
                            )
                        },
                        isDone = false,
                    ),
                ),
            ),
            navigateBack = {},
        )
    }
}

object GetMoreFreeStorage {
    const val screen = "GetMoreFreeStorage screen"
}
