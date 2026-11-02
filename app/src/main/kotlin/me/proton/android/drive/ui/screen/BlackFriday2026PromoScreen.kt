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

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import me.proton.android.drive.ui.viewevent.BlackFriday2026PromoViewEvent
import me.proton.android.drive.ui.viewmodel.BlackFriday2026PromoViewModel
import me.proton.android.drive.ui.viewstate.BlackFriday2026PromoViewState
import me.proton.android.drive.ui.viewstate.BlackFriday2026PromoViewState.CellValue
import me.proton.android.drive.ui.viewstate.BlackFriday2026PromoViewState.ComparisonRow
import me.proton.core.compose.theme.ProtonDimens
import me.proton.core.compose.theme.ProtonTheme
import me.proton.core.compose.theme.headlineSmallNorm
import me.proton.core.drive.base.presentation.common.Action
import me.proton.core.drive.base.presentation.component.DeferredCircularProgressIndicator
import me.proton.core.drive.base.presentation.component.ProtonButton
import me.proton.core.drive.base.presentation.component.ProtonOutlinedButton
import me.proton.core.drive.base.presentation.component.ThemelessStatusBarScreen
import me.proton.core.drive.base.presentation.component.protonButtonColors
import me.proton.core.drive.base.presentation.component.protonOutlinedButtonColors
import me.proton.core.drive.base.presentation.extension.isCompactHeight
import me.proton.core.drive.base.presentation.theme.DriveTheme
import me.proton.core.drive.base.presentation.R as BasePresentation
import me.proton.core.drive.i18n.R as I18N
import me.proton.core.presentation.R as CorePresentation

@Composable
fun BlackFriday2026PromoScreen(
    navigateBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val viewModel = hiltViewModel<BlackFriday2026PromoViewModel>()
    val viewState by viewModel.viewState.collectAsStateWithLifecycle(null)
    val viewEvent = remember {
        viewModel.viewEvent(
            navigateBack = navigateBack,
        )
    }
    LaunchedEffect(Unit) {
        viewEvent.onPromoShown()
    }
    viewState?.let { state ->
        BlackFriday2026Promo(
            viewState = state,
            viewEvent = viewEvent,
            modifier = modifier,
        )
    }
}

@Composable
fun BlackFriday2026Promo(
    viewState: BlackFriday2026PromoViewState,
    viewEvent: BlackFriday2026PromoViewEvent,
    modifier: Modifier = Modifier,
) {
    ThemelessStatusBarScreen(useDarkIcons = false)
    DriveTheme(isDark = true) {
        if (isCompactHeight()) {
            BlackFriday2026PromoCompact(viewState, viewEvent, modifier)
        } else {
            BlackFriday2026PromoFull(viewState, viewEvent, modifier)
        }
    }
}

@Composable
private fun BlackFriday2026PromoFull(
    viewState: BlackFriday2026PromoViewState,
    viewEvent: BlackFriday2026PromoViewEvent,
    modifier: Modifier = Modifier,
) {
    val isLargeFont = LocalDensity.current.fontScale >= LargeFontScale
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(BackgroundColor)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState()),
            ) {
                Header(viewState)
                ComparisonTable(
                    viewState = viewState,
                    modifier = Modifier.padding(
                        horizontal = ProtonDimens.DefaultSpacing,
                        vertical = ProtonDimens.MediumSpacing,
                    ),
                )
                if (isLargeFont) {
                    ClaimOffer(
                        viewState = viewState,
                        viewEvent = viewEvent,
                        modifier = Modifier
                            .navigationBarsPadding()
                            .padding(ProtonDimens.DefaultSpacing),
                    )
                }
            }
            if (!isLargeFont) {
                HorizontalDivider(color = DividerColor)
                ClaimOffer(
                    viewState = viewState,
                    viewEvent = viewEvent,
                    modifier = Modifier
                        .navigationBarsPadding()
                        .padding(ProtonDimens.DefaultSpacing),
                )
            }
        }
        CloseButton(
            closeAction = viewState.closeAction,
            modifier = Modifier
                .statusBarsPadding()
                .padding(ProtonDimens.DefaultSpacing)
                .align(Alignment.TopEnd),
        )
    }
}

