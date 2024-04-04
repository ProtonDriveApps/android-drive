package me.proton.android.drive.ui.viewmodel

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import me.proton.android.drive.extension.asBoolean
import me.proton.core.plan.presentation.compose.usecase.ShouldUpgradeStorage

interface NotificationDotViewModel {
    val notificationDotRequested: Flow<Boolean>

    companion object {
        operator fun invoke(shouldUpgradeStorage: ShouldUpgradeStorage) =
            object : NotificationDotViewModel {
                override val notificationDotRequested: Flow<Boolean> =
                    shouldUpgradeStorage().map { it.asBoolean }
            }
    }
}
