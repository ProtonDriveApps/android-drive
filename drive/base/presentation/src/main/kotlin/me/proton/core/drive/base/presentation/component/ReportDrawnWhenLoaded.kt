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

package me.proton.core.drive.base.presentation.component

import androidx.activity.FullyDrawnReporter
import androidx.activity.compose.LocalFullyDrawnReporterOwner
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.RememberObserver
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf

class ContentDrawnGate(private val fullyDrawnReporter: FullyDrawnReporter) : RememberObserver {

    private var isHeld = false
    private var isReleased = false

    fun release() {
        if (isHeld && !isReleased) {
            isReleased = true
            fullyDrawnReporter.removeReporter()
        }
    }

    override fun onRemembered() {
        if (!isHeld && !fullyDrawnReporter.isFullyDrawnReported) {
            isHeld = true
            fullyDrawnReporter.addReporter()
        }
    }

    override fun onForgotten() = Unit

    override fun onAbandoned() = Unit
}

val LocalContentDrawnGate = staticCompositionLocalOf<ContentDrawnGate?> { null }

@Composable
fun ProvideContentDrawnGate(content: @Composable () -> Unit) {
    val fullyDrawnReporter = LocalFullyDrawnReporterOwner.current?.fullyDrawnReporter
    val gate = remember(fullyDrawnReporter) { fullyDrawnReporter?.let(::ContentDrawnGate) }
    CompositionLocalProvider(LocalContentDrawnGate provides gate, content = content)
}

@Composable
fun ReportDrawnWhenLoaded(isLoading: Boolean) {
    val gate = LocalContentDrawnGate.current ?: return
    SideEffect {
        if (!isLoading) gate.release()
    }
}