@Composable
private fun BlackFriday2026PromoCompact(
    viewState: BlackFriday2026PromoViewState,
    viewEvent: BlackFriday2026PromoViewEvent,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(BackgroundColor)
            .safeDrawingPadding(),
    ) {
        Row(modifier = Modifier.fillMaxSize()) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(),
                contentAlignment = Alignment.Center,
            ) {
                Header(viewState)
            }
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(),
            ) {
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                        .padding(ProtonDimens.DefaultSpacing),
                    verticalArrangement = Arrangement.spacedBy(ProtonDimens.DefaultSpacing),
                ) {
                    ComparisonTable(viewState = viewState)
                    OfferCard(viewState)
                }
                HorizontalDivider(color = DividerColor)
                ClaimOfferActions(
                    viewState = viewState,
                    viewEvent = viewEvent,
                    modifier = Modifier.padding(ProtonDimens.DefaultSpacing),
                )
            }
        }
        CloseButton(
            closeAction = viewState.closeAction,
            modifier = Modifier
                .padding(ProtonDimens.SmallSpacing)
                .align(Alignment.TopStart),
        )
    }
}

@Composable
private fun Header(
    viewState: BlackFriday2026PromoViewState,
    modifier: Modifier = Modifier,
) {
    Image(
        painter = painterResource(viewState.headerResId),
        contentDescription = stringResource(viewState.headerDescriptionResId),
        contentScale = ContentScale.FillWidth,
        modifier = modifier.fillMaxWidth(),
    )
}

@Composable
private fun CloseButton(
    closeAction: Action.Icon,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .size(CloseButtonSize)
            .clip(CircleShape)
            .background(CloseButtonColor)
            .clickable(onClick = closeAction.onAction),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            painter = painterResource(closeAction.iconResId),
            contentDescription = stringResource(closeAction.contentDescriptionResId),
            tint = BackgroundColor,
            modifier = Modifier.size(ProtonDimens.SmallIconSize),
        )
    }
}

@Composable
private fun ComparisonTable(
    viewState: BlackFriday2026PromoViewState,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min)
    ) {
        Box(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .fillMaxHeight()
                .width(PlanColumnWidth)
                .clip(RoundedCornerShape(ProtonDimens.DefaultCornerRadius))
                .background(PlanColumnColor)
        )
        Column(modifier = Modifier.fillMaxWidth()) {
            TableRow(
                label = null,
                minHeight = TableHeaderMinHeight,
                currentPlan = {
                    Text(
                        text = viewState.currentPlanColumnTitle,
                        style = ProtonTheme.typography.body1Medium,
                        color = Color.White,
                        textAlign = TextAlign.Center,
                    )
                },
                plan = { PlanBadge(viewState.planColumnTitle) },
            )
            viewState.comparisonRows.forEachIndexed { index, row ->
                TableRow(
                    label = row.label,
                    currentPlan = { Cell(row.currentPlan, isPlan = false) },
                    plan = { Cell(row.plan, isPlan = true) },
                )
                if (index != viewState.comparisonRows.lastIndex) {
                    HorizontalDivider(color = DividerColor)
                }
            }
        }
    }
}

@Composable
private fun TableRow(
    label: String?,
    currentPlan: @Composable () -> Unit,
    plan: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    minHeight: Dp = if (isCompactHeight()) TableHeaderMinHeight else TableRowMinHeight,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = minHeight),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label.orEmpty(),
            style = ProtonTheme.typography.body1Regular,
            color = Color.White,
            modifier = Modifier
                .weight(1f)
                .padding(vertical = ProtonDimens.SmallSpacing),
        )
        Box(
            modifier = Modifier
                .widthIn(min = CurrentPlanColumnMinWidth)
                .padding(horizontal = ProtonDimens.SmallSpacing),
            contentAlignment = Alignment.Center,
        ) {
            currentPlan()
        }
        Box(
            modifier = Modifier
                .width(PlanColumnWidth)
                .padding(horizontal = ProtonDimens.ExtraSmallSpacing, vertical = ProtonDimens.SmallSpacing),
            contentAlignment = Alignment.Center,
        ) {
            plan()
        }
    }
}

@Composable
private fun PlanBadge(title: String) {
    Text(
        text = title,
        style = ProtonTheme.typography.body1Medium,
        color = OnAccentColor,
        modifier = Modifier
            .clip(RoundedCornerShape(BadgeCornerRadius))
            .background(AccentBrush)
            .padding(horizontal = ProtonDimens.SmallSpacing, vertical = ProtonDimens.ExtraSmallSpacing),
    )
}

