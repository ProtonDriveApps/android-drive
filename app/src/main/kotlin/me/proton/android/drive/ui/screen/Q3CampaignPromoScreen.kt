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
import android.content.ContextWrapper
import android.content.pm.ActivityInfo
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.Divider
import androidx.compose.material.Icon
import androidx.compose.material.Text
import androidx.compose.material.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import me.proton.android.drive.ui.viewevent.Q3CampaignPromoViewEvent
import me.proton.android.drive.ui.viewmodel.Q3CampaignPromoViewModel
import me.proton.android.drive.ui.viewstate.Q3CampaignPromoViewState
import me.proton.core.compose.component.DeferredCircularProgressIndicator
import me.proton.core.compose.component.ProtonButton
import me.proton.core.compose.component.protonButtonColors
import me.proton.core.compose.theme.ProtonDimens
import me.proton.core.compose.theme.ProtonTheme
import me.proton.core.drive.base.domain.extension.flowOf
import me.proton.core.drive.base.presentation.common.Action
import me.proton.core.drive.base.presentation.component.ThemelessStatusBarScreen
import me.proton.core.drive.base.presentation.component.TopBarActions
import me.proton.core.drive.base.presentation.R as BasePresentation
import me.proton.core.drive.i18n.R as I18N
import me.proton.core.presentation.R as CorePresentation


@Composable
fun Q3CampaignPromoScreen(
    navigateBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    LockScreenOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT)
    val viewModel = hiltViewModel<Q3CampaignPromoViewModel>()
    val viewState by viewModel.viewState.collectAsStateWithLifecycle(
        null
    )
    val viewEvent = remember {
        viewModel.viewEvent(
            navigateBack = navigateBack,
        )
    }

    AnimatedVisibility(viewState != null) {
        LaunchedEffect(Unit) {
            viewEvent.onPromoShown()
        }
        viewState?.let { state ->
            Q3CampaignPromoScreenPortrait(
                viewState = state,
                viewEvent = viewEvent,
                modifier = modifier,
            )
        }
    }
}

@Composable
private fun LockScreenOrientation(orientation: Int) {
    val activity = LocalContext.current.findActivity() ?: return
    DisposableEffect(activity, orientation) {
        val originalOrientation = activity.requestedOrientation
        activity.requestedOrientation = orientation
        onDispose { activity.requestedOrientation = originalOrientation }
    }
}

private tailrec fun android.content.Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}

@Composable
fun Q3CampaignPromoScreenPortrait(
    viewState: Q3CampaignPromoViewState,
    viewEvent: Q3CampaignPromoViewEvent,
    modifier: Modifier = Modifier,
) {
    // Forced to dark theme regardless of system setting
    ThemelessStatusBarScreen(useDarkIcons = false)
    ProtonTheme(isDark = true) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Image(
                painter = painterResource(viewState.backgroundResId),
                contentDescription = null,
                contentScale = ContentScale.FillWidth,
                modifier = Modifier.fillMaxWidth()
            )
            Column(
                modifier = modifier
                    .verticalScroll(rememberScrollState())
                    .weight(1f)
                    .padding(horizontal = ProtonDimens.DefaultSpacing),
                verticalArrangement = Arrangement.Center,
            ) {
                PricingComparisonTable(rows = viewState.comparisonRows)
            }
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(all = ProtonDimens.DefaultSpacing),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Offer(
                    period = viewState.period,
                    totalPrice = viewState.totalPrice,
                    monthlyPrice = viewState.monthlyPrice,
                    monthlyPricePeriod = viewState.monthlyPricePeriod,
                )
                if (viewState.productId != null && viewState.offerToken != null) {
                    GetDealButton(
                        title = stringResource(viewState.getDealButtonResId),
                        productId = viewState.productId,
                        offerToken = viewState.offerToken,
                        isPaymentLoading = viewState.isPaymentLoading,
                        onPurchaseClicked = viewEvent.onPurchaseClicked,
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 48.dp)
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
                .align(Alignment.TopEnd),
            closeAction = viewState.closeAction,
        )
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

@Composable
private fun Offer(
    period: String,
    totalPrice: String,
    monthlyPrice: String,
    monthlyPricePeriod: String,
    modifier: Modifier = Modifier,
) {
    val minHeight = 68.dp
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
                    painter = painterResource(BasePresentation.drawable.img_q3_campaign_discount_small),
                    contentDescription = null,
                    modifier = Modifier.padding(horizontal = ProtonDimens.SmallSpacing),
                )
            }
            Column(
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.End,
            ) {
                Text(
                    text = totalPrice,
                    style = ProtonTheme.typography.body1Medium.copy(color = color, fontWeight = FontWeight.Bold),
                )
                Text(
                    text = stringResource(I18N.string.promo_offer_per_month_details, "$monthlyPrice$monthlyPricePeriod"),
                    style = ProtonTheme.typography.body2Medium.copy(color = color.copy(alpha = 0.7f)),
                )
            }
        }
    }
}

