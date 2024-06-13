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

package me.proton.android.drive.ui.effect

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import me.proton.android.drive.ui.viewstate.HomeScaffoldState

sealed class HomeEffect {
    data object OpenDrawer : HomeEffect()
    data class ShowSnackbar(val message: String) : HomeEffect()
    data class BottomNavigation(val enabled: Boolean) : HomeEffect()
}

interface HomeTabViewModel {
    val homeEffect: Flow<HomeEffect>
}

@Composable
fun HomeTabViewModel.HandleHomeEffect(homeScaffoldState: HomeScaffoldState) {
    LaunchedEffect(this, LocalContext.current) {
        launch {
            homeEffect
                .collectLatest { effect ->
                    when (effect) {
                        is HomeEffect.OpenDrawer -> homeScaffoldState.scaffoldState.drawerState.open()
                        is HomeEffect.ShowSnackbar -> homeScaffoldState.scaffoldState.snackbarHostState.showSnackbar(
                            effect.message
                        )
                        is HomeEffect.BottomNavigation -> homeScaffoldState.bottomNavigationEnabled.value = effect.enabled
                    }
                }
        }
    }
}
