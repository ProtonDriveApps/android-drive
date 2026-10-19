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

package me.proton.core.drive.base.presentation.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color
import me.proton.core.compose.theme.ProtonColors
import me.proton.core.compose.theme.ProtonShapes
import me.proton.core.compose.theme.ProtonTheme
import me.proton.core.compose.theme.ProtonTypography
import me.proton.core.compose.theme.isNightMode

@Composable
fun DriveTheme(
    isDark: Boolean = isNightMode(),
    colors: ProtonColors = if (isDark) ProtonColors.Dark else ProtonColors.Light,
    typography: ProtonTypography = ProtonTypography.Default,
    shapes: ProtonShapes = ProtonShapes(),
    content: @Composable () -> Unit,
) {
    ProtonTheme(
        isDark = isDark,
        colors = colors,
        typography = typography,
        shapes = shapes,
    ) {
        val protonColors = ProtonTheme.colors
        MaterialTheme(
            colorScheme = protonColors.toColorScheme(),
            typography = typography.toTypography(),
            shapes = shapes.toShapes(),
        ) {
            CompositionLocalProvider(
                LocalContentColor provides protonColors.textNorm,
                content = content,
            )
        }
    }
}

private fun ProtonColors.toColorScheme(): ColorScheme {
    val outline = textNorm.copy(alpha = 0.38f)
    val outlineVariant = textNorm.copy(alpha = 0.12f)
    return if (isDark) {
        darkColorScheme(
            primary = brandNorm,
            onPrimary = Color.White,
            primaryContainer = backgroundSecondary,
            onPrimaryContainer = textNorm,
            inversePrimary = brandLighten20,
            secondary = brandNorm,
            onSecondary = Color.White,
            secondaryContainer = interactionWeakNorm,
            onSecondaryContainer = textNorm,
            tertiary = brandDarken20,
            onTertiary = Color.White,
            tertiaryContainer = backgroundSecondary,
            onTertiaryContainer = textNorm,
            background = backgroundNorm,
            onBackground = textNorm,
            surface = backgroundSecondary,
            onSurface = textNorm,
            surfaceVariant = backgroundSecondary,
            onSurfaceVariant = textWeak,
            surfaceTint = Color.Transparent,
            inverseSurface = textNorm,
            inverseOnSurface = textInverted,
            error = notificationError,
            onError = textInverted,
            errorContainer = backgroundSecondary,
            onErrorContainer = notificationError,
            outline = outline,
            outlineVariant = outlineVariant,
            scrim = blenderNorm,
            surfaceBright = backgroundSecondary,
            surfaceContainer = backgroundSecondary,
            surfaceContainerHigh = backgroundSecondary,
            surfaceContainerHighest = backgroundSecondary,
            surfaceContainerLow = backgroundSecondary,
            surfaceContainerLowest = backgroundNorm,
            surfaceDim = backgroundNorm,
        )
    } else {
        lightColorScheme(
            primary = brandNorm,
            onPrimary = Color.White,
            primaryContainer = backgroundSecondary,
            onPrimaryContainer = textNorm,
            inversePrimary = brandLighten20,
            secondary = brandNorm,
            onSecondary = Color.White,
            secondaryContainer = interactionWeakNorm,
            onSecondaryContainer = textNorm,
            tertiary = brandDarken20,
            onTertiary = Color.White,
            tertiaryContainer = backgroundSecondary,
            onTertiaryContainer = textNorm,
            background = backgroundNorm,
            onBackground = textNorm,
            surface = backgroundSecondary,
            onSurface = textNorm,
            surfaceVariant = backgroundSecondary,
            onSurfaceVariant = textWeak,
            surfaceTint = Color.Transparent,
            inverseSurface = textNorm,
            inverseOnSurface = textInverted,
            error = notificationError,
            onError = textInverted,
            errorContainer = backgroundSecondary,
            onErrorContainer = notificationError,
            outline = outline,
            outlineVariant = outlineVariant,
            scrim = blenderNorm,
            surfaceBright = backgroundSecondary,
            surfaceContainer = backgroundSecondary,
            surfaceContainerHigh = backgroundSecondary,
            surfaceContainerHighest = backgroundSecondary,
            surfaceContainerLow = backgroundSecondary,
            surfaceContainerLowest = backgroundNorm,
            surfaceDim = backgroundNorm,
        )
    }
}

private fun ProtonTypography.toTypography() = Typography(
    displayLarge = hero,
    displayMedium = hero,
    displaySmall = headline,
    headlineLarge = headline,
    headlineMedium = headline,
    headlineSmall = subheadline,
    titleLarge = headline,
    titleMedium = body1Medium,
    titleSmall = body2Medium,
    bodyLarge = body1Regular,
    bodyMedium = body2Regular,
    bodySmall = captionRegular,
    labelLarge = body1Regular,
    labelMedium = captionMedium,
    labelSmall = overlineMedium,
)

private fun ProtonShapes.toShapes() = Shapes(
    extraSmall = small,
    small = small,
    medium = medium,
    large = large,
    extraLarge = large,
)