@Composable
fun GetDealButton(
    title: String,
    productId: String,
    offerToken: String,
    isPaymentLoading: Boolean,
    modifier: Modifier = Modifier,
    onPurchaseClicked: (productId: String, offerToken: String) -> Unit,
) {
    if (isPaymentLoading) {
        Box(modifier = modifier, contentAlignment = Alignment.Center) {
            DeferredCircularProgressIndicator()
        }
    } else {
        ProtonButton(
            onClick = { onPurchaseClicked(productId, offerToken) },
            elevation = null,
            shape = ProtonTheme.shapes.large,
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

@Composable
private fun PricingComparisonTable(
    rows: List<Q3CampaignPromoViewState.ComparisonRow>,
    modifier: Modifier = Modifier,
) {
    // Box layering: the highlighted "Unlimited" column background, pinned to the end, full height
    Box(modifier = modifier.fillMaxWidth()) {
        Box(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .fillMaxHeight()
                .width(ComparisonUnlimitedColumnWidth)
                .clip(RoundedCornerShape(20.dp))
                .background(comparisonHighlightBackground)
        )

        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Spacer(modifier = Modifier.weight(1f))
                Box(
                    modifier = Modifier.width(ComparisonFreeColumnWidth),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = stringResource(I18N.string.promo_q3_campaign_comparison_header_free),
                        color = Color.White,
                        fontWeight = FontWeight.Medium,
                        fontSize = 14.sp,
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Box(
                    modifier = Modifier.width(ComparisonUnlimitedColumnWidth),
                    contentAlignment = Alignment.Center,
                ) {
                    Box(
                        modifier = Modifier
                            .border(width = 1.dp, color = comparisonAccentPink, shape = RoundedCornerShape(50))
                            .padding(horizontal = 16.dp, vertical = 6.dp),
                    ) {
                        Text(
                            text = stringResource(I18N.string.promo_q3_campaign_comparison_header_unlimited),
                            color = Color.White,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 13.sp,
                        )
                    }
                }
            }

            rows.forEachIndexed { index, row ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = ComparisonRowVerticalPadding),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = row.label,
                        color = Color.White,
                        fontSize = 15.sp,
                        lineHeight = 19.sp,
                        modifier = Modifier.weight(1f),
                    )
                    Box(
                        modifier = Modifier.width(ComparisonFreeColumnWidth),
                        contentAlignment = Alignment.Center,
                    ) {
                        ComparisonCellContent(value = row.free, isHighlighted = false)
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Box(
                        modifier = Modifier
                            .width(ComparisonUnlimitedColumnWidth)
                            .padding(horizontal = 8.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        ComparisonCellContent(value = row.unlimited, isHighlighted = true)
                    }
                }

                if (index != rows.lastIndex) {
                    Divider(
                        modifier = Modifier.fillMaxWidth(),
                        color = comparisonDividerColor,
                        thickness = 1.dp,
                    )
                }
            }
        }
    }
}

@Composable
private fun ComparisonCellContent(value: Q3CampaignPromoViewState.CellValue, isHighlighted: Boolean) {
    when (value) {
        is Q3CampaignPromoViewState.CellValue.Check -> {
            if (!value.included) {
                Text(text = "-", color = comparisonMutedText, fontSize = 16.sp)
            } else if (isHighlighted) {
                Box(
                    modifier = Modifier
                        .size(22.dp)
                        .clip(RoundedCornerShape(50))
                        .background(Color.White),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        painter = painterResource(CorePresentation.drawable.ic_proton_checkmark),
                        contentDescription = null,
                        tint = Color.Black,
                        modifier = Modifier.size(14.dp),
                    )
                }
            }
        }
        is Q3CampaignPromoViewState.CellValue.Value -> {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = value.text,
                    color = if (isHighlighted) comparisonAccentPink else Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                )
                value.subtitle?.let {
                    Text(text = it, color = comparisonMutedText, fontSize = 11.sp, textAlign = TextAlign.Center)
                }
            }
        }
    }
}

