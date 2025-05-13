/*
 * Copyright (c) 2024 Proton AG.
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

package me.proton.android.drive.ui.viewmodel

import android.content.Context
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import me.proton.android.drive.usecase.MarkWhatsNewAsShown
import me.proton.core.drive.base.domain.extension.flowOf
import me.proton.core.drive.base.domain.extension.getOrNull
import me.proton.core.drive.base.domain.log.LogTag.VIEW_MODEL
import me.proton.core.drive.base.presentation.viewevent.WhatsNewViewEvent
import me.proton.core.drive.base.presentation.viewmodel.UserViewModel
import me.proton.core.drive.base.presentation.viewstate.WhatsNewViewState
import me.proton.drive.android.settings.domain.entity.WhatsNewKey
import javax.inject.Inject
import me.proton.core.drive.base.presentation.R as BasePresentation
import me.proton.core.drive.i18n.R as I18N

@HiltViewModel
class WhatsNewViewModel @Inject constructor(
    @ApplicationContext appContext: Context,
    savedStateHandle: SavedStateHandle,
    private val markWhatsNewAsShown: MarkWhatsNewAsShown,
) : ViewModel(), UserViewModel by UserViewModel(savedStateHandle) {
    private val key: WhatsNewKey = requireNotNull(savedStateHandle.get<WhatsNewKey>(KEY))
    private var dismiss: (() -> Unit)? = null

    val viewState: Flow<WhatsNewViewState?> = flowOf {
        when (key) {
            WhatsNewKey.ALBUMS -> WhatsNewViewState(
                title = appContext.getString(I18N.string.whats_new_albums_title),
                description = appContext.getString(I18N.string.whats_new_albums_description),
                action = appContext.getString(I18N.string.whats_new_albums_action),
                image = BasePresentation.drawable.img_whats_new_albums,
            )
            else -> null
        }
    }

    fun viewEvent(dismiss: () -> Unit): WhatsNewViewEvent = object : WhatsNewViewEvent {
        override val onDone = { dismiss() }
        override val whatsNewShown = { whatsNewShown() }
    }.also {
        this.dismiss = dismiss
    }

    private fun whatsNewShown() {
        viewModelScope.launch {
            markWhatsNewAsShown(key)
                .getOrNull(VIEW_MODEL, "Marking whats new as shown failed for: $key")
        }
    }

    companion object {
        const val KEY = "key"
    }
}
