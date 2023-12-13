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

package me.proton.android.drive.ui.navigation.internal

import android.content.Context
import android.os.Bundle
import android.os.Parcel
import android.util.Base64
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavDestination
import androidx.navigation.NavHostController
import androidx.navigation.Navigator
import androidx.navigation.compose.ComposeNavigator
import androidx.navigation.compose.DialogNavigator
import me.proton.core.crypto.common.keystore.KeyStoreCrypto

@ExperimentalAnimationApi
@Composable
fun rememberAnimatedNavController(
    keyStoreCrypto: KeyStoreCrypto,
    vararg navigators: Navigator<out NavDestination>
): NavHostController {
    val context = LocalContext.current
    return rememberSaveable(inputs = navigators, saver = NavControllerSaver(context, keyStoreCrypto)) {
        createNavController(context)
    }.apply {
        for (navigator in navigators) {
            navigatorProvider.addNavigator(navigator)
        }
    }
}

@ExperimentalAnimationApi
fun createNavController(context: Context) =
    NavHostController(context).apply {
        navigatorProvider.addNavigator(ComposeNavigator())
        navigatorProvider.addNavigator(DialogNavigator())
        navigatorProvider.addNavigator(ModalBottomSheetNavigator())
    }

/**
 * Saver to save and restore the NavController across config change and process death.
 */
@Suppress("FunctionName")
@ExperimentalAnimationApi
private fun NavControllerSaver(
    context: Context,
    keyStoreCrypto: KeyStoreCrypto,
): Saver<NavHostController, *> = Saver(
    save = { keyStoreCrypto.encrypt(it.saveState()) },
    restore = { createNavController(context).apply { restoreState(keyStoreCrypto.decrypt(it)) } }
)

@Suppress("FunctionName")
@ExperimentalAnimationApi
fun MutableNavControllerSaver(
    context: Context,
    keyStoreCrypto: KeyStoreCrypto,
): Saver<MutableState<NavHostController>, *> = Saver(
    save = { keyStoreCrypto.encrypt(it.value.saveState()) },
    restore = { mutableStateOf(createNavController(context).apply { restoreState(keyStoreCrypto.decrypt(it)) }) }
)

internal fun KeyStoreCrypto.encrypt(bundle: Bundle?) = Bundle().apply {
    if (bundle == null) {
        return@apply
    }
    val parcel = Parcel.obtain()
    bundle.writeToParcel(parcel, 0)
    val base64 = String(Base64.encode(parcel.marshall(), Base64.NO_WRAP))
    val (key, value) = runCatching {
        PROTON_NAV_ENCRYPTED_KEY to encrypt(base64)
    }.getOrNull() ?: (PROTON_NAV_BASE_64_KEY to base64)
    putString(key, value)
    parcel.recycle()
}

internal fun KeyStoreCrypto.decrypt(bundle: Bundle): Bundle {
    val value = when {
        bundle.containsKey(PROTON_NAV_ENCRYPTED_KEY) -> decrypt(
            requireNotNull(bundle.getString(PROTON_NAV_ENCRYPTED_KEY))
        )
        bundle.containsKey(PROTON_NAV_BASE_64_KEY) -> requireNotNull(
            bundle.getString(PROTON_NAV_BASE_64_KEY)
        )
        else -> return Bundle()
    }
    val bytes = Base64.decode(value, Base64.NO_WRAP)
    val parcel = Parcel.obtain()
    parcel.unmarshall(bytes, 0, bytes.size)
    parcel.setDataPosition(0)
    val decrypted = Bundle.CREATOR.createFromParcel(parcel)
    parcel.recycle()
    return decrypted
}

private const val PROTON_NAV_ENCRYPTED_KEY = "proton.nav.bundle.encrypted"
private const val PROTON_NAV_BASE_64_KEY = "proton.nav.bundle.base64"
