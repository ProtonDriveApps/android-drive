/*
 * Copyright (c) 2026 Proton AG.
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

import android.app.Activity
import android.content.ActivityNotFoundException
import android.view.ContextThemeWrapper
import android.view.ViewGroup
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Text
import androidx.compose.material.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.paint
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.zIndex
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import me.proton.android.drive.R
import me.proton.android.drive.log.DriveLogTag
import me.proton.android.drive.ui.effect.PaymentErrorEffect
import me.proton.android.drive.ui.viewevent.SummerSalePromoViewEvent
import me.proton.android.drive.ui.viewmodel.SummerSalePromoViewModel
import me.proton.android.drive.ui.viewstate.ProtonPaymentButtonViewState
import me.proton.android.drive.ui.viewstate.SummerSalePromoViewState
import me.proton.core.compose.theme.ProtonDimens
import me.proton.core.compose.theme.ProtonTheme
import me.proton.core.drive.base.domain.extension.flowOf
import me.proton.core.drive.base.presentation.common.Action
import me.proton.core.drive.base.presentation.component.ThemelessStatusBarScreen
import me.proton.core.drive.base.presentation.component.TopBarActions
import me.proton.core.drive.base.presentation.extension.isLandscape
import me.proton.core.payment.presentation.view.ProtonPaymentButton
import me.proton.core.payment.presentation.viewmodel.ProtonPaymentEvent
import me.proton.core.plan.presentation.ui.StartUnredeemedPurchase
import me.proton.core.util.kotlin.CoreLogger
import me.proton.core.drive.base.presentation.R as BasePresentation
import me.proton.core.drive.base.presentation.component.TopAppBar as BaseTopAppBar


@Composable
fun SummerSalePromoScreen(
    navigateBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val viewModel = hiltViewModel<SummerSalePromoViewModel>()
    val viewState by viewModel.viewState.collectAsStateWithLifecycle(
        null
    )
    val viewEvent = remember {
        viewModel.viewEvent(
            navigateBack = navigateBack,
        )
    }

    val context = LocalContext.current
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) {
        if (it.resultCode == Activity.RESULT_OK) {
            viewEvent.onRedeemedSuccessfully()
        }
    }

    LaunchedEffect(viewModel, LocalContext.current) {
        viewModel.paymentErrorEffect
            .onEach { effect ->
                when (effect) {
                    PaymentErrorEffect.GiapUnredeemed -> try {
                        launcher.launch(
                            input = StartUnredeemedPurchase.createIntent(context, Unit)
                        )
                    } catch (e: ActivityNotFoundException) {
                        CoreLogger.w(
                            tag = DriveLogTag.UI,
                            e = e,
                            message = "Unexpected UnredeemedPurchaseActivity activity not found",
                        )
                        viewEvent.onPaymentCallback(ProtonPaymentEvent.Error.UnrecoverableBillingError)
                    }
                }
            }
            .launchIn(this)
    }

    AnimatedVisibility(viewState != null) {
        LaunchedEffect(Unit) {
            viewEvent.onPromoShown()
        }
        viewState?.let { state ->
            if (isLandscape) {
                SummerSalePromoScreenLandscape(
                    viewState = state,
                    viewEvent = viewEvent,
                    modifier = modifier,
                )
            } else {
                SummerSalePromoScreenPortrait(
                    viewState = state,
                    viewEvent = viewEvent,
                    modifier = modifier,
                )
            }
        }
    }
}

@Composable
fun SummerSalePromoScreenPortrait(
    viewState: SummerSalePromoViewState,
    viewEvent: SummerSalePromoViewEvent,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(ProtonTheme.colors.backgroundNorm)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Image(
                painter = painterResource(viewState.backgroundResId),
                contentDescription = null,
                contentScale = ContentScale.FillWidth,
                modifier = Modifier
                    .fillMaxWidth()
            )
            Column(
                modifier = modifier
                    .verticalScroll(rememberScrollState())
                    .weight(1f)
                    .padding(horizontal = ProtonDimens.DefaultSpacing),
                verticalArrangement = Arrangement.Center,
            ) {
                viewState.items.forEach { item ->
                    Item(
                        imageResId = item.imageResId,
                        title = item.title,
                        modifier = Modifier
                            .heightIn(min = 48.dp)
                            .padding(bottom = ProtonDimens.LargeSpacing)
                    )
                }
            }
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(all = ProtonDimens.DefaultSpacing),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Offer(
                    period = viewState.period,
                    monthlyPrice = viewState.monthlyPrice,
                    monthlyPricePeriod = viewState.monthlyPricePeriod,
                )
                viewState.paymentButtonViewState?.let {
                    val fontScale = LocalConfiguration.current.fontScale
                    val baseHeight = 48.dp
                    val maxHeight = baseHeight * fontScale
                    GetDealButton(
                        title = stringResource(viewState.getDealButtonResId),
                        viewState = it,
                        onPaymentCallback = viewEvent.onPaymentCallback,
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = baseHeight, max = maxHeight)
                    )
                }

                AutoRenewText(
                    autoRenewPrice = viewState.autoRenewPrice,
                    modifier = Modifier
                        .navigationBarsPadding(),
                )
            }
        }
        TopAppBar(
            modifier = Modifier
                .statusBarsPadding()
                .padding(horizontal = ProtonDimens.DefaultSpacing)
                .align(Alignment.TopStart),
            closeAction = viewState.closeAction,
        )
    }
}

@Composable
fun SummerSalePromoScreenLandscape(
    viewState: SummerSalePromoViewState,
    viewEvent: SummerSalePromoViewEvent,
    modifier: Modifier = Modifier,
) {
    ThemelessStatusBarScreen(useDarkIcons = false)
    ProtonTheme(isDark = true) {
        Column(
            modifier = modifier
                .fillMaxSize()
                .paint(
                    painter = painterResource(viewState.backgroundLandResId),
                    contentScale = ContentScale.Crop,
                    alignment = Alignment.TopStart,
                )
                .statusBarsPadding()
                .navigationBarsPadding(),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(),
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(ProtonDimens.DefaultSpacing),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Image(
                        painter = painterResource(viewState.titleImageResId),
                        contentDescription = null,
                        modifier = Modifier.size(width = 375.dp, height = 41.dp)
                    )
                    Image(
                        painter = painterResource(viewState.subtitleImageResId),
                        contentDescription = null,
                        modifier = Modifier.size(width = 125.dp, height = 52.dp)
                    )
                }
                TopAppBar(
                    closeAction = viewState.closeAction,
                    modifier = Modifier.safeDrawingPadding(),
                )
            }
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .weight(1f)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                RowItems(
                    items = viewState.items,
                    modifier = Modifier.padding(horizontal = ProtonDimens.LargeSpacing)
                )
            }
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        top = ProtonDimens.SmallSpacing,
                        start = ProtonDimens.DefaultSpacing,
                        end = ProtonDimens.DefaultSpacing,
                    ),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(ProtonDimens.DefaultSpacing)
            ) {
                viewState.paymentButtonViewState?.let {
                    ClaimOffer(
                        title = stringResource(viewState.getDealButtonResId),
                        viewState = it,
                        period = viewState.period,
                        monthlyPrice = viewState.monthlyPrice,
                        monthlyPricePeriod = viewState.monthlyPricePeriod,
                        onPaymentCallback = viewEvent.onPaymentCallback,
                        id = it.id + 1,
                    )
                }
                AutoRenewText(
                    autoRenewPrice = viewState.autoRenewPrice,
                )
            }
        }
    }
}

@Composable
private fun AutoRenewText(
    autoRenewPrice: String,
    modifier: Modifier = Modifier,
    textAlign: TextAlign = TextAlign.Center,
) {
    val color = if (ProtonTheme.colors.isDark) Color.White else driveCustomBlue
    Text(
        text = autoRenewPrice,
        style = ProtonTheme.typography.body2Medium.copy(
            color = color,
            fontWeight = FontWeight.W400,
        ),
        textAlign = textAlign,
        modifier = modifier.fillMaxWidth(),
    )
}

@Composable
private fun TopAppBar(
    closeAction: Action,
    modifier: Modifier = Modifier,
    backgroundColor: Color = Color.Transparent,
    iconTintColor: Color = ProtonTheme.colors.iconNorm,
    elevation: Dp = 0.dp,
) {
    if (isLandscape) {
        BaseTopAppBar(
            navigationIcon = painterResource((closeAction as Action.Icon).iconResId),
            navigationContentDescription = stringResource(closeAction.contentDescriptionResId),
            onNavigationIcon = closeAction.onAction,
            title = "",
            backgroundColor = backgroundColor,
            modifier = modifier,
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
private fun Offer(
    period: String,
    monthlyPrice: String,
    monthlyPricePeriod: String,
    modifier: Modifier = Modifier,
) {
    val minHeight = if (isLandscape) 48.dp else 68.dp
    val color = if (ProtonTheme.colors.isDark) Color.White else driveCustomBlue
    val bgColorAlpha = if (ProtonTheme.colors.isDark) 0.1f else 0.4f
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(ProtonDimens.SmallSpacing)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = minHeight)
                .border(
                    width = 2.dp,
                    color = color,
                    shape = RoundedCornerShape(16.dp)
                )
                .background(
                    color = Color.White.copy(alpha = bgColorAlpha),
                    shape = RoundedCornerShape(16.dp)
                )
                .clip(RoundedCornerShape(16.dp))
                .padding(horizontal = ProtonDimens.DefaultSpacing),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = period,
                    style = ProtonTheme.typography.body1Medium.copy(color = color),
                )
                Image(
                    painter = painterResource(BasePresentation.drawable.img_summer_sale_discount_small),
                    contentDescription = null,
                    modifier = Modifier.padding(horizontal = ProtonDimens.SmallSpacing),
                )
            }
            Column(
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.End,
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Row(
                        verticalAlignment = Alignment.Bottom,
                    ) {
                        Text(
                            text = monthlyPrice,
                            style = ProtonTheme.typography.body1Medium.copy(color = color)
                        )
                        Text(
                            text = monthlyPricePeriod,
                            style = ProtonTheme.typography.body2Medium.copy(color = color.copy(alpha = 0.7f)),
                            modifier = Modifier.padding(start = 4.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun GetDealButton(
    title: String,
    viewState: ProtonPaymentButtonViewState,
    modifier: Modifier = Modifier,
    id: Int = viewState.id,
    onPaymentCallback: (ProtonPaymentEvent) -> Unit,
) {
    AndroidView(
        factory = { context ->
            val themedContext = ContextThemeWrapper(
                context,
                R.style.ThemeOverlay_Drive_PaymentButton
            )
            ProtonPaymentButton(themedContext)
                .apply {
                    this.id = id
                    layoutParams = ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.WRAP_CONTENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT
                    )
                    buttonText = title
                }
        },
        update = { view ->
            view.apply {
                userId = viewState.userId
                plan = viewState.plan
                currency = viewState.currency
                cycle = viewState.cycle
                paymentProvider = null
                setOnEventListener { event -> onPaymentCallback(event) }
            }
        },
        modifier = modifier,
    )
}

@Composable
fun ClaimOffer(
    title: String,
    viewState: ProtonPaymentButtonViewState,
    period: String,
    monthlyPrice: String,
    monthlyPricePeriod: String,
    modifier: Modifier = Modifier,
    id: Int = viewState.id,
    onPaymentCallback: (ProtonPaymentEvent) -> Unit,
) {
    val color = if (ProtonTheme.colors.isDark) Color.White else driveCustomBlue
    Row(
        modifier = modifier
            .defaultMinSize(minHeight = 56.dp)
            .height(IntrinsicSize.Min)
            .padding(start = 24.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
    ) {
        GetDealButton(
            title = title,
            viewState = viewState,
            modifier = Modifier
                .zIndex(1f)
                .widthIn(min = 200.dp)
                .fillMaxHeight(),
            id = id,
            onPaymentCallback = onPaymentCallback,
        )
        Row(
            modifier = Modifier
                .zIndex(0f)
                .offset(x = (-48).dp)
                .background(
                    color = Color.Transparent,
                    shape = RoundedCornerShape(100.dp)
                )
                .border(
                    width = 2.dp,
                    color = ProtonTheme.colors.brandNorm,
                    shape = RoundedCornerShape(100.dp)
                )
                .clip(RoundedCornerShape(100.dp))
                .padding(start = 56.dp, end = 16.dp)
                .heightIn(min = 56.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = period,
                style = ProtonTheme.typography.body1Medium.copy(color = color),
            )
            Image(
                painter = painterResource(BasePresentation.drawable.img_summer_sale_discount_small),
                contentDescription = null,
                modifier = Modifier.padding(horizontal = ProtonDimens.SmallSpacing),
            )
            Column(
                modifier = Modifier
                    .padding(start = ProtonDimens.DefaultSpacing, end = 10.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.End,
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Row(
                        verticalAlignment = Alignment.Bottom,
                    ) {
                        Text(
                            text = monthlyPrice,
                            style = ProtonTheme.typography.body1Medium.copy(color = color)
                        )
                        Text(
                            text = monthlyPricePeriod,
                            style = ProtonTheme.typography.body2Medium.copy(color = color.copy(alpha = 0.7f)),
                            modifier = Modifier.padding(start = 4.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun Item(
    imageResId: Int,
    title: String,
    modifier: Modifier = Modifier
) {
    val color = if (ProtonTheme.colors.isDark) Color.White else driveCustomBlue
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(ProtonDimens.DefaultSpacing)
    ) {
        Image(
            painter = painterResource(imageResId),
            contentDescription = null,
            modifier = Modifier.size(40.dp),
        )
        Text(
            text = title,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            style = ProtonTheme.typography.subheadline.copy(
                color = color,
                fontWeight = FontWeight.Light,
            ),
        )
    }
}

@Composable
fun RowItems(
    items: Set<SummerSalePromoViewState.Item>,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth(0.9f),
        horizontalArrangement = Arrangement.SpaceEvenly,
    ) {
        items.forEach { item ->
            CardItem(
                imageResId = if (isLandscape) item.imageLandResId else item.imageResId,
                title = item.title,
                modifier = Modifier
                    .padding(horizontal = ProtonDimens.DefaultSpacing)
                    .weight(1f)
            )
        }
    }
}

@Composable
private fun CardItem(
    imageResId: Int,
    title: String,
    modifier: Modifier = Modifier
) {
    val color = if (ProtonTheme.colors.isDark) Color.White else driveCustomBlue
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Image(
            painter = painterResource(imageResId),
            contentDescription = null,
            modifier = Modifier.size(32.dp),
        )
        Text(
            text = title,
            maxLines = 4,
            overflow = TextOverflow.Ellipsis,
            style = ProtonTheme.typography.body1Regular.copy(color = color),
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = ProtonDimens.SmallSpacing),
        )
    }
}

private val driveCustomBlue = Color(0xFF372580)
