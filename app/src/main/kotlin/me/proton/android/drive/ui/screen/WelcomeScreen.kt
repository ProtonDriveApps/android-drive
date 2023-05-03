/*
 * Copyright (c) 2023 Proton AG.
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

import android.content.res.Configuration
import androidx.annotation.StringRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.Icon
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.google.accompanist.pager.ExperimentalPagerApi
import com.google.accompanist.pager.HorizontalPager
import com.google.accompanist.pager.rememberPagerState
import kotlinx.coroutines.launch
import me.proton.android.drive.ui.viewevent.WelcomeViewEvent
import me.proton.android.drive.ui.viewmodel.WelcomeViewModel
import me.proton.android.drive.ui.viewstate.WelcomeDescriptionAction
import me.proton.android.drive.ui.viewstate.WelcomeViewState
import me.proton.core.compose.component.ProtonSolidButton
import me.proton.core.compose.component.ProtonTextButton
import me.proton.core.compose.flow.rememberFlowWithLifecycle
import me.proton.core.compose.theme.ProtonDimens.DefaultButtonMinHeight
import me.proton.core.compose.theme.ProtonDimens.DefaultSpacing
import me.proton.core.compose.theme.ProtonDimens.ExtraSmallSpacing
import me.proton.core.compose.theme.ProtonDimens.LargeSpacing
import me.proton.core.compose.theme.ProtonDimens.MediumSpacing
import me.proton.core.compose.theme.ProtonDimens.SmallSpacing
import me.proton.core.compose.theme.ProtonTheme
import me.proton.core.compose.theme.default
import me.proton.core.compose.theme.headline
import me.proton.core.compose.theme.headlineSmall
import me.proton.core.drive.i18n.R as I18N

@Composable
fun WelcomeScreen(
    navigateToLauncher: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val welcomeViewModel = hiltViewModel<WelcomeViewModel>()
    Welcome(
        viewModel = welcomeViewModel,
        navigateToLauncher = navigateToLauncher,
        modifier = modifier
            .fillMaxSize()
            .systemBarsPadding()
            .testTag(WelcomeScreenTestTag.screen),
    )
}

@Composable
fun Welcome(
    viewModel: WelcomeViewModel,
    navigateToLauncher: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val items by rememberFlowWithLifecycle(flow = viewModel.items)
        .collectAsState(initial = emptyList())
    if (items.isNotEmpty()) {
        Welcome(
            items = items,
            viewEvent = viewModel::viewEvent,
            navigateToLauncher = navigateToLauncher,
            modifier = modifier,
        )
    }
}

@OptIn(ExperimentalPagerApi::class)
@Composable
fun Welcome(
    items: List<WelcomeViewState>,
    viewEvent: (navigateToLauncher: () -> Unit, nextPage: () -> Unit) -> WelcomeViewEvent,
    navigateToLauncher: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val pagerState = rememberPagerState(initialPage = 0)
    val scope = rememberCoroutineScope()
    HorizontalPager(
        state = pagerState,
        count = items.size,
        modifier = modifier,
    ) { page ->
        Welcome(
            viewState = items[page],
            viewEvent = viewEvent(navigateToLauncher) {
                scope.launch { pagerState.animateScrollToPage(pagerState.currentPage + 1) }
            },
            page = page,
            pageCount = items.size,
        )
    }
}

@Composable
fun Welcome(
    viewState: WelcomeViewState,
    viewEvent: WelcomeViewEvent,
    page: Int,
    pageCount: Int,
    modifier: Modifier = Modifier
) {
    when (LocalConfiguration.current.orientation) {
        Configuration.ORIENTATION_PORTRAIT -> WelcomeSingleColumn(viewState, viewEvent, page, pageCount, modifier)
        else -> WelcomeTwoColumns(viewState, viewEvent, page, pageCount, modifier)
    }
}

@Composable
fun WelcomeSingleColumn(
    viewState: WelcomeViewState,
    viewEvent: WelcomeViewEvent,
    page: Int,
    pageCount: Int,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxSize(),
    ) {
        WelcomeGraphics(
            hasSkip = viewState.hasSkip,
            graphicsResId = viewState.graphicResId,
            onSkip = viewEvent.onSkip,
            modifier = Modifier.weight(1f)
        )
        WelcomeDetails(
            page = page,
            pageCount = pageCount,
            title = viewState.title,
            descriptionResId = viewState.descriptionResId,
            actionTitleResId = viewState.actionTitleResId,
            descriptionActions = viewState.descriptionActions,
            onAction = viewEvent.onAction,
            modifier = Modifier.height(WelcomeDetailsHeight)
        )
    }
}

@Composable
fun WelcomeTwoColumns(
    viewState: WelcomeViewState,
    viewEvent: WelcomeViewEvent,
    page: Int,
    pageCount: Int,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxSize(),
    ) {
        WelcomeDetails(
            page = page,
            pageCount = pageCount,
            title = viewState.title,
            descriptionResId = viewState.descriptionResId,
            actionTitleResId = viewState.actionTitleResId,
            descriptionActions = viewState.descriptionActions,
            onAction = viewEvent.onAction,
            modifier = Modifier
                .weight(1f),
            contentVerticalArrangement = Arrangement.Center,
        )
        WelcomeGraphics(
            hasSkip = viewState.hasSkip,
            graphicsResId = viewState.graphicResId,
            onSkip = viewEvent.onSkip,
            modifier = Modifier
                .weight(1f)
        )
    }
}

@Composable
fun WelcomeGraphics(
    hasSkip: Boolean,
    graphicsResId: Int,
    modifier: Modifier = Modifier,
    onSkip: () -> Unit,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(ProtonTheme.colors.backgroundSecondary)
            .systemBarsPadding()
    ) {
        Box(
            modifier = Modifier
                .height(WelcomeHeaderMinHeight)
                .fillMaxWidth()
        ) {
            if (hasSkip) {
                ProtonTextButton(
                    onClick = onSkip,
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                ) {
                    Text(
                        text = stringResource(id = I18N.string.common_skip_action),
                        style = ProtonTheme.typography.headlineSmall,
                        color = ProtonTheme.colors.interactionNorm,
                    )
                }
            }
        }
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            Image(
                painter = painterResource(id = graphicsResId),
                contentDescription = null,
                modifier = Modifier
                    .padding(top = DefaultSpacing)
                    .align(Alignment.Center)
            )
        }
    }
}

@Composable
fun WelcomeDetails(
    page: Int,
    pageCount: Int,
    title: String,
    @StringRes descriptionResId: Int,
    @StringRes actionTitleResId: Int,
    descriptionActions: List<WelcomeDescriptionAction>,
    onAction: (page: Int) -> Unit,
    modifier: Modifier = Modifier,
    contentVerticalArrangement: Arrangement.Vertical = Arrangement.Top,
) {
    Column(
        modifier = modifier
            .padding(horizontal = MediumSpacing)
            .fillMaxSize()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) {
            DotsIndicator(
                total = pageCount,
                selected = page,
                selectedColor = ProtonTheme.colors.interactionNorm,
                unselectedColor = ProtonTheme.colors.interactionWeakNorm,
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .padding(top = MediumSpacing)
            )
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = contentVerticalArrangement,
            ) {
                Text(
                    text = title,
                    style = ProtonTheme.typography.headline,
                    modifier = Modifier
                        .align(Alignment.CenterHorizontally)
                        .padding(top = MediumSpacing)
                )
                Text(
                    text = stringResource(id = descriptionResId),
                    textAlign = TextAlign.Center,
                    style = ProtonTheme.typography.default,
                    modifier = Modifier
                        .align(Alignment.CenterHorizontally)
                        .padding(vertical = SmallSpacing)
                )
                if (descriptionActions.isNotEmpty()) {
                    DescriptionActions(
                        actions = descriptionActions,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        }
        Box(
            modifier = Modifier
                .padding(vertical = LargeSpacing)
        ) {
            ProtonSolidButton(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(DefaultButtonMinHeight),
                onClick = { onAction(page) },
            ) {
                Text(text = stringResource(id = actionTitleResId))
            }
        }
    }
}

@Composable
fun DescriptionActions(
    actions: List<WelcomeDescriptionAction>,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Column(
            modifier = Modifier.padding(top = ExtraSmallSpacing, bottom = ExtraSmallSpacing)
        ) {
            actions.forEach { descriptionAction ->
                DescriptionAction(descriptionAction, Modifier.padding(top = SmallSpacing))
            }
        }
    }
}

@Composable
fun DescriptionAction(
    action: WelcomeDescriptionAction,
    modifier: Modifier = Modifier,
) {
    Row(modifier = modifier) {
        Icon(
            painter = painterResource(id = action.iconResId),
            contentDescription = null,
            modifier = Modifier.padding(end = SmallSpacing)
        )
        Text(
            text = stringResource(id = action.titleResId),
        )
    }
}

@Composable
fun DotsIndicator(
    total: Int,
    selected: Int,
    selectedColor: Color,
    unselectedColor: Color,
    modifier: Modifier = Modifier
) {
    LazyRow(modifier = modifier) {
        items(total) { index ->
            if (index == selected) {
                Box(
                    modifier = Modifier
                        .size(SmallSpacing)
                        .clip(CircleShape)
                        .background(selectedColor)
                )
            } else {
                Box(
                    modifier = Modifier
                        .size(SmallSpacing)
                        .clip(CircleShape)
                        .background(unselectedColor)
                )
            }
            if (index != total - 1) {
                Spacer(Modifier.size(SmallSpacing))
            }
        }
    }
}

private val WelcomeHeaderMinHeight = 56.dp
private val WelcomeDetailsHeight = 360.dp

object WelcomeScreenTestTag {
    const val screen = "welcome screen"
}
