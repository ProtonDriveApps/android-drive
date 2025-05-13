/*
 * Copyright (c) 2023-2024 Proton AG.
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
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import me.proton.android.drive.R
import me.proton.android.drive.ui.component.PromoContainer
import me.proton.android.drive.ui.viewmodel.SubscriptionPromoViewModel
import me.proton.android.drive.ui.viewstate.SubscriptionPromoViewState
import me.proton.core.compose.theme.ProtonTheme
import me.proton.core.drive.base.presentation.component.RunAction
import me.proton.core.drive.i18n.R as I18N

@Composable
fun SubscriptionPromoScreen(
    modifier: Modifier = Modifier,
    runAction: RunAction,
    navigateToSubscription: () -> Unit,
) {
    val viewModel = hiltViewModel<SubscriptionPromoViewModel>()
    val viewState by viewModel.viewState.collectAsStateWithLifecycle(null)
    val viewEvent = remember {
        viewModel.viewEvent(
            runAction = runAction,
            navigateToSubscription = navigateToSubscription,
        )
    }
    DisposableEffect(Unit) {
        onDispose {
            viewEvent.onDismiss()
        }
    }
    viewState?.let { viewState ->
        SubscriptionPromo(
            modifier = modifier.testTag(SubscriptionPromoTestTag.content),
            viewState = viewState,
            onGetDriveLite = { viewEvent.onGetSubscription() },
            onCancel = { viewEvent.onCancel() },
        )
    }
}

@Composable
private fun SubscriptionPromo(
    modifier: Modifier,
    viewState: SubscriptionPromoViewState,
    onGetDriveLite: () -> Unit,
    onCancel: () -> Unit
) {
    PromoContainer(
        modifier = modifier.systemBarsPadding(),
        title = viewState.title,
        description = viewState.description,
        image = {
            Image(
                modifier = Modifier.defaultMinSize(minWidth = 320.dp),
                painter = painterResource(id = viewState.image),
                contentDescription = null,
                contentScale = ContentScale.FillWidth
            )
        },
        actionText = viewState.actionText,
        dismissActionText = stringResource(id = I18N.string.photos_upsell_dismiss_action),
        onAction = { onGetDriveLite() },
        onCancel = { onCancel() },
    )
}

@Preview
@Preview(widthDp = 600, heightDp = 360)
@Composable
private fun PhotosUpsellPreview() {
    ProtonTheme {
        SubscriptionPromo(
            modifier = Modifier.fillMaxSize(),
            viewState = SubscriptionPromoViewState(
                image = R.drawable.img_drive_lite,
                title ="More storage for only \$1.00",
                description ="When you need a little more storage, but not a lot. Introducing Drive Lite, featuring 20 GB storage for only \$1.00 a month. ",
                actionText ="Get Drive Lite",
            ),
            onGetDriveLite = {},
            onCancel = {},
        )
    }
}

object SubscriptionPromoTestTag {
    const val content = "subscription promo content"
}
