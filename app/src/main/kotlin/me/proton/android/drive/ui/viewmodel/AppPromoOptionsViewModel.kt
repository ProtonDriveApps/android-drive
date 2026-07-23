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

import android.content.Context
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.launch
import me.proton.android.drive.extension.log
import me.proton.android.drive.ui.viewevent.AppPromoOptionsViewEvent
import me.proton.android.drive.ui.viewstate.AppPromoOptionsViewState
import me.proton.android.drive.usecase.MarkRatingBoosterAsShown
import me.proton.core.drive.announce.event.domain.entity.Event
import me.proton.core.drive.announce.event.domain.usecase.AsyncAnnounceEvent
import me.proton.core.drive.base.domain.log.LogTag.VIEW_MODEL
import me.proton.core.drive.base.presentation.viewmodel.UserViewModel
import javax.inject.Inject
import me.proton.core.drive.i18n.R as I18N

@HiltViewModel
class AppPromoOptionsViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    @ApplicationContext appContext: Context,
    private val markRatingBoosterAsShown: MarkRatingBoosterAsShown,
    private val asyncAnnounceEvent: AsyncAnnounceEvent,
) : ViewModel(), UserViewModel by UserViewModel(savedStateHandle) {
    val initialViewState = AppPromoOptionsViewState(
        title = appContext.getString(
            I18N.string.promo_app_title,
            appContext.getString(I18N.string.app_name)
        ),
        description = appContext.getString(
            I18N.string.promo_app_description,
            appContext.getString(I18N.string.app_name)
        ),
        positiveButtonTitle = appContext.getString(I18N.string.promo_app_positive_button),
        negativeButtonTitle = appContext.getString(I18N.string.promo_app_negative_button),
    )

    fun viewEvent(
        navigateToRatingBooster: () -> Unit,
        navigateToContactSupportOptions: () -> Unit,
    ): AppPromoOptionsViewEvent = object : AppPromoOptionsViewEvent {
        override val onPositive = { onPositiveFeedback(navigateToRatingBooster) }
        override val onNegative = { onNegativeFeedback(navigateToContactSupportOptions) }
        override val onShown = { onShown() }
    }

    private fun onPositiveFeedback(navigateToRatingBooster: () -> Unit) {
        asyncAnnounceEvent(
            userId = userId,
            event = Event.Sentry.AppPromotion(Event.Sentry.AppPromotion.Action.LIKE_IT),
        )
        navigateToRatingBooster()
    }

    private fun onNegativeFeedback(navigateToContactSupportOptions: () -> Unit) {
        viewModelScope.launch {
            asyncAnnounceEvent(
                userId = userId,
                event = Event.Sentry.AppPromotion(Event.Sentry.AppPromotion.Action.COULD_BE_BETTER),
            )
            markRatingBoosterAsShown(userId = userId).onFailure { error ->
                error.log(VIEW_MODEL, "Marking rating booster as shown failed")
            }
            navigateToContactSupportOptions()
        }
    }

    private fun onShown() {
        asyncAnnounceEvent(
            userId = userId,
            event = Event.Sentry.AppPromotion(Event.Sentry.AppPromotion.Action.SCREEN_SHOWN),
        )
    }
}
