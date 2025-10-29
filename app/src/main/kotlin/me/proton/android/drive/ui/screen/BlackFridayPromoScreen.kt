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

package me.proton.android.drive.ui.screen

import androidx.compose.animation.Crossfade
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.ButtonColors
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.Divider
import androidx.compose.material.Text
import androidx.compose.material.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.paint
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import me.proton.android.drive.ui.viewevent.BlackFridayPromoViewEvent
import me.proton.android.drive.ui.viewmodel.BlackFridayPromoViewModel
import me.proton.android.drive.ui.viewstate.BlackFridayPromoViewState
import me.proton.core.compose.component.ProtonButton
import me.proton.core.compose.theme.ProtonDimens
import me.proton.core.compose.theme.ProtonTheme
import me.proton.core.drive.base.domain.extension.flowOf
import me.proton.core.drive.base.presentation.common.Action
import me.proton.core.drive.base.presentation.component.ThemelessStatusBarScreen
import me.proton.core.drive.base.presentation.component.TopBarActions
import me.proton.core.drive.base.presentation.extension.conditional
import me.proton.core.drive.base.presentation.extension.isLandscape
import me.proton.core.drive.base.presentation.extension.isPortrait
import me.proton.core.drive.base.presentation.extension.protonBlackFridayPromoButtonColors
import me.proton.core.drive.base.presentation.R as BasePresentation
import me.proton.core.drive.base.presentation.component.TopAppBar as BaseTopAppBar

@Composable
fun BlackFridayPromoScreen(
    navigateToSubscription: () -> Unit,
    navigateBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val viewModel = hiltViewModel<BlackFridayPromoViewModel>()
    val viewState by viewModel.viewState.collectAsStateWithLifecycle(
        null
    )
    val viewEvent = remember {
        viewModel.viewEvent(
            navigateToSubscription = navigateToSubscription,
            navigateBack = navigateBack,
        )
    }
    Crossfade(viewState) { state ->
        if (state != null) {
            if (isLandscape) {
                BlackFridayPromoScreenLandscape(
                    viewState = state,
                    viewEvent = viewEvent,
                    modifier = modifier,
                )
            } else {
                BlackFridayPromoScreen(
                    viewState = state,
                    viewEvent = viewEvent,
                    modifier = modifier,
                )
            }
        }
    }
}

@Composable
fun BlackFridayPromoScreen(
    viewState: BlackFridayPromoViewState,
    viewEvent: BlackFridayPromoViewEvent,
    modifier: Modifier = Modifier,
) {
    ThemelessStatusBarScreen(useDarkIcons = false)
    Column(
        modifier = modifier
            .fillMaxSize()
            .paint(
                painter = painterResource(viewState.backgroundResId),
                contentScale = ContentScale.Crop,
                alignment = Alignment.BottomEnd,
            )
            .statusBarsPadding(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        TopAppBar(
            closeAction = viewState.closeAction,
        )
        Image(
            painter = painterResource(viewState.titleImageResId),
            contentDescription = null,
        )
        Column(
            modifier = Modifier
                .verticalScroll(rememberScrollState())
                .weight(1f),
            verticalArrangement = Arrangement.Center,
        ) {
            viewState.items.forEach { item ->
                Item(
                    imageResId = item.imageResId,
                    title = item.title,
                )
            }
        }
        Divider(
            color = Color.White.copy(alpha = 0.12f)
        )
        OfferPanel(
            firstMonthPrice = viewState.firstMonthPrice,
            period = viewState.period,
            pricePeriod = viewState.pricePeriod,
            autoRenewPrice = viewState.autoRenewPrice,
            getDealButton = stringResource(viewState.getDealButtonResId),
            onGetDealClick = viewEvent.onGetDeal,
        )
    }
}

@Composable
fun BlackFridayPromoScreenLandscape(
    viewState: BlackFridayPromoViewState,
    viewEvent: BlackFridayPromoViewEvent,
    modifier: Modifier = Modifier,
) {
    ThemelessStatusBarScreen(useDarkIcons = false)
    Row(
        modifier = modifier
            .fillMaxSize()
            .paint(
                painter = painterResource(viewState.backgroundResId),
                contentScale = ContentScale.Crop,
                alignment = Alignment.BottomEnd,
            )
            .statusBarsPadding(),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .weight(1f),
        ) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) {
                Image(
                    painter = painterResource(viewState.titleImageResId),
                    contentDescription = null,
                )
            }
            TopAppBar(
                closeAction = viewState.closeAction,
            )
        }
        Column(
            modifier = Modifier
                .fillMaxSize()
                .weight(1f),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Column(
                modifier = Modifier
                    .verticalScroll(rememberScrollState())
                    .weight(1f),
                verticalArrangement = Arrangement.Center,
            ) {
                viewState.items.forEach { item ->
                    Item(
                        imageResId = item.imageResId,
                        title = item.title,
                    )
                }
            }
            OfferPanel(
                firstMonthPrice = viewState.firstMonthPrice,
                period = viewState.period,
                pricePeriod = viewState.pricePeriod,
                autoRenewPrice = viewState.autoRenewPrice,
                getDealButton = stringResource(viewState.getDealButtonResId),
                onGetDealClick = viewEvent.onGetDeal,
            )
        }
    }
}

