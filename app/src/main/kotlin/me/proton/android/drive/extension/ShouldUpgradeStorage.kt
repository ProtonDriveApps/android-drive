package me.proton.android.drive.extension

import me.proton.core.plan.presentation.compose.usecase.ShouldUpgradeStorage

internal val ShouldUpgradeStorage.Result.asBoolean: Boolean get() = this != ShouldUpgradeStorage.Result.NoUpgrade
