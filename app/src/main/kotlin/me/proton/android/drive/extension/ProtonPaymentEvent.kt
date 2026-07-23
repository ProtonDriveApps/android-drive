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

val ProtonPaymentEvent.Error.failureReason: Event.Sentry.Payments.FailureReason get() = when (this) {
    is ProtonPaymentEvent.Error.GiapUnredeemed -> Event.Sentry.Payments.FailureReason.GiapUnredeemed
    is ProtonPaymentEvent.Error.UnrecoverableBillingError -> Event.Sentry.Payments.FailureReason.UnrecoverableBillingError
    is ProtonPaymentEvent.Error.UserCancelled -> Event.Sentry.Payments.FailureReason.UserCancelled
    is ProtonPaymentEvent.Error.Generic -> Event.Sentry.Payments.FailureReason.Generic
    is ProtonPaymentEvent.Error.EmptyCustomerId -> Event.Sentry.Payments.FailureReason.EmptyCustomerId
    is ProtonPaymentEvent.Error.GoogleProductDetailsNotFound -> Event.Sentry.Payments.FailureReason.GoogleProductDetailsNotFound
    is ProtonPaymentEvent.Error.PurchaseNotFound -> Event.Sentry.Payments.FailureReason.PurchaseNotFound
    is ProtonPaymentEvent.Error.RecoverableBillingError -> Event.Sentry.Payments.FailureReason.RecoverableBillingError
    is ProtonPaymentEvent.Error.SubscriptionManagedByOtherApp -> Event.Sentry.Payments.FailureReason.SubscriptionManagedByOtherApp
    is ProtonPaymentEvent.Error.UnsupportedPaymentProvider -> Event.Sentry.Payments.FailureReason.UnsupportedPaymentProvider
}