@Composable
private fun Cell(value: CellValue, isPlan: Boolean) {
    when (value) {
        is CellValue.Check -> if (value.included) {
            Box(
                modifier = Modifier
                    .size(20.dp)
                    .clip(CircleShape)
                    .background(Color.White),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    painter = painterResource(CorePresentation.drawable.ic_proton_checkmark),
                    contentDescription = null,
                    tint = BackgroundColor,
                    modifier = Modifier.size(14.dp),
                )
            }
        } else {
            Text(text = "-", color = MutedTextColor)
        }
        is CellValue.Value -> Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = value.text,
                style = ProtonTheme.typography.body1Medium,
                color = Color.White,
                fontWeight = if (isPlan) FontWeight.Medium else FontWeight.Normal,
                textAlign = TextAlign.Center,
            )
            value.subtitle?.let { subtitle ->
                Text(
                    text = subtitle,
                    style = ProtonTheme.typography.captionRegular.copy(fontSize = 11.sp),
                    color = Color.White,
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}

@Composable
private fun ClaimOffer(
    viewState: BlackFriday2026PromoViewState,
    viewEvent: BlackFriday2026PromoViewEvent,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(ProtonDimens.SmallSpacing),
    ) {
        OfferCard(viewState)
        ClaimOfferActions(viewState = viewState, viewEvent = viewEvent)
    }
}

@Composable
private fun ClaimOfferActions(
    viewState: BlackFriday2026PromoViewState,
    viewEvent: BlackFriday2026PromoViewEvent,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(ProtonDimens.SmallSpacing),
    ) {
        if (viewState.productId != null && viewState.offerToken != null) {
            ClaimOfferButton(
                title = stringResource(viewState.claimOfferButtonResId),
                isPaymentLoading = viewState.isPaymentLoading,
                onClick = { viewEvent.onPurchaseClicked(viewState.productId, viewState.offerToken) },
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = ProtonDimens.DefaultButtonMinHeight),
            )
        }
        viewState.stayOnPlanButtonResId?.let { stayOnPlanButtonResId ->
            ProtonOutlinedButton(
                onClick = viewEvent.onStayOnPlan,
                enabled = !viewState.isPaymentLoading,
                colors = ButtonDefaults.protonOutlinedButtonColors(backgroundColor = BackgroundColor),
                shape = ButtonShape,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = ProtonDimens.DefaultButtonMinHeight),
            ) {
                Text(text = stringResource(stayOnPlanButtonResId))
            }
        }
        viewState.footer?.let { footer ->
            Text(
                text = footer,
                style = ProtonTheme.typography.captionRegular,
                color = MutedTextColor,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Composable
private fun OfferCard(
    viewState: BlackFriday2026PromoViewState,
    modifier: Modifier = Modifier,
) {
    val isLargeFont = LocalDensity.current.fontScale >= LargeFontScale
    val cardModifier = modifier
        .fillMaxWidth()
        .heightIn(min = OfferCardMinHeight)
        .border(
            width = 2.dp,
            brush = AccentBrush,
            shape = RoundedCornerShape(ProtonDimens.LargeCornerRadius),
        )
        .padding(horizontal = ProtonDimens.DefaultSpacing, vertical = ProtonDimens.SmallSpacing)
    if (isLargeFont) {
        Column(
            modifier = cardModifier,
            verticalArrangement = Arrangement.spacedBy(ProtonDimens.SmallSpacing),
        ) {
            OfferPeriod(viewState)
            OfferPrices(viewState, Modifier.align(Alignment.End))
        }
    } else {
        Row(
            modifier = cardModifier,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            OfferPeriod(viewState)
            Spacer(modifier = Modifier.weight(1f))
            OfferPrices(viewState)
        }
    }
}

@Composable
private fun OfferPeriod(viewState: BlackFriday2026PromoViewState) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(ProtonDimens.SmallSpacing),
    ) {
        Text(
            text = viewState.period,
            style = ProtonTheme.typography.body1Regular,
            color = Color.White,
        )
        PlanBadge(viewState.discount)
    }
}

