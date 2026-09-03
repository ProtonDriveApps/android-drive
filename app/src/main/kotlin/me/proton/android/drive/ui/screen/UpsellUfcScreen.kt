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

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.Divider
import androidx.compose.material.Icon
import androidx.compose.material.Surface
import androidx.compose.material.TopAppBar
import androidx.compose.material.Text
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import me.proton.android.drive.ui.effect.UpsellUfcEffect
import me.proton.android.drive.ui.viewevent.UpsellUfcViewEvent
import me.proton.android.drive.ui.viewmodel.UpsellUfcViewModel
import me.proton.android.drive.ui.viewstate.UpsellUfcViewState
import me.proton.core.compose.component.DeferredCircularProgressIndicator
import me.proton.core.compose.component.ProtonButton
import me.proton.core.compose.component.protonButtonColors
import me.proton.core.compose.theme.ProtonDimens
import me.proton.core.compose.theme.ProtonTheme
import me.proton.core.drive.base.domain.extension.flowOf
import me.proton.core.drive.base.presentation.common.Action
import me.proton.core.drive.base.presentation.component.ThemelessStatusBarScreen
import me.proton.core.drive.base.presentation.component.TopBarActions
import me.proton.core.drive.base.presentation.component.list.ListError
import me.proton.core.drive.base.presentation.extension.isCompactHeight
import me.proton.core.drive.base.presentation.extension.isLandscape
import me.proton.core.drive.base.presentation.extension.shadow
import me.proton.core.drive.base.presentation.R as BasePresentation
import me.proton.core.drive.base.presentation.component.TopAppBar as BaseTopAppBar
import me.proton.core.drive.i18n.R as I18N
import me.proton.core.presentation.R as CorePresentation

@Composable
fun UpsellUfcScreen(
    navigateToSubscription: () -> Unit,
    navigateBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val viewModel = hiltViewModel<UpsellUfcViewModel>()
    val viewState by viewModel.viewState.collectAsStateWithLifecycle(
        viewModel.initialViewState
    )
    val viewEvent = remember {
        viewModel.viewEvent(
            navigateBack = navigateBack,
        )
    }
    LaunchedEffect(Unit) {
        viewEvent.onShown()
    }
    LaunchedEffect(Unit) {
        viewModel.effect.collect { effect ->
            when (effect) {
                UpsellUfcEffect.RedirectToSubscription -> navigateToSubscription()
            }
        }
    }
    Upsell(
        viewState = viewState,
        viewEvent = viewEvent,
        modifier = modifier,
    )
}

@Composable
fun Upsell(
    viewState: UpsellUfcViewState,
    viewEvent: UpsellUfcViewEvent,
    modifier: Modifier = Modifier,
) {
    ThemelessStatusBarScreen(useDarkIcons = false)
    ProtonTheme(isDark = true) {
        if (isCompactHeight()) {
            UpsellCompact(
                viewState = viewState,
                viewEvent = viewEvent,
                modifier = modifier,
            )
        } else {
            UpsellFull(
                viewState = viewState,
                viewEvent = viewEvent,
                modifier = modifier,
            )
        }
    }
}

@Composable
private fun UpsellFull(
    viewState: UpsellUfcViewState,
    viewEvent: UpsellUfcViewEvent,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(Color(0xFF1D121D), Color(0xFF6D4AFF)),
                )
            ),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        UpsellTopAppBar(
            modifier = Modifier.safeDrawingPadding(),
            closeAction = viewState.closeAction,
        )
        Image(
            painter = painterResource(BasePresentation.drawable.img_upsell_header),
            contentDescription = null,
        )
        UpsellState(
            viewState = viewState,
            viewEvent = viewEvent,
            modifier = Modifier.fillMaxSize(),
        )
    }
}

@Composable
private fun UpsellCompact(
    viewState: UpsellUfcViewState,
    viewEvent: UpsellUfcViewEvent,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(Color(0xFF1D121D), Color(0xFF6D4AFF)),
                )
            ),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Row(
            modifier = Modifier
                .statusBarsPadding()
                .fillMaxSize(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                UpsellTopAppBar(closeAction = viewState.closeAction)
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) {
                    if (viewState is UpsellUfcViewState.Content) {
                        UpsellTitleArea(
                            title = viewState.title,
                            subtitle = viewState.subtitle,
                            modifier = Modifier.align(Alignment.TopCenter),
                        )
                    }
                    Image(
                        painter = painterResource(BasePresentation.drawable.img_upsell_header),
                        contentDescription = null,
                    )
                }
            }
            UpsellState(
                viewState = viewState,
                viewEvent = viewEvent,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxSize(),
            )
        }
    }
}

