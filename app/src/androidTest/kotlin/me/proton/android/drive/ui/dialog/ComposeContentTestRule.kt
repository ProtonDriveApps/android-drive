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

package me.proton.android.drive.ui.dialog

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.ComposeContentTestRule
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.dialog
import androidx.navigation.compose.rememberNavController

const val HOME_ROUTE = "home"
const val DIALOG_ROUTE = "dialog"
const val SHOW_DIALOG_BUTTON = "show dialog"

fun ComposeContentTestRule.setupDialog(dialog: @Composable (navController: NavHostController) -> Unit) {
    this.setContent {
        val navController = rememberNavController()
        NavHost(
            navController = navController,
            startDestination = HOME_ROUTE,
        ) {
            composable(HOME_ROUTE) {
                Box(modifier = Modifier.fillMaxSize()) {
                    Button(
                        onClick = { navController.navigate(DIALOG_ROUTE) },
                    ) {
                        Text(text = SHOW_DIALOG_BUTTON)
                    }
                }
            }
            dialog(DIALOG_ROUTE) {
                dialog(navController)
            }
        }
    }
}