private val ComparisonFreeColumnWidth = 64.dp
private val ComparisonUnlimitedColumnWidth = 132.dp
private val ComparisonRowVerticalPadding = 20.dp
private val comparisonHighlightBackground = Color(0xFF1C1420)
private val comparisonAccentPink = Color(0xFFFF4D8D)
private val comparisonMutedText = Color(0xFF6B6B6B)
private val comparisonDividerColor = Color(0xFF262626)

private val driveCustomBlue = Color(0xFF372580)

@Composable
private fun previewViewState() = Q3CampaignPromoViewState(
    backgroundResId = BasePresentation.drawable.bg_q3_campaign_promo_port_dark,
    closeAction = Action.Image(
        imageResId = BasePresentation.drawable.drive_q3_campaign_close_button,
        contentDescriptionResId = I18N.string.common_close_action,
        onAction = {},
    ),
    comparisonRows = listOf(
        Q3CampaignPromoViewState.ComparisonRow(
            label = stringResource(I18N.string.promo_q3_campaign_comparison_storage_label),
            free = Q3CampaignPromoViewState.CellValue.Value(
                stringResource(I18N.string.promo_q3_campaign_comparison_storage_free),
            ),
            unlimited = Q3CampaignPromoViewState.CellValue.Value(
                text = stringResource(I18N.string.promo_q3_campaign_comparison_storage_unlimited),
                subtitle = stringResource(I18N.string.promo_q3_campaign_comparison_storage_unlimited_subtitle),
            ),
        ),
        Q3CampaignPromoViewState.ComparisonRow(
            label = stringResource(I18N.string.promo_q3_campaign_comparison_share_edit_label),
            free = Q3CampaignPromoViewState.CellValue.Check(included = false),
            unlimited = Q3CampaignPromoViewState.CellValue.Check(included = true),
        ),
        Q3CampaignPromoViewState.ComparisonRow(
            label = stringResource(I18N.string.promo_q3_campaign_comparison_mail_label),
            free = Q3CampaignPromoViewState.CellValue.Check(included = false),
            unlimited = Q3CampaignPromoViewState.CellValue.Check(included = true),
        ),
        Q3CampaignPromoViewState.ComparisonRow(
            label = stringResource(I18N.string.promo_q3_campaign_comparison_vpn_label),
            free = Q3CampaignPromoViewState.CellValue.Check(included = false),
            unlimited = Q3CampaignPromoViewState.CellValue.Check(included = true),
        ),
        Q3CampaignPromoViewState.ComparisonRow(
            label = stringResource(I18N.string.promo_q3_campaign_comparison_pass_label),
            free = Q3CampaignPromoViewState.CellValue.Check(included = false),
            unlimited = Q3CampaignPromoViewState.CellValue.Check(included = true),
        ),
    ),
    getDealButtonResId = I18N.string.promo_claim_offer_button,
    totalPrice = stringResource(I18N.string.promo_q3_campaign_total_price_preview),
    monthlyPrice = stringResource(I18N.string.promo_q3_campaign_monthly_price_preview),
    period = pluralStringResource(I18N.plurals.common_x_months, 12, 12).lowercase(),
    monthlyPricePeriod = "/${stringResource(I18N.string.common_month).lowercase()}",
    autoRenewPrice = stringResource(
        I18N.string.plan_welcome_offer_auto_renews_annual,
        stringResource(I18N.string.promo_q3_campaign_renewal_price_preview),
    ),
)

private val previewViewEvent = object : Q3CampaignPromoViewEvent {}

@Preview(name = "Portrait", showBackground = true)
@Composable
private fun Q3CampaignPromoScreenPortraitPreview() {
    ProtonTheme {
        Q3CampaignPromoScreenPortrait(
            viewState = previewViewState(),
            viewEvent = previewViewEvent,
        )
    }
}
