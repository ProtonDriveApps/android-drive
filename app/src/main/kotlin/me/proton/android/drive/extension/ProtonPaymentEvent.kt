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

package me.proton.android.drive.extension

import me.proton.core.drive.announce.event.domain.entity.Event
import me.proton.core.payment.presentation.viewmodel.ProtonPaymentEvent

val ProtonPaymentEvent.Error.failureReason: Event.Sentry.SummerSale2026.FailureReason get() = when (this) {
    is ProtonPaymentEvent.Error.GiapUnredeemed -> Event.Sentry.SummerSale2026.FailureReason.GiapUnredeemed
    is ProtonPaymentEvent.Error.UnrecoverableBillingError -> Event.Sentry.SummerSale2026.FailureReason.UnrecoverableBillingError
    is ProtonPaymentEvent.Error.UserCancelled -> Event.Sentry.SummerSale2026.FailureReason.UserCancelled
    is ProtonPaymentEvent.Error.Generic -> Event.Sentry.SummerSale2026.FailureReason.Generic
    is ProtonPaymentEvent.Error.EmptyCustomerId -> Event.Sentry.SummerSale2026.FailureReason.EmptyCustomerId
    is ProtonPaymentEvent.Error.GoogleProductDetailsNotFound -> Event.Sentry.SummerSale2026.FailureReason.GoogleProductDetailsNotFound
    is ProtonPaymentEvent.Error.PurchaseNotFound -> Event.Sentry.SummerSale2026.FailureReason.PurchaseNotFound
    is ProtonPaymentEvent.Error.RecoverableBillingError -> Event.Sentry.SummerSale2026.FailureReason.RecoverableBillingError
    is ProtonPaymentEvent.Error.SubscriptionManagedByOtherApp -> Event.Sentry.SummerSale2026.FailureReason.SubscriptionManagedByOtherApp
    is ProtonPaymentEvent.Error.UnsupportedPaymentProvider -> Event.Sentry.SummerSale2026.FailureReason.UnsupportedPaymentProvider
}
