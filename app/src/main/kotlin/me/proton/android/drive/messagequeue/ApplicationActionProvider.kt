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

package me.proton.android.drive.messagequeue

import kotlinx.coroutines.ExperimentalCoroutinesApi
import me.proton.core.drive.messagequeue.domain.ActionProvider
import java.io.Serializable
import javax.inject.Inject

@ExperimentalCoroutinesApi
class ApplicationActionProvider @Inject constructor(
    private val actionProviders: Set<@JvmSuppressWildcards ActionProvider>,
) : ActionProvider {

    override fun provideAction(extra: Serializable?): ActionProvider.Action? =
        actionProviders.firstNotNullOfOrNull { actionProvider -> actionProvider.provideAction(extra) }

}