@Composable
private fun TopAppBar(
    closeAction: Action,
    modifier: Modifier = Modifier,
    backgroundColor: Color = Color.Transparent,
    iconTintColor: Color = Color.White,
    elevation: Dp = 0.dp,
) {
    if (isLandscape) {
        BaseTopAppBar(
            navigationIcon = painterResource((closeAction as Action.Icon).iconResId),
            onNavigationIcon = closeAction.onAction,
            title = "",
            backgroundColor = backgroundColor,
        )
    } else {
        TopAppBar(
            title = {},
            modifier = modifier,
            backgroundColor = backgroundColor,
            elevation = elevation,
            actions = {
                TopBarActions(
                    actionFlow = flowOf { setOf(closeAction) },
                    iconTintColor = iconTintColor,
                )
            }
        )
    }
}

@Composable
private fun OfferPanel(
    firstMonthPrice: String,
    period: String,
    pricePeriod: String,
    autoRenewPrice: String,
    getDealButton: String,
    modifier: Modifier = Modifier,
    onGetDealClick: () -> Unit,
) {
    val verticalSpacing = if (isLandscape) 6.dp else 12.dp
    val backgroundPainter = painterResource(BasePresentation.drawable.bg_black_friday_promo_panel)
    val topPadding = if (isLandscape) ProtonDimens.SmallSpacing else ProtonDimens.DefaultSpacing
    Column(
        modifier = modifier
            .conditional(isPortrait) {
                paint(
                    painter = backgroundPainter,
                    contentScale = ContentScale.FillBounds,
                    alignment = Alignment.BottomEnd,
                )
            }
            .padding(top = topPadding)
            .padding(horizontal = ProtonDimens.DefaultSpacing),
        verticalArrangement = Arrangement.spacedBy(verticalSpacing),
    ) {
        Offer(
            period = period,
            price = firstMonthPrice,
            pricePeriod = pricePeriod,
        )
        GetDealButton(
            title = getDealButton,
            onClick = onGetDealClick,
            modifier = Modifier.fillMaxWidth()
        )
        Text(
            text = autoRenewPrice,
            style = ProtonTheme.typography.body2Medium.copy(Color.White),
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding(),
        )
    }
}

@Composable
private fun Offer(
    period: String,
    price: String,
    pricePeriod: String,
    modifier: Modifier = Modifier,
) {
    val minHeight = if (isLandscape) 48.dp else 68.dp
    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = minHeight)
            .background(
                color = Color.Black.copy(alpha = 0.2f),
                shape = RoundedCornerShape(16.dp)
            )
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = period,
            style = ProtonTheme.typography.body1Medium.copy(Color.White),
            modifier = modifier.weight(1f)
        )
        Text(
            text = price,
            style = ProtonTheme.typography.body1Bold.copy(Color.White)
        )
        Text(
            text = pricePeriod,
            style = ProtonTheme.typography.body1Medium.copy(Color.White),
            modifier = Modifier.padding(start = 4.dp)
        )
    }
}

@Composable
fun GetDealButton(
    title: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    loading: Boolean = false,
    contained: Boolean = true,
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
    colors: ButtonColors = ButtonDefaults.protonBlackFridayPromoButtonColors(loading),
    onClick: () -> Unit,
) {
    val minHeight = if (isLandscape) 44.dp else 54.dp
    ProtonButton(
        onClick = onClick,
        modifier = modifier.heightIn(min = minHeight),
        enabled = enabled,
        loading = loading,
        contained = contained,
        interactionSource = interactionSource,
        elevation = ButtonDefaults.elevation(0.dp),
        shape = RoundedCornerShape(100.dp),
        border = BorderStroke(0.dp, ProtonTheme.colors.backgroundNorm),
        colors = colors,
        contentPadding = ButtonDefaults.ContentPadding,
        content = {
            Text(
                text = title,
                style = ProtonTheme.typography.body1Bold.copy(color = Color(0xFF0C0C14))
            )
        },
    )
}

@Composable
private fun Item(
    imageResId: Int,
    title: String,
    modifier: Modifier = Modifier
) {
    val minHeight = if (isLandscape) 40.dp else 56.dp
    val textStyle = if (isLandscape) {
        ProtonTheme.typography.body1Bold.copy(color = Color.White)
    } else {
        ProtonTheme.typography.subheadline.copy(color = Color.White)
    }
    val size = if (isLandscape) 32.dp else 40.dp
    Row(
        modifier = modifier.heightIn(min = minHeight),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(ProtonDimens.DefaultSpacing)
    ) {
        Image(
            painter = painterResource(imageResId),
            contentDescription = null,
            modifier = Modifier.size(size),
        )
        Text(
            text = title,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            style = textStyle,
        )
    }
}
