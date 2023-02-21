/*
 * Copyright (c) 2023 Proton AG.
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

package me.proton.android.drive.ui.provider

import android.annotation.SuppressLint
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

private val localSnackbarPaddingState = mutableStateOf(0.dp)
val LocalSnackbarPadding = staticCompositionLocalOf { localSnackbarPaddingState }

@Composable
fun ProvideLocalSnackbarPadding(content: @Composable () -> Unit) = CompositionLocalProvider(
    LocalSnackbarPadding provides localSnackbarPaddingState,
    content = content,
)

@SuppressLint("ComposableNaming")
@Composable
fun setLocalSnackbarPadding(value: Dp) {
    val localSnackbarPadding = LocalSnackbarPadding.current
    DisposableEffect(Unit) {
        localSnackbarPadding.value = value
        onDispose {
            localSnackbarPadding.value = 0.dp
        }
    }
}
