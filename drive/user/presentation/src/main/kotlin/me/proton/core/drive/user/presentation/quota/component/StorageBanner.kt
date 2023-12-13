/*
 * Copyright (c) 2023 Proton AG.
 * This file is part of Proton Core.
 *
 * Proton Core is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Proton Core is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Proton Core.  If not, see <https://www.gnu.org/licenses/>.
 */

package me.proton.core.drive.user.presentation.quota.component

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Card
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import me.proton.core.compose.component.ProtonSolidButton
import me.proton.core.compose.flow.rememberFlowWithLifecycle
import me.proton.core.compose.theme.ProtonDimens
import me.proton.core.compose.theme.ProtonTheme
import me.proton.core.compose.theme.defaultNorm
import me.proton.core.drive.user.domain.entity.QuotaLevel
import me.proton.core.drive.user.presentation.quota.extension.toState
import me.proton.core.drive.user.presentation.quota.viewevent.StorageQuotasViewEvent
import me.proton.core.drive.user.presentation.quota.viewmodel.StorageQuotasViewModel
import me.proton.core.drive.user.presentation.quota.viewstate.QuotaViewState
import me.proton.core.drive.i18n.R as I18N
import me.proton.core.presentation.R as CorePresentation

@Composable
fun StorageBanner(
    onGetStorage: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val viewModel = hiltViewModel<StorageQuotasViewModel>()
    val viewState by rememberFlowWithLifecycle(flow = viewModel.viewState)
        .collectAsState(initial = null)
    val viewEvent = remember {
        viewModel.viewEvent(
            getStorage = onGetStorage,
        )
    }
    AnimatedVisibility(
        modifier = modifier,
        visible = viewState != null,
        enter = EnterTransition.None,
        exit = shrinkVertically() + fadeOut(),
    ) {
        viewState?.let { state ->
            StorageBanner(
                state,
                viewEvent,
            )
        }
    }
}

@Composable
fun StorageBanner(
    viewState: QuotaViewState,
    viewEvent: StorageQuotasViewEvent,
    modifier: Modifier = Modifier,
) {
    StorageBanner(
        modifier = modifier,
        iconResId = viewState.iconResId,
        iconTint = when (viewState.level) {
            QuotaViewState.Level.ERROR -> ProtonTheme.colors.notificationError
            QuotaViewState.Level.WARNING -> ProtonTheme.colors.notificationWarning
            QuotaViewState.Level.INFO -> ProtonTheme.colors.iconWeak
        },
        title = viewState.title,
        canDismiss = viewState.canDismiss,
        actionLabel = viewState.actionLabel,
        getStorage = viewEvent.onGetStorage,
        onDismiss = { viewEvent.onCancel(viewState.level) },
    )
}

@Composable
fun StorageBanner(
    iconResId: Int,
    iconTint: Color,
    title: AnnotatedString,
    canDismiss: Boolean,
    getStorage: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    actionLabel: String,
) {
    Card(
        elevation = 0.dp
    ) {
        Column(modifier = modifier.padding(bottom = ProtonDimens.SmallSpacing)) {
            Row(
                Modifier.padding(start = containerPadding),
                Arrangement.spacedBy(ProtonDimens.SmallSpacing),
            ) {
                Icon(
                    modifier = Modifier.padding(top = containerPadding),
                    painter = painterResource(id = iconResId),
                    contentDescription = null,
                    tint = iconTint
                )
                Text(
                    modifier = Modifier
                        .weight(1F).let {
                            if (canDismiss) {
                                it.padding(
                                    top = containerPadding,
                                    bottom = ProtonDimens.SmallSpacing,
                                )
                            } else {
                                it.padding(
                                    top = containerPadding,
                                    bottom = ProtonDimens.SmallSpacing,
                                    end = containerPadding,
                                )
                            }
                        },
                    text = title,
                    style = ProtonTheme.typography.defaultNorm
                )
                if (canDismiss) {
                    IconButton(onClick = { onDismiss() }) {
                        Icon(
                            painter = painterResource(id = CorePresentation.drawable.ic_proton_cross),
                            contentDescription = stringResource(id = I18N.string.common_close_action)
                        )
                    }
                }
            }
            ProtonSolidButton(
                modifier = Modifier
                    .align(Alignment.End)
                    .padding(horizontal = containerPadding),
                onClick = getStorage
            ) {
                Text(
                    text = actionLabel,
                    modifier = Modifier.padding(horizontal = ProtonDimens.DefaultSpacing)
                )
            }
        }
    }
}

private val containerPadding = ProtonDimens.ListItemTextStartPadding

@Preview
@Composable
fun InfoStorageBannerPreview() {
    ProtonTheme {
        StorageBanner(
            viewState = QuotaLevel.INFO.toState(LocalContext.current)!!,
            viewEvent = object : StorageQuotasViewEvent {},
        )
    }
}

@Preview
@Composable
fun WarningStorageBannerPreview() {
    ProtonTheme {
        StorageBanner(
            viewState = QuotaLevel.WARNING.toState(LocalContext.current)!!,
            viewEvent = object : StorageQuotasViewEvent {},
        )
    }
}

@Preview
@Composable
fun ErrorStorageBannerPreview() {
    ProtonTheme {
        StorageBanner(
            viewState = QuotaLevel.ERROR.toState(LocalContext.current)!!,
            viewEvent = object : StorageQuotasViewEvent {},
        )
    }
}