@Composable
private fun UpsellState(
    viewState: UpsellUfcViewState,
    viewEvent: UpsellUfcViewEvent,
    modifier: Modifier = Modifier,
) {
    AnimatedContent(
        targetState = viewState,
        transitionSpec = {
            slideInVertically(initialOffsetY = { it }) + fadeIn() togetherWith fadeOut()
        },
        modifier = modifier,
    ) { state ->
        when (state) {
            is UpsellUfcViewState.Loading -> UpsellLoading()
            is UpsellUfcViewState.Error -> UpsellError(state)
            is UpsellUfcViewState.Content -> UpsellContent(state, viewEvent)
        }
    }
}

@Composable
private fun UpsellLoading(modifier: Modifier = Modifier) {
    Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        DeferredCircularProgressIndicator()
    }
}

@Composable
private fun UpsellError(error: UpsellUfcViewState.Error) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.BottomCenter) {
        ListError(
            message = error.message,
            actionResId = error.actionResId,
            onAction = error.onAction,
        )
    }
}

@Composable
private fun UpsellContent(
    content: UpsellUfcViewState.Content,
    viewEvent: UpsellUfcViewEvent,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxSize()) {
        if (!isCompactHeight()) {
            UpsellTitleArea(
                title = content.title,
                subtitle = content.subtitle,
            )
        }
        UpsellComparisonTable(
            freePlanHeader = content.freePlanColumnTitle,
            planHeader = content.planColumnTitle,
            storageLabel = content.storageLabel,
            storageFree = content.storageFree,
            storagePlan = content.storagePlan,
            storagePlanSubtitle = content.storagePlanSubtitle,
            versionHistoryLabel = content.versionHistoryLabel,
            versionHistoryFree = content.versionHistoryFree,
            versionHistoryPlan = content.versionHistoryPlan,
            shareWithEditAccessLabel = content.shareWithEditAccessLabel,
            shareWithEditAccessFree = content.shareWithEditAccessFree,
            shareWithEditAccessPlan = content.shareWithEditAccessPlan,
            modifier = Modifier.weight(1f),
        )
        UpsellClaimOfferArea(
            buttonTitle = content.buttonTitle,
            offerDescription = content.footer,
            isPaymentLoading = content.isPaymentLoading,
            onPurchaseClicked = {
                viewEvent.onPurchaseClicked(content.productId, content.offerToken)
            },
        )
    }
}