@Composable
private fun OfferPrices(
    viewState: BlackFriday2026PromoViewState,
    modifier: Modifier = Modifier,
) {
    if (viewState.price == null) return
    Column(
        horizontalAlignment = Alignment.End,
        modifier = modifier.padding(start = ProtonDimens.SmallSpacing),
    ) {
        PriceLine(
            price = viewState.price,
            period = "/${stringResource(I18N.string.common_year).lowercase()}",
            priceStyle = ProtonTheme.typography.headlineSmallNorm.copy(color = Color.White),
        )
        viewState.monthlyPrice?.let { monthlyPrice ->
            PriceLine(
                price = monthlyPrice,
                period = "/${stringResource(I18N.string.common_month).lowercase()}",
                priceStyle = ProtonTheme.typography.captionRegular.copy(color = MutedTextColor),
            )
        }
    }
}

@Composable
private fun PriceLine(
    price: String,
    period: String,
    priceStyle: TextStyle,
) {
    val periodStyle = ProtonTheme.typography.captionRegular
    Text(
        text = buildAnnotatedString {
            withStyle(priceStyle.toSpanStyle()) { append(price) }
            withStyle(SpanStyle(color = MutedTextColor, fontSize = periodStyle.fontSize)) { append(" $period") }
        },
        textAlign = TextAlign.End,
    )
}

@Composable
private fun ClaimOfferButton(
    title: String,
    isPaymentLoading: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    if (isPaymentLoading) {
        Box(modifier = modifier, contentAlignment = Alignment.Center) {
            DeferredCircularProgressIndicator()
        }
    } else {
        ProtonButton(
            onClick = onClick,
            elevation = null,
            shape = ButtonShape,
            border = null,
            colors = ButtonDefaults.protonButtonColors(
                backgroundColor = ProtonTheme.colors.brandNorm,
                contentColor = Color.White,
            ),
            modifier = modifier,
        ) {
            Text(text = title)
        }
    }
}

private const val LargeFontScale = 1.5f
private val CloseButtonSize = 32.dp
private val ButtonShape = RoundedCornerShape(percent = 50)
private val BadgeCornerRadius = 6.dp
private val PlanColumnWidth = 114.dp
private val CurrentPlanColumnMinWidth = 72.dp
private val TableHeaderMinHeight = 64.dp
private val TableRowMinHeight = 96.dp
private val OfferCardMinHeight = 68.dp
private val BackgroundColor = Color(0xFF030712)
private val PlanColumnColor = Color(0xFF1F2029)
private val DividerColor = Color(0xFF262833)
private val CloseButtonColor = Color(0xCCFFFFFF)
private val MutedTextColor = Color(0xFFC2C1C0)
private val OnAccentColor = Color(0xFF1B1340)
private val AccentBrush = Brush.linearGradient(
    colors = listOf(Color(0xFFFFFAD7), Color(0xFFFFC283), Color(0xFFFF8383)),
)

@Composable
private fun previewViewState(withPrices: Boolean = true) = BlackFriday2026PromoViewState(
    headerResId = BasePresentation.drawable.bg_black_friday_2026_promo_port,
    headerDescriptionResId = I18N.string.promo_black_friday_2026_header_description,
    closeAction = Action.Icon(
        iconResId = CorePresentation.drawable.ic_proton_cross,
        contentDescriptionResId = I18N.string.common_close_action,
        onAction = {},
    ),
    currentPlanColumnTitle = stringResource(I18N.string.promo_black_friday_2026_free_column_title),
    planColumnTitle = stringResource(I18N.string.promo_black_friday_2026_plus_column_title),
    comparisonRows = listOf(
        ComparisonRow(
            label = stringResource(I18N.string.promo_upsell_storage_label),
            currentPlan = CellValue.Value(stringResource(I18N.string.promo_black_friday_2026_storage_free)),
            plan = CellValue.Value(
                text = stringResource(I18N.string.promo_black_friday_2026_storage_plus),
                subtitle = stringResource(I18N.string.promo_upsell_storage_plan_subtitle, 40, "Free"),
            ),
        ),
        ComparisonRow(
            label = stringResource(I18N.string.promo_upsell_version_history_label),
            currentPlan = CellValue.Value(pluralStringResource(I18N.plurals.common_x_days, 7, 7)),
            plan = CellValue.Value(pluralStringResource(I18N.plurals.common_x_years, 10, 10)),
        ),
        ComparisonRow(
            label = stringResource(I18N.string.promo_upsell_share_with_edit_access_label),
            currentPlan = CellValue.Check(included = false),
            plan = CellValue.Check(included = true),
        ),
    ),
    period = pluralStringResource(I18N.plurals.common_x_months, 12, 12),
    discount = stringResource(I18N.string.promo_black_friday_2026_discount, 50),
    price = "$29.88".takeIf { withPrices },
    monthlyPrice = stringResource(I18N.string.promo_offer_per_month_details, "$2.49").takeIf { withPrices },
    footer = stringResource(I18N.string.promo_black_friday_2026_footer, "$47.88").takeIf { withPrices },
    claimOfferButtonResId = I18N.string.promo_claim_offer_button,
    productId = "product_id".takeIf { withPrices },
    offerToken = "offer_token".takeIf { withPrices },
)

