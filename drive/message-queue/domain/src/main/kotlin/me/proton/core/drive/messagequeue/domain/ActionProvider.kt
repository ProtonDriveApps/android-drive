/*
 * Copyright (c) 2021-2023 Proton AG.
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

package me.proton.core.drive.messagequeue.domain

import android.content.Context
import androidx.annotation.StringRes
import java.io.Serializable

interface ActionProvider {

    fun provideAction(extra: Serializable?): Action?

    data class Action private constructor(
        @StringRes private val labelResId: Int,
        private val label: CharSequence?,
        private val onAction: suspend () -> Unit,
    ) {

        constructor(label: CharSequence, onAction: suspend () -> Unit) : this(
            labelResId = 0,
            label = label,
            onAction = onAction
        )


        constructor(@StringRes label: Int, onAction: suspend () -> Unit) : this(
            labelResId = label,
            label = null,
            onAction = onAction
        )

        suspend operator fun invoke() = onAction()

        fun getLabel(context: Context): CharSequence = label ?: context.getString(labelResId)
    }
}