@Composable
private fun UpsellTopAppBar(
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
private fun UpsellTitleArea(
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier,
) {
    val topSpacing = if (isCompactHeight()) 0.dp else ProtonDimens.LargeSpacing
    Column(modifier = modifier) {
        Text(
            text = title,
            style = ProtonTheme.typography.headline,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = topSpacing, bottom = ProtonDimens.SmallSpacing),
        )
        Text(
            text = subtitle,
            style = ProtonTheme.typography.body1Regular,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun UpsellComparisonTable(
    freePlanHeader: String,
    planHeader: String,
    storageLabel: String,
    storageFree: String,
    storagePlan: String,
    storagePlanSubtitle: String,
    versionHistoryLabel: String,
    versionHistoryFree: String,
    versionHistoryPlan: String,
    shareWithEditAccessLabel: String,
    shareWithEditAccessFree: Boolean,
    shareWithEditAccessPlan: Boolean,
    modifier: Modifier = Modifier,
) {
    val rowSpacing = if (isCompactHeight()) ProtonDimens.DefaultSpacing else ProtonDimens.MediumSpacing
    Box(
        modifier = modifier.fillMaxWidth(),
        contentAlignment = Alignment.TopCenter,
    ) {
        Column(
            modifier = Modifier
                .verticalScroll(rememberScrollState())
                .widthIn(max = 400.dp)
                .padding(top = rowSpacing)
                .padding(horizontal = ProtonDimens.DefaultSpacing),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(IntrinsicSize.Min),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(text = "", modifier = Modifier.padding(top = ProtonDimens.LargeSpacing).weight(0.4f))
                Text(
                    text = freePlanHeader,
                    textAlign = TextAlign.Center,
                    style = ProtonTheme.typography.body1Medium,
                    modifier = Modifier.weight(0.3f),
                )
                UpsellPlanHeader(
                    planName = planHeader,
                    modifier = Modifier
                        .weight(0.3f)
                        .fillMaxHeight()
                        .background(
                            color = Color.White.copy(alpha = 0.08f),
                            shape = RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp),
                        ),
                )
            }
            Row(
                modifier = Modifier.height(IntrinsicSize.Min).fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = storageLabel,
                    style = ProtonTheme.typography.body2Regular,
                    modifier = Modifier.padding(vertical = rowSpacing).weight(0.4f),
                )
                Text(
                    text = storageFree,
                    textAlign = TextAlign.Center,
                    style = ProtonTheme.typography.body1Medium,
                    modifier = Modifier.weight(0.3f),
                )
                Column(
                    modifier = Modifier
                        .weight(0.3f)
                        .fillMaxHeight()
                        .background(Color.White.copy(alpha = 0.08f)),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(
                        text = storagePlan,
                        textAlign = TextAlign.Center,
                        style = ProtonTheme.typography.body1Medium.goldGradient(),
                    )
                    Text(
                        text = storagePlanSubtitle,
                        textAlign = TextAlign.Center,
                        style = ProtonTheme.typography.overlineRegular,
                    )
                }
            }
            TableDivider()
            Row(
                modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Min),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = versionHistoryLabel,
                    style = ProtonTheme.typography.body2Regular,
                    modifier = Modifier.padding(vertical = rowSpacing).weight(0.4f),
                )
                Text(
                    text = versionHistoryFree,
                    textAlign = TextAlign.Center,
                    style = ProtonTheme.typography.body1Medium,
                    modifier = Modifier.weight(0.3f),
                )
                Box(
                    modifier = Modifier
                        .weight(0.3f)
                        .fillMaxHeight()
                        .background(Color.White.copy(alpha = 0.08f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = versionHistoryPlan,
                        textAlign = TextAlign.Center,
                        style = ProtonTheme.typography.body1Medium.goldGradient(),
                    )
                }
            }
            TableDivider()
            Row(
                modifier = Modifier.height(IntrinsicSize.Min).fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = shareWithEditAccessLabel,
                    style = ProtonTheme.typography.body2Regular,
                    modifier = Modifier.padding(vertical = rowSpacing).weight(0.4f),
                )
                UpsellShareWithEditAccess(isAllowed = shareWithEditAccessFree, modifier = Modifier.weight(0.3f))
                Box(
                    modifier = Modifier
                        .weight(0.3f)
                        .fillMaxHeight()
                        .background(
                            color = Color.White.copy(alpha = 0.08f),
                            shape = RoundedCornerShape(bottomStart = 8.dp, bottomEnd = 8.dp),
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    UpsellShareWithEditAccess(isAllowed = shareWithEditAccessPlan)
                }
            }
        }
    }
}

@Composable
private fun TableDivider() {
    Divider(
        color = Color.White.copy(alpha = 0.12f),
        modifier = Modifier.fillMaxWidth().height(1.dp),
    )
}

@Composable
private fun UpsellPlanHeader(planName: String, modifier: Modifier = Modifier) {
    Box(contentAlignment = Alignment.Center, modifier = modifier) {
        Text(
            text = planName,
            textAlign = TextAlign.Center,
            style = ProtonTheme.typography.body1Medium,
            modifier = Modifier
                .border(width = 2.dp, brush = goldBrush, shape = RoundedCornerShape(8.dp))
                .background(color = Color.Black.copy(alpha = 0.2f), shape = RoundedCornerShape(8.dp))
                .padding(horizontal = 12.dp, vertical = 8.dp),
        )
    }
}

@Composable
private fun UpsellShareWithEditAccess(isAllowed: Boolean, modifier: Modifier = Modifier) {
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        if (isAllowed) {
            Icon(
                painter = painterResource(CorePresentation.drawable.ic_proton_checkmark_circle_filled),
                tint = Color.White,
                contentDescription = null,
            )
        } else {
            Text(text = "—")
        }
    }
}

