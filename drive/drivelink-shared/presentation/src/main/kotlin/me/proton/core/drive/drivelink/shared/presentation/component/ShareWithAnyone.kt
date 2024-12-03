/*
 * Copyright (c) 2024 Proton AG. 
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

package me.proton.core.drive.drivelink.shared.presentation.component

import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.Icon
import androidx.compose.material.Surface
import androidx.compose.material.Switch
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import me.proton.core.compose.component.ProtonErrorMessage
import me.proton.core.compose.component.ProtonErrorMessageWithAction
import me.proton.core.compose.component.ProtonSolidButton
import me.proton.core.compose.component.protonButtonColors
import me.proton.core.compose.theme.ProtonDimens
import me.proton.core.compose.theme.ProtonDimens.DefaultSpacing
import me.proton.core.compose.theme.ProtonDimens.ExtraSmallSpacing
import me.proton.core.compose.theme.ProtonDimens.MediumSpacing
import me.proton.core.compose.theme.ProtonDimens.SmallIconSize
import me.proton.core.compose.theme.ProtonDimens.SmallSpacing
import me.proton.core.compose.theme.ProtonTheme
import me.proton.core.compose.theme.default
import me.proton.core.compose.theme.defaultNorm
import me.proton.core.compose.theme.defaultSmallNorm
import me.proton.core.compose.theme.defaultStrongNorm
import me.proton.core.drive.base.presentation.component.Deferred
import me.proton.core.drive.drivelink.shared.presentation.viewstate.LoadingViewState
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds
import me.proton.core.drive.i18n.R as I18N
import me.proton.core.presentation.R as CorePresentation

@Composable
fun ShareWithAnyone(
    viewState: LoadingViewState,
    publicUrl: String?,
    accessibilityDescription: String,
    permissionsDescription: String,
    onRetry: () -> Unit,
    onStartSharing: () -> Unit,
    onStopSharing: () -> Unit,
    onCopyLink: (String) -> Unit,
    onConfigureSharing: () -> Unit,
    modifier: Modifier = Modifier,
    onMore: (() -> Unit)? = null,
) {
    val isShared = publicUrl != null
    Column(modifier) {
        ShareWithAnyoneTitle(
            modifier = Modifier.padding(horizontal = DefaultSpacing),
            isShared = isShared,
            isLoading = viewState is LoadingViewState.Loading,
            onStartSharing = onStartSharing,
            onStopSharing = onStopSharing,
        )
        ShareWithAnyoneSwitch(
            modifier = Modifier.padding(horizontal = DefaultSpacing),
            accessibilityDescription = accessibilityDescription,
            permissionDescription = permissionsDescription,
            isShared = isShared,
            onMore = onMore,
        )
        ShareWithAnyoneContent(
            modifier = Modifier.padding(horizontal = DefaultSpacing),
            viewState = viewState,
            publicUrl = publicUrl,
            onRetry = onRetry,
            onCopyLink = onCopyLink,
            onConfigureSharing = onConfigureSharing,
        )
    }
}

@Composable
private fun ShareWithAnyoneTitle(
    isShared: Boolean,
    isLoading: Boolean,
    onStartSharing: () -> Unit,
    onStopSharing: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(modifier = modifier
        .toggleable(
            value = isShared,
            enabled = !isLoading
        ) { checked ->
            if (checked) {
                onStartSharing()
            } else {
                onStopSharing()
            }
        }
        .padding(
            top = MediumSpacing,
            bottom = SmallSpacing
        ),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            modifier = Modifier.weight(1F),
            text = stringResource(I18N.string.manage_access_share_with_anyone),
            style = ProtonTheme.typography.defaultStrongNorm,
        )

        Switch(
            enabled = !isLoading,
            checked = isShared,
            onCheckedChange = null,
        )
    }
}

@Composable
private fun ShareWithAnyoneSwitch(
    accessibilityDescription: String,
    permissionDescription: String,
    isShared: Boolean,
    modifier: Modifier = Modifier,
    onMore: (() -> Unit)? = null,
) {
    Row(
        modifier = modifier
            .padding(vertical = SmallSpacing)
            .clickable(enabled = isShared) {
                onMore?.invoke()
            },
        horizontalArrangement = Arrangement.spacedBy(ProtonDimens.ListItemTextStartPadding),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        ShareWithAnyoneIcon(isShared = isShared)
        Column(Modifier.weight(1F)) {
            Text(
                text = accessibilityDescription,
                maxLines = 1,
                style = ProtonTheme.typography.defaultNorm,
            )
            Text(
                text = permissionDescription,
                maxLines = 1,
                style = ProtonTheme.typography.defaultSmallNorm,
            )
        }
        if (onMore != null) {
            Icon(
                painter = painterResource(id = CorePresentation.drawable.ic_proton_chevron_down_filled),
                contentDescription = stringResource(id = I18N.string.common_more),
                tint = if (isShared) {
                    ProtonTheme.colors.iconNorm
                } else {
                    ProtonTheme.colors.iconDisabled
                },
            )
        }
    }
}

@Composable
private fun ShareWithAnyoneIcon(
    isShared: Boolean,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .size(LargeIconSize)
            .background(
                if (isShared)
                    ProtonTheme.colors.notificationSuccess.copy(alpha = 0.16F)
                else
                    ProtonTheme.colors.backgroundSecondary,
                RoundedCornerShape(ProtonDimens.LargeCornerRadius)
            ),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            modifier = Modifier.size(SmallIconSize),
            painter = painterResource(id = CorePresentation.drawable.ic_proton_globe),
            tint = if (isShared) {
                ProtonTheme.colors.notificationSuccess
            } else {
                ProtonTheme.colors.iconWeak
            },
            contentDescription = null
        )
    }
}

@Composable
private fun ShareWithAnyoneContent(
    viewState: LoadingViewState,
    publicUrl: String?,
    onRetry: () -> Unit,
    onCopyLink: (String) -> Unit,
    onConfigureSharing: () -> Unit,
    modifier: Modifier = Modifier,
) {
    AnimatedContent(
        modifier = modifier,
        targetState = viewState to publicUrl,
        label = "share-with-anyone",
    ) { (viewState, publicUrl) ->
        Column(modifier = Modifier.fillMaxWidth()) {
            when (viewState) {
                LoadingViewState.Initial -> Unit
                is LoadingViewState.Loading -> ShareWithAnyoneLoading(
                    message = viewState.message,
                    modifier = Modifier.align(Alignment.CenterHorizontally),
                )

                is LoadingViewState.Error.Retryable -> ShareWithAnyoneErrorWithRetry(
                    message = viewState.message,
                    modifier = Modifier.align(Alignment.CenterHorizontally),
                    retry = onRetry,
                )

                is LoadingViewState.Error.NonRetryable -> ShareWithAnyoneError(
                    message = viewState.message,
                    deferDuration = viewState.defer,
                    modifier = Modifier.align(Alignment.CenterHorizontally),
                )

                is LoadingViewState.Available -> Unit
            }
            if (publicUrl != null) {
                ProtonSolidButton(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = {
                        onCopyLink(publicUrl)
                    },
                    colors = ButtonDefaults.protonWeakButtonColors(),
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(ExtraSmallSpacing)
                    ) {
                        Icon(
                            modifier = Modifier.size(SmallIconSize),
                            painter = painterResource(id = CorePresentation.drawable.ic_proton_link),
                            contentDescription = null
                        )
                        Text(text = stringResource(id = I18N.string.common_copy_link_action))
                    }
                }
                ProtonSolidButton(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = { onConfigureSharing() },
                    colors = ButtonDefaults.protonWeakButtonColors(),
                ) {
                    Text(text = stringResource(id = I18N.string.manage_access_link_settings_action))
                }
            }
        }
    }
}

@Composable
fun ButtonDefaults.protonWeakButtonColors(
    loading: Boolean = false,
) = protonButtonColors(
    backgroundColor = ProtonTheme.colors.interactionWeakNorm,
    contentColor = ProtonTheme.colors.textNorm,
    disabledBackgroundColor = if (loading) {
        ProtonTheme.colors.interactionWeakPressed
    } else {
        ProtonTheme.colors.interactionWeakDisabled
    },
    disabledContentColor = if (loading) {
        ProtonTheme.colors.textNorm
    } else {
        ProtonTheme.colors.textDisabled
    },
)

@Composable
fun ShareWithAnyoneLoading(
    message: String,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        CircularProgressIndicator()
        Text(
            text = message,
            style = ProtonTheme.typography.default,
            modifier = Modifier.padding(top = ProtonDimens.MediumSpacing)
        )
    }
}

@Composable
fun ShareWithAnyoneErrorWithRetry(
    message: String,
    retry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .padding(all = DefaultSpacing),
        contentAlignment = Alignment.BottomCenter,
    ) {
        ProtonErrorMessageWithAction(
            errorMessage = message,
            action = stringResource(I18N.string.common_retry_action),
            onAction = retry
        )
    }
}

@Composable
fun ShareWithAnyoneError(
    message: String,
    deferDuration: Duration,
    modifier: Modifier = Modifier,
) {
    Deferred(
        duration = deferDuration,
    ) {
        Box(
            modifier = modifier
                .padding(all = DefaultSpacing),
            contentAlignment = Alignment.BottomCenter,
        ) {
            ProtonErrorMessage(
                errorMessage = message,
            )
        }
    }
}

private val LargeIconSize = 32.dp

@Preview
@Composable
fun ShareWithAnyoneNoLinkPreview() {
    ProtonTheme {
        Surface {
            ShareWithAnyone(
                viewState = LoadingViewState.Initial,
                publicUrl = null,
                accessibilityDescription = stringResource(id = I18N.string.manage_access_link_description_public),
                permissionsDescription = stringResource(id = I18N.string.manage_access_link_viewer_permission),
                onRetry = { },
                onStartSharing = { },
                onStopSharing = { },
                onCopyLink = { },
                onConfigureSharing = { },
                onMore = { },
            )
        }
    }
}

@Preview
@Composable
fun ShareWithAnyoneLoadingPreview() {
    ProtonTheme {
        Surface {
            ShareWithAnyone(
                viewState = LoadingViewState.Loading("Creating link to file"),
                publicUrl = null,
                accessibilityDescription = stringResource(id = I18N.string.manage_access_link_description_public),
                permissionsDescription = stringResource(id = I18N.string.manage_access_link_viewer_permission),
                onRetry = { },
                onStartSharing = { },
                onStopSharing = { },
                onCopyLink = { },
                onConfigureSharing = { },
                onMore = null,
            )
        }
    }
}

@Preview
@Composable
private fun ShareWithAnyoneErrorRetryablePreview() {
    ProtonTheme {
        Surface {
            ShareWithAnyone(
                viewState = LoadingViewState.Error.Retryable("Retryable error"),
                publicUrl = null,
                accessibilityDescription = stringResource(id = I18N.string.manage_access_link_description_public),
                permissionsDescription = stringResource(id = I18N.string.manage_access_link_viewer_permission),
                onRetry = { },
                onStartSharing = { },
                onStopSharing = { },
                onCopyLink = { },
                onConfigureSharing = { },
                onMore = null,
            )
        }
    }
}

@Preview
@Composable
private fun ShareWithAnyoneErrorNonRetryablePreview() {
    ProtonTheme {
        Surface {
            ShareWithAnyone(
                viewState = LoadingViewState.Error.NonRetryable("Non retryable error", 0.seconds),
                publicUrl = null,
                accessibilityDescription = stringResource(id = I18N.string.manage_access_link_description_public),
                permissionsDescription = stringResource(id = I18N.string.manage_access_link_viewer_permission),
                onRetry = { },
                onStartSharing = { },
                onStopSharing = { },
                onCopyLink = { },
                onConfigureSharing = { },
                onMore = null,
            )
        }
    }
}

@Preview
@Composable
fun ShareWithAnyoneEditorPreview() {
    ProtonTheme {
        Surface {
            ShareWithAnyone(
                viewState = LoadingViewState.Initial,
                publicUrl = "url",
                accessibilityDescription = stringResource(id = I18N.string.manage_access_link_description_public),
                permissionsDescription = stringResource(id = I18N.string.manage_access_link_editor_permission),
                onRetry = { },
                onStartSharing = { },
                onStopSharing = { },
                onCopyLink = { },
                onConfigureSharing = { },
                onMore = { },
            )
        }
    }
}

@Preview
@Composable
fun ShareWithAnyonePasswordLinkPreview() {
    ProtonTheme {
        Surface {
            ShareWithAnyone(
                viewState = LoadingViewState.Initial,
                publicUrl = "url",
                accessibilityDescription = stringResource(
                    id = I18N.string.manage_access_link_description_password_protected
                ),
                permissionsDescription = stringResource(id = I18N.string.manage_access_link_viewer_permission),
                onRetry = { },
                onStartSharing = { },
                onStopSharing = { },
                onCopyLink = { },
                onConfigureSharing = { },
                onMore = { },
            )
        }
    }
}