@Composable
private fun previewUpgradeViewState() = previewViewState().copy(
    currentPlanColumnTitle = stringResource(I18N.string.promo_black_friday_2026_plus_column_title),
    planColumnTitle = stringResource(I18N.string.promo_black_friday_2026_unlimited_column_title),
    comparisonRows = listOf(
        ComparisonRow(
            label = stringResource(I18N.string.promo_upsell_storage_label),
            currentPlan = CellValue.Value(stringResource(I18N.string.promo_black_friday_2026_storage_plus)),
            plan = CellValue.Value(stringResource(I18N.string.promo_black_friday_2026_storage_unlimited)),
        ),
    ) + listOf(
        I18N.string.promo_black_friday_2026_premium_access_label,
        I18N.string.promo_black_friday_2026_aliases_label,
        I18N.string.promo_black_friday_2026_custom_email_domain_label,
        I18N.string.promo_black_friday_2026_priority_support_label,
    ).map { labelResId ->
        ComparisonRow(
            label = stringResource(labelResId),
            currentPlan = CellValue.Check(included = false),
            plan = CellValue.Check(included = true),
        )
    },
    price = "$77.88",
    monthlyPrice = stringResource(I18N.string.promo_offer_per_month_details, "$6.49"),
    footer = stringResource(I18N.string.promo_black_friday_2026_footer, "$155.88"),
    stayOnPlanButtonResId = I18N.string.promo_black_friday_2026_stay_on_plan_button,
)

private val previewViewEvent = object : BlackFriday2026PromoViewEvent {}

@Preview(name = "Portrait", device = Devices.PIXEL_7)
@Composable
private fun BlackFriday2026PromoPortraitPreview() {
    BlackFriday2026Promo(viewState = previewViewState(), viewEvent = previewViewEvent)
}

@Preview(name = "Portrait loading", device = Devices.PIXEL_7)
@Composable
private fun BlackFriday2026PromoPortraitLoadingPreview() {
    BlackFriday2026Promo(viewState = previewViewState(withPrices = false), viewEvent = previewViewEvent)
}

@Preview(name = "Landscape", device = "spec:width=915dp,height=412dp,orientation=landscape")
@Composable
private fun BlackFriday2026PromoLandscapePreview() {
    BlackFriday2026Promo(viewState = previewViewState(), viewEvent = previewViewEvent)
}

@Preview(name = "Maximum font size", device = Devices.PIXEL_7, fontScale = 2f)
@Composable
private fun BlackFriday2026PromoLargeFontPreview() {
    BlackFriday2026Promo(viewState = previewViewState(), viewEvent = previewViewEvent)
}

@Preview(name = "Upgrade portrait", device = Devices.PIXEL_7)
@Composable
private fun BlackFriday2026PromoUpgradePortraitPreview() {
    BlackFriday2026Promo(viewState = previewUpgradeViewState(), viewEvent = previewViewEvent)
}

@Preview(name = "Upgrade landscape", device = "spec:width=915dp,height=412dp,orientation=landscape")
@Composable
private fun BlackFriday2026PromoUpgradeLandscapePreview() {
    BlackFriday2026Promo(viewState = previewUpgradeViewState(), viewEvent = previewViewEvent)
}

@Preview(name = "Upgrade maximum font size", device = Devices.PIXEL_7, fontScale = 2f)
@Composable
private fun BlackFriday2026PromoUpgradeLargeFontPreview() {
    BlackFriday2026Promo(viewState = previewUpgradeViewState(), viewEvent = previewViewEvent)
}