@Composable
private fun UpsellClaimOfferArea(
    buttonTitle: String,
    offerDescription: String,
    isPaymentLoading: Boolean,
    onPurchaseClicked: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val spacing = if (isCompactHeight()) ProtonDimens.DefaultSpacing else ProtonDimens.MediumSpacing
    val bgColor = if (isCompactHeight()) Color.Transparent else ProtonTheme.colors.textAccent
    Column(
        modifier = modifier
            .shadow(offsetY = (-2).dp)
            .fillMaxWidth(),
    ) {
        Divider(
            color = Color.White.copy(alpha = 0.12f),
            modifier = Modifier.fillMaxWidth().height(1.dp),
        )
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .background(bgColor)
                .fillMaxWidth()
                .padding(all = spacing),
        ) {
            if (isPaymentLoading) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .widthIn(max = 160.dp)
                        .height(48.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    DeferredCircularProgressIndicator()
                }
            } else {
                ProtonButton(
                    onClick = onPurchaseClicked,
                    elevation = null,
                    shape = ProtonTheme.shapes.large,
                    border = null,
                    colors = ButtonDefaults.protonButtonColors(
                        backgroundColor = Color.White,
                        contentColor = Color(0xFF301E6C),
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .widthIn(max = 160.dp),
                ) {
                    Text(
                        text = buttonTitle,
                        style = ProtonTheme.typography.body1Bold,
                        modifier = Modifier.padding(all = ProtonDimens.SmallSpacing),
                    )
                }
            }
            Text(
                text = offerDescription,
                textAlign = TextAlign.Center,
                style = ProtonTheme.typography.body2Regular,
                modifier = Modifier
                    .navigationBarsPadding()
                    .fillMaxWidth()
                    .padding(top = spacing),
            )
        }
    }
}

private val goldBrush: Brush
    get() = Brush.horizontalGradient(
        colors = listOf(
            Color(0xFFB2781A),
            Color(0xFFDBA42A),
            Color(0xFFFEE083),
            Color(0xFFFDEBA9),
        )
    )

private fun TextStyle.goldGradient(): TextStyle = copy(brush = goldBrush)

private val previewCloseAction = Action.Icon(
    iconResId = CorePresentation.drawable.ic_proton_cross,
    contentDescriptionResId = I18N.string.common_close_action,
    onAction = {},
)

@Preview
@Composable
fun UpsellPreviewLoading() {
    Surface {
        Upsell(
            viewState = UpsellUfcViewState.Loading(previewCloseAction),
            viewEvent = object : UpsellUfcViewEvent {},
        )
    }
}

@Preview
@Composable
fun UpsellPreviewError() {
    Surface {
        Upsell(
            viewState = UpsellUfcViewState.Error(
                closeAction = previewCloseAction,
                message = "Something went wrong. Please try again.",
                actionResId = I18N.string.common_retry_action,
                onAction = {},
            ),
            viewEvent = object : UpsellUfcViewEvent {},
        )
    }
}

@Preview
@Composable
fun UpsellPreviewContent() {
    Surface {
        Upsell(
            viewState = UpsellUfcViewState.Content(
                closeAction = previewCloseAction,
                title = "Try Drive Plus 1 TB for €1",
                subtitle = "Compare plans and choose the best for you",
                freePlanColumnTitle = "Free",
                planColumnTitle = "Plus 1 TB",
                storageLabel = "Storage",
                versionHistoryLabel = "Version history",
                shareWithEditAccessLabel = "Share with edit access",
                storageFree = "5 GB",
                storagePlan = "1 TB",
                storagePlanSubtitle = "200x more than Free",
                versionHistoryFree = "1 week",
                versionHistoryPlan = "10 years",
                shareWithEditAccessFree = false,
                shareWithEditAccessPlan = true,
                buttonTitle = "Get 1 month for €1",
                footer = "Welcome offer. Auto renews at €4.99/month",
                productId = "product_id",
                offerToken = "offer_token",
                isPaymentLoading = false,
            ),
            viewEvent = object : UpsellUfcViewEvent {},
        )
    }
}

@Preview
@Composable
fun UpsellPreviewContentLoading() {
    Surface {
        Upsell(
            viewState = UpsellUfcViewState.Content(
                closeAction = previewCloseAction,
                title = "Try Drive Plus 1 TB for €1",
                subtitle = "Compare plans and choose the best for you",
                freePlanColumnTitle = "Free",
                planColumnTitle = "Plus 1 TB",
                storageLabel = "Storage",
                versionHistoryLabel = "Version history",
                shareWithEditAccessLabel = "Share with edit access",
                storageFree = "5 GB",
                storagePlan = "1 TB",
                storagePlanSubtitle = "200x more than Free",
                versionHistoryFree = "1 week",
                versionHistoryPlan = "10 years",
                shareWithEditAccessFree = false,
                shareWithEditAccessPlan = true,
                buttonTitle = "Get 1 month for €1",
                footer = "Welcome offer. Auto renews at €4.99/month",
                productId = "product_id",
                offerToken = "offer_token",
                isPaymentLoading = true,
            ),
            viewEvent = object : UpsellUfcViewEvent {},
        )
    }
}
