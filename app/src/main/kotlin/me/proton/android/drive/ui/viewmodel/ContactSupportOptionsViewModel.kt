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

package me.proton.android.drive.ui.viewmodel

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import androidx.core.net.toUri
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.launch
import me.proton.android.drive.ui.viewevent.ContactSupportOptionsViewEvent
import me.proton.android.drive.ui.viewstate.ContactSupportOptionsViewState
import me.proton.core.drive.base.domain.usecase.BroadcastMessages
import me.proton.core.drive.base.presentation.viewmodel.UserViewModel
import me.proton.core.drive.messagequeue.domain.entity.BroadcastMessage
import javax.inject.Inject
import me.proton.core.drive.i18n.R as I18N

@HiltViewModel
class ContactSupportOptionsViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    @param:ApplicationContext private val appContext: Context,
    private val broadcastMessages: BroadcastMessages,
) : ViewModel(), UserViewModel by UserViewModel(savedStateHandle) {

    val initialViewState = ContactSupportOptionsViewState(
        title = appContext.getString(I18N.string.support_contact_options_title),
        description = appContext.getString(I18N.string.support_contact_options_description),
        contactUsButtonTitle = appContext.getString(I18N.string.support_contact_options_action_button),
        notNowButtonTitle = appContext.getString(I18N.string.support_contact_options_cancel_button)
    )

    fun viewEvent(
        navigateBack: () -> Unit,
    ): ContactSupportOptionsViewEvent = object : ContactSupportOptionsViewEvent {
        override val onContactUs = { onContactUs(navigateBack) }
        override val onNotNow = { navigateBack() }
    }

    private fun onContactUs(onDismiss: () -> Unit) {
        try {
            val url = appContext.getString(I18N.string.support_contact_options_action_url)
            appContext.startActivity(
                Intent(Intent.ACTION_VIEW, url.toUri())
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            )
        } catch (_: ActivityNotFoundException) {
            viewModelScope.launch {
                broadcastMessages(
                    userId = userId,
                    message = appContext.getString(I18N.string.common_error_no_browser_available),
                    type = BroadcastMessage.Type.ERROR,
                )
            }
        }
        onDismiss()
    }
}
