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

import android.view.ContextThemeWrapper
import android.view.ViewGroup
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
import androidx.compose.material.Text
import androidx.compose.material.TopAppBar
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
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import me.proton.android.drive.R
import me.proton.android.drive.ui.component.PaymentUnredeemedHandler
import me.proton.android.drive.ui.effect.PaymentErrorEffect
import me.proton.android.drive.ui.viewevent.UpsellViewEvent
import me.proton.android.drive.ui.viewmodel.UpsellViewModel
import me.proton.android.drive.ui.viewstate.ProtonPaymentButtonViewState
import me.proton.android.drive.ui.viewstate.UpsellViewState
import me.proton.core.compose.component.DeferredCircularProgressIndicator
import me.proton.core.compose.component.ProtonButton
import me.proton.core.compose.component.protonButtonColors
import me.proton.core.compose.theme.ProtonDimens
import me.proton.core.compose.theme.ProtonTheme
import me.proton.core.domain.entity.UserId
import me.proton.core.drive.base.domain.extension.flowOf
import me.proton.core.drive.base.presentation.common.Action
import me.proton.core.drive.base.presentation.component.ThemelessStatusBarScreen
import me.proton.core.drive.base.presentation.component.TopBarActions
import me.proton.core.drive.base.presentation.component.list.ListError
import me.proton.core.drive.base.presentation.extension.isCompactHeight
import me.proton.core.drive.base.presentation.extension.isLandscape
import me.proton.core.drive.base.presentation.extension.shadow
import me.proton.core.payment.presentation.view.ProtonPaymentButton
import me.proton.core.payment.presentation.viewmodel.ProtonPaymentEvent
import me.proton.core.plan.domain.entity.DynamicPlan
import me.proton.core.plan.domain.entity.DynamicPlanState
import me.proton.core.drive.base.presentation.R as BasePresentation
import me.proton.core.drive.base.presentation.component.TopAppBar as BaseTopAppBar
import me.proton.core.drive.i18n.R as I18N
import me.proton.core.presentation.R as CorePresentation

@Composable
fun UpsellScreen(
    navigateToSubscription: () -> Unit,
    navigateBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val viewModel = hiltViewModel<UpsellViewModel>()
    val viewState by viewModel.viewState.collectAsStateWithLifecycle(
        viewModel.initialViewState
    )
    val viewEvent = remember {
        viewModel.viewEvent(
            navigateToSubscription = navigateToSubscription,
            navigateBack = navigateBack,
        )
    }
    LaunchedEffect(Unit) {
        viewEvent.onShown()
    }
    Upsell(
        viewState = viewState,
        viewEvent = viewEvent,
        paymentErrorEffect = viewModel.paymentErrorEffect,
        claimOffer = ::ClaimOffer,
        modifier = modifier,
    )
}

@Composable
fun Upsell(
    viewState: UpsellViewState,
    viewEvent: UpsellViewEvent,
    paymentErrorEffect: Flow<PaymentErrorEffect>,
    claimOffer: @Composable (String, ProtonPaymentButtonViewState, Modifier, Int, (ProtonPaymentEvent) -> Unit) -> Unit,
    modifier: Modifier = Modifier,
) {
    PaymentUnredeemedHandler(
        paymentErrorEffect = paymentErrorEffect,
        viewEvent = viewEvent,
    )

    ThemelessStatusBarScreen(useDarkIcons = false)
    ProtonTheme(isDark = true) {
        if (isCompactHeight()) {
            UpsellCompact(
                viewState = viewState,
                viewEvent = viewEvent,
                claimOffer = claimOffer,
                modifier = modifier,
            )
        } else {
            Upsell(
                viewState = viewState,
                viewEvent = viewEvent,
                claimOffer = claimOffer,
                modifier = modifier,
            )
        }
    }
}

@Composable
fun Upsell(
    viewState: UpsellViewState,
    viewEvent: UpsellViewEvent,
    claimOffer: @Composable (String, ProtonPaymentButtonViewState, Modifier, Int, (ProtonPaymentEvent) -> Unit) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF1D121D),
                        Color(0xFF6D4AFF)
                    ),
                )
            ),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        TopAppBar(
            modifier = Modifier
                .safeDrawingPadding(),
            closeAction = viewState.closeAction,
        )
        Image(
            painter = painterResource(BasePresentation.drawable.img_upsell_header),
            contentDescription = null,
        )
        UpsellState(
            viewState = viewState,
            viewEvent = viewEvent,
            claimOffer = claimOffer,
            modifier = Modifier.fillMaxSize(),
        )
    }
}

@Composable
fun UpsellCompact(
    viewState: UpsellViewState,
    viewEvent: UpsellViewEvent,
    claimOffer: @Composable (String, ProtonPaymentButtonViewState, Modifier, Int, (ProtonPaymentEvent) -> Unit) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF1D121D),
                        Color(0xFF6D4AFF)
                    ),
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
                TopAppBar(
                    closeAction = viewState.closeAction,
                )
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    if (viewState is UpsellViewState.Content) {
                        TitleArea(
                            title = viewState.title,
                            subtitle = viewState.subtitle,
                            modifier = Modifier.align(Alignment.TopCenter)
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
                claimOffer = claimOffer,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxSize(),
            )
        }
    }
}

