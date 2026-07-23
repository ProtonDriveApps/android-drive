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

package me.proton.android.drive.ui.component

import android.app.Activity
import android.content.ActivityNotFoundException
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import me.proton.android.drive.log.DriveLogTag
import me.proton.android.drive.ui.effect.PaymentErrorEffect
import me.proton.android.drive.ui.viewevent.ProtonPaymentViewEvent
import me.proton.core.payment.presentation.viewmodel.ProtonPaymentEvent
import me.proton.core.plan.presentation.ui.StartUnredeemedPurchase
import me.proton.core.util.kotlin.CoreLogger

@Composable
fun PaymentUnredeemedHandler(
    paymentErrorEffect: Flow<PaymentErrorEffect>,
    viewEvent: ProtonPaymentViewEvent,
) {
    val context = LocalContext.current
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) {
        if (it.resultCode == Activity.RESULT_OK) {
            viewEvent.onRedeemedSuccessfully()
        }
    }

    LaunchedEffect(paymentErrorEffect, LocalContext.current) {
        paymentErrorEffect
            .onEach { effect ->
                when (effect) {
                    PaymentErrorEffect.GiapUnredeemed -> try {
                        launcher.launch(
                            input = StartUnredeemedPurchase.createIntent(context, Unit)
                        )
                    } catch (e: ActivityNotFoundException) {
                        CoreLogger.w(
                            tag = DriveLogTag.UI,
                            e = e,
                            message = "Unexpected UnredeemedPurchaseActivity activity not found",
                        )
                        viewEvent.onPaymentCallback(ProtonPaymentEvent.Error.UnrecoverableBillingError)
                    }
                }
            }
            .launchIn(this)
    }
}
