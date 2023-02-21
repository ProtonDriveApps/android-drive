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

package me.proton.android.drive.ui.navigation

import android.Manifest
import androidx.core.os.bundleOf
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.rule.GrantPermissionRule
import me.proton.android.drive.ui.navigation.internal.decrypt
import me.proton.android.drive.ui.navigation.internal.encrypt
import me.proton.core.crypto.android.keystore.AndroidKeyStoreCrypto
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class NavHostControllerKtTest {

    @get:Rule
    val runtimePermissionRule: GrantPermissionRule = GrantPermissionRule.grant(
        Manifest.permission.WRITE_EXTERNAL_STORAGE,
        Manifest.permission.READ_EXTERNAL_STORAGE
    )

    @Test
    fun encrypt() {
        // region Arrange
        val keyStoreCrypto = AndroidKeyStoreCrypto.default
        val bundle = bundleOf("int" to 1, "string" to "whatever", "float" to 0.2f)
        // endregion
        // region Act
        val encrypted = keyStoreCrypto.encrypt(bundle)
        assert(encrypted.containsKey("int").not()) { "Bundle should not have a key 'int'" }
        assert(encrypted.containsKey("string").not()) { "Bundle should not have a key 'string'" }
        assert(encrypted.containsKey("float").not()) { "Bundle should not have a key 'float'" }
        val decrypted = keyStoreCrypto.decrypt(encrypted)
        // endregion
        // region Assert
        assert(decrypted.getInt("int", 0) == 1) {
            "'int' should be equal to 1 but was ${decrypted.getInt("int", 0)}"
        }
        assert(decrypted.getString("string") == "whatever") {
            "'string' should be equal to 'whatever' but was ${decrypted.getString("string")}"
        }
        assert(decrypted.getFloat("float", 0f) == 0.2f) {
            "'float' should be equal to '0.2f' but was ${decrypted.getFloat("float", 0f)}"
        }
        // endregion
    }
}