@Composable
fun UpsellState(
    viewState: UpsellViewState,
    viewEvent: UpsellViewEvent,
    claimOffer: @Composable (String, ProtonPaymentButtonViewState, Modifier, Int, (ProtonPaymentEvent) -> Unit) -> Unit,
    modifier: Modifier = Modifier,
) {
    AnimatedContent(
        targetState = viewState,
        transitionSpec = {
            slideInVertically(initialOffsetY = { fullHeight -> fullHeight }) + fadeIn() togetherWith fadeOut()
        },
        modifier = modifier,
    ) { state ->
        state
            .onLoading {
                UpsellLoading()
            }
            .onError { error: UpsellViewState.Error ->
                UpsellError(error)
            }
            .onContent { content: UpsellViewState.Content ->
                UpsellContent(
                    content = content,
                    claimOffer = claimOffer,
                    onPaymentCallback = viewEvent.onPaymentCallback
                )
            }
    }
}

@Composable
private fun UpsellLoading(
    modifier: Modifier = Modifier,
) {
    Box(
        modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        DeferredCircularProgressIndicator()
    }
}

@Composable
private fun UpsellError(
    error: UpsellViewState.Error,
) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.BottomCenter,
    ) {
        ListError(
            message = error.message,
            actionResId = error.actionResId,
            onAction = error.onAction,
        )
    }
}

@Composable
private fun UpsellContent(
    content: UpsellViewState.Content,
    claimOffer: @Composable (String, ProtonPaymentButtonViewState, Modifier, Int, (ProtonPaymentEvent) -> Unit) -> Unit,
    modifier: Modifier = Modifier,
    onPaymentCallback: (ProtonPaymentEvent) -> Unit,
) {
    Column(
        modifier = modifier.fillMaxSize()
    ) {
        if (isCompactHeight().not()) {
            TitleArea(
                title = content.title,
                subtitle = content.subtitle,
            )
        }
        ComparisonTable(
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
        ClaimOfferArea(
            buttonTitle = content.buttonTitle,
            viewState = content.protonPaymentButtonViewState,
            offerDescription = content.footer,
            onPaymentCallback = onPaymentCallback,
            claimOffer = claimOffer,
        )
    }
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
private fun ComparisonTable(
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
        modifier = modifier
            .fillMaxWidth(),
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
                Text(
                    text = "",
                    modifier = Modifier
                        .padding(top = ProtonDimens.LargeSpacing)
                        .weight(0.4f),
                )
                Text(
                    text = freePlanHeader,
                    textAlign = TextAlign.Center,
                    style = ProtonTheme.typography.body1Medium,
                    modifier = Modifier.weight(0.3f),

                    )
                PlanHeader(
                    planName = planHeader,
                    modifier = Modifier
                        .weight(0.3f)
                        .fillMaxHeight()
                        .background(
                            color = Color.White.copy(alpha = 0.08f),
                            shape = RoundedCornerShape(
                                topStart = 8.dp,
                                topEnd = 8.dp,
                                bottomStart = 0.dp,
                                bottomEnd = 0.dp,
                            ),
                        ),
                )
            }
            Row(
                modifier = Modifier
                    .height(IntrinsicSize.Min)
                    .fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = storageLabel,
                    style = ProtonTheme.typography.body2Regular,
                    modifier = Modifier
                        .padding(vertical = rowSpacing)
                        .weight(0.4f),
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
            Divider(
                color = Color.White.copy(alpha = 0.12f),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp),
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(IntrinsicSize.Min),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = versionHistoryLabel,
                    style = ProtonTheme.typography.body2Regular,
                    modifier = Modifier
                        .padding(vertical = rowSpacing)
                        .weight(0.4f),
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
            Divider(
                color = Color.White.copy(alpha = 0.12f),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp),
            )
            Row(
                modifier = Modifier
                    .height(IntrinsicSize.Min)
                    .fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = shareWithEditAccessLabel,
                    style = ProtonTheme.typography.body2Regular,
                    modifier = Modifier
                        .padding(vertical = rowSpacing)
                        .weight(0.4f),
                )
                ShareWithEditAccess(
                    isAllowed = shareWithEditAccessFree,
                    modifier = Modifier.weight(0.3f),
                )
                Box(
                    modifier = Modifier
                        .weight(0.3f)
                        .fillMaxHeight()
                        .background(
                            color = Color.White.copy(alpha = 0.08f),
                            shape = RoundedCornerShape(
                                topStart = 0.dp,
                                topEnd = 0.dp,
                                bottomStart = 8.dp,
                                bottomEnd = 8.dp,
                            ),
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    ShareWithEditAccess(
                        isAllowed = shareWithEditAccessPlan
                    )
                }
            }
        }
    }
}

