/*
 * Copyright (c) 2026 Proton AG.
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
package me.proton.core.drive.base.presentation.component

import androidx.annotation.StringRes
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import me.proton.core.compose.theme.ProtonTheme
import me.proton.core.compose.theme.defaultWeak

@Composable
fun ProtonAlertDialog(
    onDismissRequest: () -> Unit,
    confirmButton: @Composable () -> Unit,
    @StringRes titleResId: Int,
    modifier: Modifier = Modifier,
    text: @Composable (() -> Unit)? = null,
    dismissButton: @Composable (() -> Unit)? = null,
    shape: Shape = ProtonTheme.shapes.medium,
    backgroundColor: Color = ProtonTheme.colors.backgroundNorm,
    properties: DialogProperties = DialogProperties(),
) {
    ProtonAlertDialog(
        onDismissRequest = onDismissRequest,
        confirmButton = confirmButton,
        modifier = modifier,
        dismissButton = dismissButton,
        title = stringResource(id = titleResId),
        text = text,
        shape = shape,
        backgroundColor = backgroundColor,
        properties = properties,
    )
}

@Composable
fun ProtonAlertDialog(
    onDismissRequest: () -> Unit,
    confirmButton: @Composable () -> Unit,
    title: String,
    modifier: Modifier = Modifier,
    dismissButton: @Composable (() -> Unit)? = null,
    text: @Composable (() -> Unit)? = null,
    shape: Shape = ProtonTheme.shapes.medium,
    backgroundColor: Color = ProtonTheme.colors.backgroundNorm,
    properties: DialogProperties = DialogProperties(),
) {
    ProtonAlertDialog(
        onDismissRequest = onDismissRequest,
        confirmButton = confirmButton,
        modifier = modifier,
        dismissButton = dismissButton,
        title = { ProtonDialogTitle(title = title) },
        text = text,
        shape = shape,
        backgroundColor = backgroundColor,
        properties = properties,
    )
}

@Composable
fun ProtonAlertDialog(
    onDismissRequest: () -> Unit,
    confirmButton: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    dismissButton: @Composable (() -> Unit)? = null,
    title: @Composable (() -> Unit)? = null,
    text: @Composable (() -> Unit)? = null,
    shape: Shape = ProtonTheme.shapes.medium,
    backgroundColor: Color = ProtonTheme.colors.backgroundNorm,
    properties: DialogProperties = DialogProperties(),
) {
    AlertDialog(
        onDismissRequest = onDismissRequest,
        confirmButton = confirmButton,
        modifier = modifier.fixAlertDialogSize(),
        dismissButton = dismissButton,
        title = title,
        text = text,
        shape = shape,
        containerColor = backgroundColor,
        properties = DialogProperties(
            dismissOnBackPress = properties.dismissOnBackPress,
            dismissOnClickOutside = properties.dismissOnClickOutside,
            securePolicy = properties.securePolicy,
            usePlatformDefaultWidth = false,
        ),
    )
}

@Composable
fun ProtonAlertDialog(
    onDismissRequest: () -> Unit,
    buttons: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    title: (@Composable () -> Unit)? = null,
    text: @Composable (() -> Unit)? = null,
    shape: Shape = ProtonTheme.shapes.medium,
    backgroundColor: Color = ProtonTheme.colors.backgroundNorm,
    properties: DialogProperties = DialogProperties(),
) {
    AlertDialog(
        onDismissRequest = onDismissRequest,
        confirmButton = buttons,
        modifier = modifier.fixAlertDialogSize(),
        title = title,
        text = text,
        shape = shape,
        containerColor = backgroundColor,
        properties = DialogProperties(
            dismissOnBackPress = properties.dismissOnBackPress,
            dismissOnClickOutside = properties.dismissOnClickOutside,
            securePolicy = properties.securePolicy,
            usePlatformDefaultWidth = false,
        ),
    )
}

@Composable
fun ProtonDialogTitle(
    @StringRes titleResId: Int,
    modifier: Modifier = Modifier,
) {
    ProtonDialogTitle(
        title = stringResource(id = titleResId),
        modifier = modifier,
    )
}

@Composable
fun ProtonDialogTitle(
    title: String,
    modifier: Modifier = Modifier,
) {
    Text(
        text = title,
        style = ProtonTheme.typography.headline,
        color = ProtonTheme.colors.textNorm,
        maxLines = 2,
        overflow = TextOverflow.Ellipsis,
        modifier = modifier,
    )
}

@Composable
fun ProtonAlertDialogText(
    @StringRes textResId: Int,
    modifier: Modifier = Modifier,
) {
    ProtonAlertDialogText(
        text = stringResource(id = textResId),
        modifier = modifier,
    )
}

@Composable
fun ProtonAlertDialogText(
    text: String,
    modifier: Modifier = Modifier,
) {
    Text(
        text = text,
        style = ProtonTheme.typography.defaultWeak,
        modifier = modifier,
    )
}

@Composable
fun ProtonAlertDialogButton(
    @StringRes titleResId: Int,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    loading: Boolean = false,
    contained: Boolean = true,
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
    onClick: () -> Unit,
) {
    ProtonAlertDialogButton(
        title = stringResource(id = titleResId),
        onClick = onClick,
        modifier = modifier,
        enabled = enabled,
        loading = loading,
        contained = contained,
        interactionSource = interactionSource,
    )
}

@Composable
fun ProtonAlertDialogButton(
    title: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    loading: Boolean = false,
    contained: Boolean = true,
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
    onClick: () -> Unit,
) {
    ProtonTextButton(
        onClick = onClick,
        modifier = modifier,
        enabled = enabled,
        loading = loading,
        contained = contained,
        interactionSource = interactionSource,
    ) {
        Text(
            text = title,
            style = ProtonTheme.typography.body1Medium,
            color = ProtonTheme.colors.textAccent,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

private fun Modifier.fixAlertDialogSize() = fillMaxWidth(fraction = AlertDialogWidthFraction)
    .widthIn(max = MaxAlertDialogWidth)

private const val AlertDialogWidthFraction = 0.9f

// Mobile alert on desktop is 560dp wide
// https://material.io/components/dialogs#specs
private val MaxAlertDialogWidth = 560.dp