@Composable
private fun PlanHeader(
    planName: String,
    modifier: Modifier = Modifier,
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
    ) {
        Text(
            text = planName,
            textAlign = TextAlign.Center,
            style = ProtonTheme.typography.body1Medium,
            modifier = Modifier
                .border(
                    width = 2.dp,
                    brush = goldBrush,
                    shape = RoundedCornerShape(8.dp)
                )
                .background(
                    color = Color.Black.copy(
                        alpha = 0.2f
                    ),
                    shape = RoundedCornerShape(8.dp),
                )
                .padding(horizontal = 12.dp, vertical = 8.dp)
        )
    }
}

@Composable
private fun ShareWithEditAccess(
    isAllowed: Boolean,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center,
    ) {
        if (isAllowed) {
            Icon(
                painter = painterResource(CorePresentation.drawable.ic_proton_checkmark_circle_filled),
                tint = Color.White,
                contentDescription = null,
            )
        } else {
            Text(
                text = "—",
            )
        }
    }
}

@Composable
private fun TitleArea(
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier,
) {
    val topSpacing = if (isCompactHeight()) 0.dp else ProtonDimens.LargeSpacing
    Column(
        modifier = modifier,
    ) {
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
            modifier = Modifier
                .fillMaxWidth(),
        )
    }
}

@Composable
private fun ClaimOfferArea(
    buttonTitle: String,
    viewState: ProtonPaymentButtonViewState,
    offerDescription: String,
    claimOffer: @Composable (String, ProtonPaymentButtonViewState, Modifier, Int, (ProtonPaymentEvent) -> Unit) -> Unit,
    modifier: Modifier = Modifier,
    onPaymentCallback: (ProtonPaymentEvent) -> Unit,
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
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp),
        )
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .background(bgColor)
                .fillMaxWidth()
                .padding(all = spacing)
        ) {
            claimOffer(
                buttonTitle,
                viewState,
                Modifier
                    .fillMaxWidth()
                    .widthIn(max = 160.dp),
                viewState.id,
                onPaymentCallback,
            )
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

@Composable
private fun ClaimOffer(
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
                R.style.ThemeOverlay_Drive_PaymentButton_Upsell
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
inline fun UpsellViewState.onLoading(block: @Composable (UpsellViewState.Loading) -> Unit) = apply {
    if (this is UpsellViewState.Loading) {
        block(this)
    }
}

@Composable
inline fun UpsellViewState.onError(block: @Composable (UpsellViewState.Error) -> Unit) = apply {
    if (this is UpsellViewState.Error) {
        block(this)
    }
}

@Composable
inline fun UpsellViewState.onContent(block: @Composable (UpsellViewState.Content) -> Unit) = apply {
    if (this is UpsellViewState.Content) {
        block(this)
    }
}

private val goldBrush: Brush get() = Brush.horizontalGradient(
    colors = listOf(
        Color(0xFFB2781A),
        Color(0xFFDBA42A),
        Color(0xFFFEE083),
        Color(0xFFFDEBA9),
    )
)

private fun TextStyle.goldGradient(): TextStyle = copy(brush = goldBrush)

@Preview
@Composable
fun UpsellPreview() {
    Surface {
        Upsell(
            viewState = UpsellViewState.Content(
                closeAction = Action.Icon(
                    iconResId = CorePresentation.drawable.ic_proton_cross,
                    contentDescriptionResId = I18N.string.common_close_action,
                    onAction = {},
                ),
                title = "Try Drive Plus 1 TB for €1",
                subtitle = stringResource(I18N.string.promo_upsell_subtitle),
                freePlanColumnTitle = "Free",
                planColumnTitle = "Plus 1 TB",
                storageLabel = stringResource(I18N.string.promo_upsell_storage_label),
                versionHistoryLabel = stringResource(I18N.string.promo_upsell_version_history_label),
                shareWithEditAccessLabel = stringResource(I18N.string.promo_upsell_share_with_edit_access_label),
                storageFree = "5 GB",
                storagePlan = "1 TB",
                storagePlanSubtitle = "200x more than Free",
                versionHistoryFree = "1 week",
                versionHistoryPlan = "10 years",
                shareWithEditAccessFree = false,
                shareWithEditAccessPlan = true,
                buttonTitle = "Get 1 month for €1",
                footer = "Welcome offer. Auto renews at €4.99/month",
                protonPaymentButtonViewState = ProtonPaymentButtonViewState(
                    userId = UserId("user-id"),
                    id = 0,
                    plan = DynamicPlan(
                        name = "Drive Plus 1 TB",
                        order = 0,
                        state = DynamicPlanState.Available,
                        title = "Drive Plus 1 TB",
                        type = null,
                    ),
                    currency = "EUR",
                    cycle = 1,
                )
            ),
            viewEvent = object : UpsellViewEvent {},
            paymentErrorEffect = emptyFlow(),
            claimOffer = { _, _, _, _, _ ->
                ProtonButton(
                    onClick = {},
                    elevation = null,
                    shape = ProtonTheme.shapes.large,
                    border = null,
                    colors = ButtonDefaults.protonButtonColors(
                        backgroundColor = Color.White,
                        contentColor = Color(0xFF301E6C),
                    ),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(
                        text = "Get 1 month for €1",
                        style = ProtonTheme.typography.body1Bold,
                        modifier = Modifier.padding(all = ProtonDimens.SmallSpacing)
                    )
                }
            },
        )
    }
}
