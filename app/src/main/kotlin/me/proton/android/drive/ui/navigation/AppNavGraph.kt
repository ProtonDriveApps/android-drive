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

@file:OptIn(ExperimentalCoroutinesApi::class)

package me.proton.android.drive.ui.navigation

import android.content.Intent
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NamedNavArgument
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.composable
import androidx.navigation.compose.dialog
import androidx.navigation.navArgument
import androidx.navigation.navDeepLink
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import me.proton.android.drive.extension.get
import me.proton.android.drive.extension.require
import me.proton.android.drive.extension.requireArguments
import me.proton.android.drive.extension.requireSerializable
import me.proton.android.drive.extension.runFromRoute
import me.proton.android.drive.lock.presentation.component.AppLock
import me.proton.android.drive.log.DriveLogTag
import me.proton.android.drive.photos.presentation.component.PhotosPermissionRationale
import me.proton.android.drive.ui.dialog.AutoLockDurations
import me.proton.android.drive.ui.dialog.ConfirmDeletionDialog
import me.proton.android.drive.ui.dialog.ConfirmEmptyTrashDialog
import me.proton.android.drive.ui.dialog.ConfirmSkipIssuesDialog
import me.proton.android.drive.ui.dialog.ConfirmStopSharingDialog
import me.proton.android.drive.ui.dialog.ConfirmStopSyncFolderDialog
import me.proton.android.drive.ui.dialog.FileOrFolderOptions
import me.proton.android.drive.ui.dialog.MultipleFileOrFolderOptions
import me.proton.android.drive.ui.dialog.ParentFolderOptions
import me.proton.android.drive.ui.dialog.SendFileDialog
import me.proton.android.drive.ui.dialog.SortingList
import me.proton.android.drive.ui.dialog.SystemAccessDialog
import me.proton.android.drive.ui.navigation.animation.defaultEnterSlideTransition
import me.proton.android.drive.ui.navigation.animation.defaultPopExitSlideTransition
import me.proton.android.drive.ui.navigation.internal.DriveNavHost
import me.proton.android.drive.ui.navigation.internal.MutableNavControllerSaver
import me.proton.android.drive.ui.navigation.internal.createNavController
import me.proton.android.drive.ui.navigation.internal.modalBottomSheet
import me.proton.android.drive.ui.navigation.internal.rememberAnimatedNavController
import me.proton.android.drive.ui.options.OptionsFilter
import me.proton.android.drive.ui.screen.AppAccessScreen
import me.proton.android.drive.ui.screen.BackupIssuesScreen
import me.proton.android.drive.ui.screen.FileInfoScreen
import me.proton.android.drive.ui.screen.HomeScreen
import me.proton.android.drive.ui.screen.LauncherScreen
import me.proton.android.drive.ui.screen.MoveToFolder
import me.proton.android.drive.ui.screen.OfflineScreen
import me.proton.android.drive.ui.screen.PhotosBackupScreen
import me.proton.android.drive.ui.screen.PreviewScreen
import me.proton.android.drive.ui.screen.SettingsScreen
import me.proton.android.drive.ui.screen.SigningOutScreen
import me.proton.android.drive.ui.screen.TrashScreen
import me.proton.android.drive.ui.screen.UploadToScreen
import me.proton.android.drive.ui.screen.WelcomeScreen
import me.proton.android.drive.ui.viewmodel.ConfirmStopSyncFolderDialogViewModel
import me.proton.core.account.domain.entity.Account
import me.proton.core.compose.component.bottomsheet.ModalBottomSheetViewState
import me.proton.core.crypto.common.keystore.KeyStoreCrypto
import me.proton.core.domain.entity.UserId
import me.proton.core.drive.drivelink.rename.presentation.Rename
import me.proton.core.drive.drivelink.shared.presentation.component.DiscardChangesDialog
import me.proton.core.drive.drivelink.shared.presentation.component.ShareViaLink
import me.proton.core.drive.folder.create.presentation.CreateFolder
import me.proton.core.drive.link.domain.entity.FolderId
import me.proton.core.drive.linkupload.presentation.compose.StorageFullDialog
import me.proton.core.drive.share.domain.entity.ShareId
import me.proton.core.drive.sorting.domain.entity.By
import me.proton.core.drive.sorting.domain.entity.Direction
import me.proton.core.drive.sorting.domain.entity.Sorting
import me.proton.core.drive.user.presentation.user.SignOutConfirmationDialog
import me.proton.core.util.kotlin.CoreLogger

@Composable
@ExperimentalAnimationApi
fun AppNavGraph(
    keyStoreCrypto: KeyStoreCrypto,
    deepLinkBaseUrl: String,
    clearBackstackTrigger: SharedFlow<Unit>,
    deepLinkIntent: SharedFlow<Intent>,
    locked: Flow<Boolean>,
    primaryAccount: Flow<Account?>,
    exitApp: () -> Unit,
    navigateToBugReport: () -> Unit,
    navigateToSubscription: () -> Unit,
    onDrawerStateChanged: (Boolean) -> Unit,
) {
    val navController = rememberAnimatedNavController(keyStoreCrypto)
    val localContext = LocalContext.current
    var homeNavController by rememberSaveable(saver = MutableNavControllerSaver(localContext, keyStoreCrypto)) {
        mutableStateOf(createNavController(localContext))
    }
    LaunchedEffect(navController) {
        navController
            .currentBackStack
            .onEach { backStackEntries ->
                val destinations = backStackEntries.map { entry -> entry.destination.route }.joinToString()
                CoreLogger.d(DriveLogTag.UI, "App current back stack: $destinations")
            }
            .launchIn(this)
    }
    LaunchedEffect(homeNavController) {
        homeNavController
            .currentBackStack
            .onEach { backStackEntries ->
                val destinations = backStackEntries.map { entry -> entry.destination.route }.joinToString()
                CoreLogger.d(DriveLogTag.UI, "Home current back stack: $destinations")
            }
            .launchIn(this)
    }
    LaunchedEffect(clearBackstackTrigger) {
        clearBackstackTrigger
            .collectLatest {
                navController.navigate(Screen.Launcher.route) {
                    popUpTo(0)
                }
                homeNavController = createNavController(localContext)
            }
    }
    LaunchedEffect(deepLinkIntent) {
        deepLinkIntent
            .collectLatest { intent ->
                CoreLogger.d(DriveLogTag.UI, "Deep link intent received")
                navController.handleDeepLink(intent)
                homeNavController = createNavController(localContext)
            }
    }
    AppLock(locked = locked, primaryAccount = primaryAccount) {
        AppNavGraph(
            navController = navController,
            homeNavController = homeNavController,
            deepLinkBaseUrl = deepLinkBaseUrl,
            exitApp = exitApp,
            navigateToBugReport = navigateToBugReport,
            navigateToSubscription = navigateToSubscription,
            onDrawerStateChanged = onDrawerStateChanged,
        )
    }
}

@Composable
@ExperimentalCoroutinesApi
@ExperimentalAnimationApi
fun AppNavGraph(
    navController: NavHostController,
    homeNavController: NavHostController,
    deepLinkBaseUrl: String,
    exitApp: () -> Unit,
    navigateToBugReport: () -> Unit,
    navigateToSubscription: () -> Unit,
    onDrawerStateChanged: (Boolean) -> Unit,
) {
    DriveNavHost(
        navController = navController,
        startDestination = Screen.Launcher.route,
        modifier = Modifier.fillMaxSize()
    ) {
        addLauncher(navController)
        addWelcome(navController)
        addSignOutConfirmationDialog(navController)
        addSigningOut()
        addHome(
            navController = navController,
            homeNavController = homeNavController,
            deepLinkBaseUrl = deepLinkBaseUrl,
            navigateToBugReport = navigateToBugReport,
            navigateToSubscription = navigateToSubscription,
            onDrawerStateChanged = onDrawerStateChanged,
        )
        addHomeFiles(
            navController = navController,
            homeNavController = homeNavController,
            deepLinkBaseUrl = deepLinkBaseUrl,
            navigateToBugReport = navigateToBugReport,
            navigateToSubscription = navigateToSubscription,
            onDrawerStateChanged = onDrawerStateChanged,
        )
        addHomeShared(
            navController = navController,
            homeNavController = homeNavController,
            deepLinkBaseUrl = deepLinkBaseUrl,
            navigateToBugReport = navigateToBugReport,
            navigateToSubscription = navigateToSubscription,
            onDrawerStateChanged = onDrawerStateChanged,
        )
        addHomePhotos(
            navController = navController,
            homeNavController = homeNavController,
            deepLinkBaseUrl = deepLinkBaseUrl,
            navigateToBugReport = navigateToBugReport,
            navigateToSubscription = navigateToSubscription,
            onDrawerStateChanged = onDrawerStateChanged,
        )
        addPhotosIssues(navController)
        addConfirmSkipIssues(navController)
        addConfirmStopSyncFolderDialog(navController)
        addPhotosPermissionRationale(navController)
        addSortingList()
        addFileOrFolderOptions(navController)
        addMultipleFileOrFolderOptions(navController)
        addParentFolderOptions(navController)
        addConfirmDeletionDialog(navController)
        addConfirmStopSharingDialog(navController)
        addConfirmEmptyTrashDialog(navController)
        addTrash(navController)
        addOffline(navController)
        addPagerPreview(navController)
        addSettings(navController)
        addFileInfo(navController)
        addMoveToFolder(navController)
        addRenameDialog(navController)
        addCreateFolderDialog(navController)
        addStorageFull(navController, deepLinkBaseUrl)
        addSendFile(navController)
        addShareViaLink(navController)
        addDiscardShareViaLinkChanges(navController)
        addUploadTo(navController, deepLinkBaseUrl, exitApp)
        addAppAccess(navController)
        addSystemAccessDialog(navController)
        addAutoLockDurations(navController)
        addPhotosBackup(navController)
    }
}

@ExperimentalCoroutinesApi
@ExperimentalAnimationApi
fun NavGraphBuilder.addLauncher(navController: NavHostController) = composable(
    route = Screen.Launcher.route,
) {
    LauncherScreen(
        navigateToHomeScreen = { userId ->
            navController.runFromRoute(route = Screen.Launcher.route) {
                navController.navigate(Screen.Home(userId)) {
                    popUpTo(Screen.Launcher.route) { inclusive = true }
                }
            }
        },
        navigateToWelcome = { userId ->
            navController.runFromRoute(route = Screen.Launcher.route) {
                navController.navigate(Screen.Welcome(userId))  {
                    popUpTo(Screen.Launcher.route) { inclusive = true }
                }
            }
        }
    )
}

@ExperimentalAnimationApi
fun NavGraphBuilder.addWelcome(navController: NavHostController) = composable(
    route = Screen.Welcome.route,
    arguments = listOf(navArgument(Screen.Welcome.USER_ID) { type = NavType.StringType }),
) {
    WelcomeScreen(
        navigateToLauncher = {
            navController.navigate(Screen.Launcher.route) {
                popUpTo(Screen.Welcome.route) { inclusive = true }
            }
        }
    )
}

@ExperimentalCoroutinesApi
@ExperimentalAnimationApi
fun NavGraphBuilder.addSigningOut() = composable(
    route = Screen.SigningOut.route,
    arguments = listOf(navArgument(Screen.SigningOut.USER_ID) { type = NavType.StringType })
) {
    SigningOutScreen()
}

fun NavGraphBuilder.addSignOutConfirmationDialog(navController: NavHostController) = dialog(
    route = Screen.Dialogs.SignOut.route,
    arguments = listOf(
        navArgument(Screen.Dialogs.SignOut.USER_ID) {
            type = NavType.StringType
        },
    ),
) { navBackStackEntry ->
    val userId = UserId(navBackStackEntry.require(Screen.Dialogs.SignOut.USER_ID))
    SignOutConfirmationDialog(
        onRemove = {
            navController.navigate(Screen.SigningOut(userId = userId)) {
                popUpTo(Screen.Home.route) { inclusive = true }
            }
        },
        onDismiss = {
            navController.popBackStack(
                route = Screen.Dialogs.SignOut.route,
                inclusive = true,
            )
        }
    )
}

fun NavGraphBuilder.addSortingList() = modalBottomSheet(
    route = Screen.Sorting.route,
    arguments = listOf(
        navArgument(Screen.Files.USER_ID) { type = NavType.StringType },
        navArgument(Screen.Files.FOLDER_ID) {
            type = NavType.StringType
            nullable = true
            defaultValue = null
        },
        navArgument(Screen.Sorting.BY) {
            type = NavType.EnumType(By::class.java)
            nullable = false
            defaultValue = Sorting.DEFAULT.by
        },
        navArgument(Screen.Sorting.DIRECTION) {
            type = NavType.EnumType(Direction::class.java)
            nullable = false
            defaultValue = Sorting.DEFAULT.direction
        },
    ),
) { _, runAction ->
    SortingList(runAction = runAction)
}

fun NavGraphBuilder.addFileOrFolderOptions(
    navController: NavHostController,
) = modalBottomSheet(
    route = Screen.FileOrFolderOptions.route,
    viewState = ModalBottomSheetViewState(dismissOnAction = false),
    arguments = listOf(
        navArgument(Screen.Files.USER_ID) { type = NavType.StringType },
        navArgument(Screen.FileOrFolderOptions.SHARE_ID) { type = NavType.StringType },
        navArgument(Screen.FileOrFolderOptions.LINK_ID) { type = NavType.StringType },
        navArgument(Screen.FileOrFolderOptions.OPTIONS_FILTER) {
            type = NavType.EnumType(OptionsFilter::class.java)
            defaultValue = OptionsFilter.FILES
        },
    ),
) { navBackStackEntry, runAction ->
    val userId = UserId(navBackStackEntry.require(Screen.Files.USER_ID))
    FileOrFolderOptions(
        runAction = runAction,
        navigateToInfo = { linkId ->
            navController.navigate(Screen.Info(userId, linkId)) {
                popUpTo(Screen.FileOrFolderOptions.route) { inclusive = true }
            }
        },
        navigateToMove = { linkId, parentId ->
            navController.navigate(Screen.Move(userId, linkId, parentId)) {
                popUpTo(Screen.FileOrFolderOptions.route) { inclusive = true }
            }
        },
        navigateToRename = { fileId ->
            navController.navigate(Screen.Files.Dialogs.Rename(userId, fileId)) {
                popUpTo(Screen.FileOrFolderOptions.route) { inclusive = true }
            }
        },
        navigateToDelete = { linkId ->
            navController.navigate(Screen.Files.Dialogs.ConfirmDeletion(userId, linkId)) {
                popUpTo(Screen.FileOrFolderOptions.route) { inclusive = true }
            }
        },
        navigateToSendFile = { fileId ->
            navController.navigate(Screen.SendFile(userId, fileId)) {
                popUpTo(Screen.FileOrFolderOptions.route) { inclusive = true }
            }
        },
        navigateToStopSharing = { linkId ->
              navController.navigate(Screen.Files.Dialogs.ConfirmStopSharing(userId, linkId)) {
                popUpTo(Screen.FileOrFolderOptions.route) { inclusive = true }
            }
        },
        navigateToShareViaLink = { linkId ->
            navController.navigate(Screen.ShareViaLink(userId, linkId)) {
                popUpTo(Screen.FileOrFolderOptions.route) { inclusive = true }
            }
        },
        dismiss = {
            navController.popBackStack(
                route = Screen.FileOrFolderOptions.route,
                inclusive = true,
            )
        }
    )
}

fun NavGraphBuilder.addMultipleFileOrFolderOptions(
    navController: NavHostController,
) = modalBottomSheet(
    route = Screen.MultipleFileOrFolderOptions.route,
    viewState = ModalBottomSheetViewState(dismissOnAction = false),
    arguments = listOf(
        navArgument(Screen.Files.USER_ID) { type = NavType.StringType },
        navArgument(Screen.MultipleFileOrFolderOptions.SELECTION_ID) { type = NavType.StringType },
        navArgument(Screen.MultipleFileOrFolderOptions.OPTIONS_FILTER) {
            type = NavType.EnumType(OptionsFilter::class.java)
            defaultValue = OptionsFilter.FILES
        },
    ),
) { navBackStackEntry, runAction ->
    val userId = UserId(navBackStackEntry.require(Screen.Files.USER_ID))
    MultipleFileOrFolderOptions(
        runAction = runAction,
        navigateToMove = { selectionId, parentId ->
            navController.navigate(Screen.Move(userId, selectionId, parentId)) {
                popUpTo(Screen.MultipleFileOrFolderOptions.route) { inclusive = true }
            }
        },
        dismiss = {
            navController.popBackStack(
                route = Screen.MultipleFileOrFolderOptions.route,
                inclusive = true,
            )
        },
    )
}

fun NavGraphBuilder.addParentFolderOptions(
    navController: NavHostController,
) = modalBottomSheet(
    route = Screen.ParentFolderOptions.route,
    viewState = ModalBottomSheetViewState(dismissOnAction = false),
    arguments = listOf(
        navArgument(Screen.Files.USER_ID) { type = NavType.StringType },
        navArgument(Screen.ParentFolderOptions.SHARE_ID) { type = NavType.StringType },
        navArgument(Screen.ParentFolderOptions.FOLDER_ID) { type = NavType.StringType },
    ),
) { navBackStackEntry, runAction ->
    val userId = UserId(navBackStackEntry.require(Screen.Files.USER_ID))
    ParentFolderOptions(
        runAction = runAction,
        navigateToCreateFolder = { parentId ->
            navController.navigate(Screen.Files.Dialogs.CreateFolder(userId, parentId)) {
                popUpTo(route = Screen.ParentFolderOptions.route) { inclusive = true }
            }
        },
        navigateToStorageFull = {
            navController.navigate(Screen.Dialogs.StorageFull(userId)) {
                popUpTo(route = Screen.ParentFolderOptions.route) { inclusive = true }
            }
        },
        dismiss = {
            navController.popBackStack(
                route = Screen.ParentFolderOptions.route,
                inclusive = true,
            )
        }
    )
}

@ExperimentalCoroutinesApi
fun NavGraphBuilder.addConfirmDeletionDialog(navController: NavHostController) = dialog(
    route = Screen.Files.Dialogs.ConfirmDeletion.route,
    arguments = listOf(
        navArgument(Screen.Files.USER_ID) { type = NavType.StringType },
        navArgument(Screen.Files.Dialogs.ConfirmDeletion.FILE_ID) { type = NavType.StringType },
        navArgument(Screen.Files.Dialogs.ConfirmDeletion.SHARE_ID) { type = NavType.StringType },
    ),
) {
    ConfirmDeletionDialog(
        onDismiss = {
            navController.popBackStack(
                route = Screen.Files.Dialogs.ConfirmDeletion.route,
                inclusive = true,
            )
        }
    )
}

@ExperimentalCoroutinesApi
fun NavGraphBuilder.addConfirmStopSharingDialog(navController: NavHostController) = dialog(
    route = Screen.Files.Dialogs.ConfirmStopSharing.route,
    arguments = listOf(
        navArgument(Screen.Files.USER_ID) { type = NavType.StringType },
        navArgument(Screen.Files.Dialogs.ConfirmStopSharing.LINK_ID) { type = NavType.StringType },
        navArgument(Screen.Files.Dialogs.ConfirmStopSharing.SHARE_ID) { type = NavType.StringType },
        navArgument(Screen.Files.Dialogs.ConfirmStopSharing.CONFIRM_POP_UP_ROUTE) {
            type = NavType.StringType
            nullable = true
            defaultValue = null
        },
        navArgument(Screen.Files.Dialogs.ConfirmStopSharing.CONFIRM_POP_UP_ROUTE_INCLUSIVE) { type = NavType.BoolType },
    ),
) { navBackStackEntry ->
    val confirmPopUpRoute = navBackStackEntry.get<String>(Screen.Files.Dialogs.ConfirmStopSharing.CONFIRM_POP_UP_ROUTE)
    val onDismiss: () -> Unit = {
        navController.popBackStack(
            route = Screen.Files.Dialogs.ConfirmStopSharing.route,
            inclusive = true,
        )
    }
    val onConfirm: () -> Unit = confirmPopUpRoute?.let {
        val confirmPopUpRouteInclusive = navBackStackEntry.get<Boolean>(
            Screen.Files.Dialogs.ConfirmStopSharing.CONFIRM_POP_UP_ROUTE_INCLUSIVE
        ) ?: true
        { navController.popBackStack(route = confirmPopUpRoute, inclusive = confirmPopUpRouteInclusive) }
    } ?: onDismiss
    ConfirmStopSharingDialog(
        onDismiss = onDismiss,
        onConfirm = onConfirm,
    )
}

@ExperimentalCoroutinesApi
fun NavGraphBuilder.addConfirmEmptyTrashDialog(navController: NavHostController) = dialog(
    route = Screen.Files.Dialogs.ConfirmEmptyTrash.route,
    arguments = listOf(
        navArgument(Screen.Files.USER_ID) { type = NavType.StringType },
    ),
) {
    ConfirmEmptyTrashDialog(
        onDismiss = {
            navController.popBackStack(
                route = Screen.Files.Dialogs.ConfirmEmptyTrash.route,
                inclusive = true,
            )
        }
    )
}

@ExperimentalCoroutinesApi
@ExperimentalAnimationApi
internal fun NavGraphBuilder.addHome(
    navController: NavHostController,
    homeNavController: NavHostController,
    deepLinkBaseUrl: String,
    route: String,
    startDestination: String,
    navigateToBugReport: () -> Unit,
    navigateToSubscription: () -> Unit,
    onDrawerStateChanged: (Boolean) -> Unit,
    arguments: List<NamedNavArgument> = listOf(
        navArgument(Screen.Home.USER_ID) {
            type = NavType.StringType
        },
    ),
) = composable(
    route = route,
    arguments = arguments,
) { navBackStackEntry ->
    val userId = UserId(navBackStackEntry.require(Screen.Files.USER_ID))
    val shareId = navBackStackEntry.get<String>(Screen.Files.SHARE_ID)
    val currentFolderId = navBackStackEntry.get<String>(Screen.Files.FOLDER_ID)?.let { folderId ->
        shareId?.let {
            FolderId(ShareId(userId, shareId), folderId)
        }
    }
    HomeScreen(
        userId,
        homeNavController,
        deepLinkBaseUrl,
        startDestination,
        navBackStackEntry.requireArguments(),
        navigateToBugReport = navigateToBugReport,
        onDrawerStateChanged = onDrawerStateChanged,
        navigateToSigningOut = {
            navController.navigate(Screen.Dialogs.SignOut(userId))
        },
        navigateToTrash = {
            navController.navigate(Screen.Trash(userId))
        },
        navigateToOffline = {
            navController.navigate(Screen.OfflineFiles(userId))
        },
        navigateToPreview = { fileId, pagerType, optionsFilter ->
            navController.navigate(Screen.PagerPreview(pagerType, userId, fileId, optionsFilter))
        },
        navigateToSorting = { sorting ->
            navController.navigate(
                Screen.Sorting(userId, currentFolderId, sorting.by, sorting.direction)
            )
        },
        navigateToSettings = {
            navController.navigate(Screen.Settings(userId))
        },
        navigateToFileOrFolderOptions = { linkId, optionsFilter ->
            navController.navigate(
                Screen.FileOrFolderOptions(userId, linkId, optionsFilter)
            )
        },
        navigateToMultipleFileOrFolderOptions = { selectionId, optionsFilter ->
            navController.navigate(
                Screen.MultipleFileOrFolderOptions(userId, selectionId, optionsFilter)
            )
        },
        navigateToParentFolderOptions = { folderId ->
            navController.navigate(
                Screen.ParentFolderOptions(userId, folderId)
            )
        },
        navigateToSubscription = navigateToSubscription,
        navigateToPhotosIssues = { folderId ->
            navController.navigate(
                Screen.BackupIssues.invoke(folderId)
            )
        },
        navigateToBackupSettings = {
            navController.navigate(
                Screen.Settings.PhotosBackup(userId)
            )
        },
        navigateToPhotosPermissionRationale = {
            navController.navigate(
                Screen.PhotosPermissionRationale(userId)
            )
        },
        modifier = Modifier.fillMaxSize(),
    )
}

@ExperimentalAnimationApi
@ExperimentalCoroutinesApi
fun NavGraphBuilder.addHome(
    navController: NavHostController,
    homeNavController: NavHostController,
    deepLinkBaseUrl: String,
    navigateToBugReport: () -> Unit,
    navigateToSubscription: () -> Unit,
    onDrawerStateChanged: (Boolean) -> Unit,
) = addHome(
    navController = navController,
    homeNavController = homeNavController,
    deepLinkBaseUrl = deepLinkBaseUrl,
    route = Screen.Home.route,
    startDestination = Screen.Files.route,
    navigateToBugReport = navigateToBugReport,
    navigateToSubscription = navigateToSubscription,
    onDrawerStateChanged= onDrawerStateChanged
)

@ExperimentalAnimationApi
@ExperimentalCoroutinesApi
fun NavGraphBuilder.addHomeFiles(
    navController: NavHostController,
    homeNavController: NavHostController,
    deepLinkBaseUrl: String,
    navigateToBugReport: () -> Unit,
    navigateToSubscription: () -> Unit,
    onDrawerStateChanged: (Boolean) -> Unit,
) = addHome(
    navController = navController,
    homeNavController = homeNavController,
    deepLinkBaseUrl = deepLinkBaseUrl,
    route = Screen.Files.route,
    startDestination = Screen.Files.route,
    navigateToBugReport = navigateToBugReport,
    navigateToSubscription = navigateToSubscription,
    onDrawerStateChanged= onDrawerStateChanged,
    arguments = listOf(
        navArgument(Screen.Files.USER_ID) { type = NavType.StringType },
        navArgument(Screen.Files.FOLDER_ID) {
            type = NavType.StringType
            nullable = true
            defaultValue = null
        },
        navArgument(Screen.Files.FOLDER_NAME) {
            type = NavType.StringType
            nullable = true
            defaultValue = null
        },
    ),
)

@ExperimentalAnimationApi
@ExperimentalCoroutinesApi
fun NavGraphBuilder.addHomeShared(
    navController: NavHostController,
    homeNavController: NavHostController,
    deepLinkBaseUrl: String,
    navigateToBugReport: () -> Unit,
    navigateToSubscription: () -> Unit,
    onDrawerStateChanged: (Boolean) -> Unit,
) = addHome(
    navController = navController,
    homeNavController = homeNavController,
    deepLinkBaseUrl = deepLinkBaseUrl,
    route = Screen.Shared.route,
    startDestination = Screen.Shared.route,
    navigateToBugReport = navigateToBugReport,
    navigateToSubscription = navigateToSubscription,
    onDrawerStateChanged = onDrawerStateChanged
)

@ExperimentalAnimationApi
@ExperimentalCoroutinesApi
fun NavGraphBuilder.addHomePhotos(
    navController: NavHostController,
    homeNavController: NavHostController,
    deepLinkBaseUrl: String,
    navigateToBugReport: () -> Unit,
    navigateToSubscription: () -> Unit,
    onDrawerStateChanged: (Boolean) -> Unit,
) = addHome(
    navController = navController,
    homeNavController = homeNavController,
    deepLinkBaseUrl = deepLinkBaseUrl,
    route = Screen.Photos.route,
    startDestination = Screen.Photos.route,
    navigateToBugReport = navigateToBugReport,
    navigateToSubscription = navigateToSubscription,
    onDrawerStateChanged = onDrawerStateChanged
)

fun NavGraphBuilder.addPhotosPermissionRationale(
    navController: NavHostController,
) = composable(
    route = Screen.PhotosPermissionRationale.route,
    arguments = listOf(
        navArgument(Screen.Files.USER_ID) { type = NavType.StringType },
    ),
) {
    PhotosPermissionRationale(
        modifier = Modifier
            .navigationBarsPadding()
            .offset(y = (-8).dp), // remove default modalBottomSheet top space
        // me.proton.core.compose.component.bottomsheet.ModalBottomSheet:59
        onBack = {
            navController.popBackStack(
                route = Screen.PhotosPermissionRationale.route,
                inclusive = true,
            )
        },
    )
}
fun NavGraphBuilder.addPhotosIssues(
    navController: NavHostController,
) = composable(
    route = Screen.BackupIssues.route,
    arguments = listOf(
        navArgument(Screen.BackupIssues.USER_ID) { type = NavType.StringType },
        navArgument(Screen.BackupIssues.SHARE_ID) { type = NavType.StringType },
        navArgument(Screen.BackupIssues.FOLDER_ID) { type = NavType.StringType },
    ),
) { navBackStackEntry ->
    val userId = UserId(navBackStackEntry.require(Screen.BackupIssues.USER_ID))
    val shareId = ShareId(userId, navBackStackEntry.require(Screen.BackupIssues.SHARE_ID))
    val folderId = FolderId(shareId, navBackStackEntry.require(Screen.BackupIssues.FOLDER_ID))
    BackupIssuesScreen(
        modifier = Modifier
            .navigationBarsPadding(),
        navigateBack = {
            navController.popBackStack(
                route = Screen.BackupIssues.route,
                inclusive = true,
            )
        },
        navigateToSkipIssues = {
            navController.navigate(
                Screen.BackupIssues.Dialogs.ConfirmSkipIssues(
                    folderId,
                    Screen.BackupIssues.route
                )
            )
        }
    )
}

fun NavGraphBuilder.addConfirmSkipIssues(
    navController: NavHostController,
) = dialog(
    route = Screen.BackupIssues.Dialogs.ConfirmSkipIssues.route,
    arguments = listOf(
        navArgument(Screen.BackupIssues.USER_ID) { type = NavType.StringType },
        navArgument(Screen.BackupIssues.SHARE_ID) { type = NavType.StringType },
        navArgument(Screen.BackupIssues.FOLDER_ID) { type = NavType.StringType },
        navArgument(Screen.BackupIssues.Dialogs.ConfirmSkipIssues.CONFIRM_POP_UP_ROUTE) {
            type = NavType.StringType
            nullable = true
            defaultValue = null
        },
        navArgument(Screen.BackupIssues.Dialogs.ConfirmSkipIssues.CONFIRM_POP_UP_ROUTE_INCLUSIVE) {
            type =
                NavType.BoolType
        },
    ),
) { navBackStackEntry ->
    val confirmPopUpRoute =
        navBackStackEntry.get<String>(Screen.BackupIssues.Dialogs.ConfirmSkipIssues.CONFIRM_POP_UP_ROUTE)
    val onDismiss: () -> Unit = {
        navController.popBackStack(
            route = Screen.BackupIssues.Dialogs.ConfirmSkipIssues.route,
            inclusive = true,
        )
    }
    val onConfirm: () -> Unit = confirmPopUpRoute?.let {
        val confirmPopUpRouteInclusive = navBackStackEntry.get<Boolean>(
            Screen.BackupIssues.Dialogs.ConfirmSkipIssues.CONFIRM_POP_UP_ROUTE_INCLUSIVE
        ) ?: true
        {
            navController.popBackStack(
                route = confirmPopUpRoute,
                inclusive = confirmPopUpRouteInclusive
            )
        }
    } ?: onDismiss
    ConfirmSkipIssuesDialog(
        modifier = Modifier
            .navigationBarsPadding(),
        onDismiss = onDismiss,
        onConfirm = onConfirm,
    )
}

fun NavGraphBuilder.addConfirmStopSyncFolderDialog(
    navController: NavHostController,
) = dialog(
    route = Screen.Settings.PhotosBackup.Dialogs.ConfirmStopSyncFolder.route,
    arguments = listOf(
        navArgument(Screen.Settings.USER_ID) { type = NavType.StringType },
        navArgument(Screen.Settings.SHARE_ID) { type = NavType.StringType },
        navArgument(Screen.Settings.FOLDER_ID) { type = NavType.StringType },
        navArgument(ConfirmStopSyncFolderDialogViewModel.ID) {
            type = NavType.IntType
        },
    ),
) { navBackStackEntry ->
    val onAction: () -> Unit = {
        navController.popBackStack(
            route = Screen.Settings.PhotosBackup.Dialogs.ConfirmStopSyncFolder.route,
            inclusive = true,
        )
    }
    ConfirmStopSyncFolderDialog(
        modifier = Modifier.navigationBarsPadding(),
        onDismiss = onAction,
        onConfirm = onAction,
    )
}

@ExperimentalCoroutinesApi
@ExperimentalAnimationApi
fun NavGraphBuilder.addTrash(navController: NavHostController) = composable(
    route = Screen.Trash.route,
    arguments = listOf(
        navArgument(Screen.Files.USER_ID) { type = NavType.StringType },
    ),
) { navBackStackEntry ->
    val userId = UserId(navBackStackEntry.require(Screen.Files.USER_ID))
    TrashScreen(
        navigateBack = {
            navController.popBackStack(
                route = Screen.Trash.route,
                inclusive = true,
            )
        },
        navigateToEmptyTrash = {
            navController.navigate(Screen.Files.Dialogs.ConfirmEmptyTrash(userId))
        },
        navigateToFileOrFolderOptions = { linkId ->
            navController.navigate(Screen.FileOrFolderOptions(userId, linkId))
        },
        navigateToSortingDialog = { sorting ->
            navController.navigate(Screen.Sorting(userId, null, sorting.by, sorting.direction))
        }
    )
}

@ExperimentalCoroutinesApi
@ExperimentalAnimationApi
fun NavGraphBuilder.addOffline(navController: NavHostController) = composable(
    route = Screen.OfflineFiles.route,
    arguments = listOf(
        navArgument(Screen.Files.USER_ID) { type = NavType.StringType },
        navArgument(Screen.Files.FOLDER_ID) {
            type = NavType.StringType
            nullable = true
            defaultValue = null
        },
        navArgument(Screen.Files.FOLDER_NAME) {
            type = NavType.StringType
            nullable = true
            defaultValue = null
        },
    ),
) { navBackStackEntry ->
    val userId = UserId(navBackStackEntry.require(Screen.Files.USER_ID))
    OfflineScreen(
        navigateToFiles = { folderId, folderName ->
            navController.navigate(
                Screen.OfflineFiles(userId, folderId, folderName)
            )
        },
        navigateToPreview = { pagerType, fileId ->
            navController.navigate(
                Screen.PagerPreview(pagerType, userId, fileId)
            )
        },
        navigateBack = {
            navController.popBackStack(
                route = Screen.OfflineFiles.route,
                inclusive = true,
            )
        },
        navigateToSortingDialog = { sorting ->
            navController.navigate(
                Screen.Sorting(userId, null, sorting.by, sorting.direction)
            )
        },
        navigateToFileOrFolderOptions = { linkId ->
            navController.navigate(
                Screen.FileOrFolderOptions(userId, linkId)
            )
        },
    )
}

@ExperimentalCoroutinesApi
@ExperimentalAnimationApi
fun NavGraphBuilder.addPagerPreview(navController: NavHostController) = composable(
    route = Screen.PagerPreview.route,
    enterTransition = defaultEnterSlideTransition { true },
    exitTransition = { ExitTransition.None },
    popEnterTransition = { EnterTransition.None },
    popExitTransition = defaultPopExitSlideTransition { true },
    arguments = listOf(
        navArgument(Screen.PagerPreview.PAGER_TYPE) { type = NavType.EnumType(PagerType::class.java) },
        navArgument(Screen.PagerPreview.USER_ID) { type = NavType.StringType },
        navArgument(Screen.PagerPreview.FILE_ID) { type = NavType.StringType },
        navArgument(Screen.PagerPreview.OPTIONS_FILTER) {
            type = NavType.EnumType(OptionsFilter::class.java)
            defaultValue = OptionsFilter.FILES
        },
    ),
) { navBackStackEntry ->
    val userId = UserId(navBackStackEntry.require(Screen.Files.USER_ID))
    val optionsFilter = navBackStackEntry.requireSerializable(Screen.PagerPreview.OPTIONS_FILTER, OptionsFilter::class.java)
    PreviewScreen(
        navigateBack = {
            navController.popBackStack(
                route = Screen.PagerPreview.route,
                inclusive = true,
            )
        },
        navigateToFileOrFolderOptions = { linkId ->
            navController.navigate(
                Screen.FileOrFolderOptions(userId, linkId, optionsFilter)
            )
        },
    )
}

@ExperimentalAnimationApi
fun NavGraphBuilder.addSettings(navController: NavHostController) = composable(
    route = Screen.Settings.route,
    arguments = listOf(
        navArgument(Screen.PagerPreview.USER_ID) { type = NavType.StringType },
    ),
) { navBackStackEntry ->
    val userId = UserId(navBackStackEntry.require(Screen.Files.USER_ID))
    SettingsScreen(
        navigateBack = {
            navController.popBackStack(
                route = Screen.Settings.route,
                inclusive = true,
            )
        },
        navigateToAppAccess = {
            navController.navigate(Screen.Settings.AppAccess(userId))
        },
        navigateToAutoLockDurations = {
            navController.navigate(Screen.Settings.AutoLockDurations(userId))
        },
        navigateToPhotosBackup = {
            navController.navigate(Screen.Settings.PhotosBackup(userId))
        },
    )
}

@ExperimentalAnimationApi
@OptIn(ExperimentalCoroutinesApi::class)
fun NavGraphBuilder.addFileInfo(navController: NavHostController) = composable(
    route = Screen.Info.route,
    enterTransition = defaultEnterSlideTransition(towards = AnimatedContentTransitionScope.SlideDirection.Up) { true },
    exitTransition = { ExitTransition.None },
    popEnterTransition = { EnterTransition.None },
    popExitTransition = defaultPopExitSlideTransition(towards = AnimatedContentTransitionScope.SlideDirection.Down) { true },
    arguments = listOf(
        navArgument(Screen.Info.USER_ID) { type = NavType.StringType },
        navArgument(Screen.Info.LINK_ID) { type = NavType.StringType },
    ),
) {
    FileInfoScreen(
        navigateBack = {
            navController.popBackStack(
                route = Screen.Info.route,
                inclusive = true,
            )
        },
    )
}

@ExperimentalCoroutinesApi
fun NavGraphBuilder.addRenameDialog(navController: NavHostController) = dialog(
    route = Screen.Files.Dialogs.Rename.route,
    arguments = listOf(
        navArgument(Screen.Files.USER_ID) { type = NavType.StringType },
        navArgument(Screen.Files.Dialogs.Rename.FILE_ID) {
            type = NavType.StringType
            nullable = true
            defaultValue = null
        },
        navArgument(Screen.Files.Dialogs.Rename.FOLDER_ID) {
            type = NavType.StringType
            nullable = true
            defaultValue = null
        },
    ),
) {
    Rename(
        onDismiss = {
            navController.popBackStack(
                route = Screen.Files.Dialogs.Rename.route,
                inclusive = true,
            )
        }
    )
}

@ExperimentalCoroutinesApi
fun NavGraphBuilder.addCreateFolderDialog(navController: NavHostController) = dialog(
    route = Screen.Files.Dialogs.CreateFolder.route,
    arguments = listOf(
        navArgument(Screen.Files.USER_ID) { type = NavType.StringType },
        navArgument(Screen.Files.Dialogs.CreateFolder.SHARE_ID) { type = NavType.StringType },
        navArgument(Screen.Files.Dialogs.CreateFolder.PARENT_ID) { type = NavType.StringType },
    ),
) {
    CreateFolder(
        onDismiss = {
            navController.popBackStack(
                route = Screen.Files.Dialogs.CreateFolder.route,
                inclusive = true,
            )
        }
    )
}

@ExperimentalCoroutinesApi
@ExperimentalAnimationApi
fun NavGraphBuilder.addMoveToFolder(navController: NavHostController) = composable(
    route = Screen.Move.route,
    arguments = listOf(
        navArgument(Screen.Files.USER_ID) { type = NavType.StringType },
        navArgument(Screen.Move.SELECTION_ID) {
            type = NavType.StringType
            nullable = true
            defaultValue = null
        },
        navArgument(Screen.Move.SHARE_ID) {
            type = NavType.StringType
            nullable = true
            defaultValue = null
        },
        navArgument(Screen.Move.LINK_ID) {
            type = NavType.StringType
            nullable = true
            defaultValue = null
        },
        navArgument(Screen.Move.PARENT_SHARE_ID) {
            type = NavType.StringType
            nullable = true
            defaultValue = null
        },
        navArgument(Screen.Move.PARENT_ID) {
            type = NavType.StringType
            nullable = true
            defaultValue = null
        },
    ),
) { navBackStackEntry ->
    val userId = UserId(navBackStackEntry.require(Screen.Files.USER_ID))
    MoveToFolder(
        navigateToCreateFolder = { parentId ->
            navController.navigate(Screen.Files.Dialogs.CreateFolder(userId, parentId))
        }
    ) {
        navController.popBackStack(
            route = Screen.Move.route,
            inclusive = true,
        )
    }
}

fun NavGraphBuilder.addStorageFull(navController: NavHostController, deepLinkBaseUrl: String) = dialog(
    route = Screen.Dialogs.StorageFull.route,
    deepLinks = listOf(
        navDeepLink { uriPattern = Screen.Dialogs.StorageFull.deepLink(deepLinkBaseUrl) }
    )
) {
    StorageFullDialog {
        navController.popBackStack(
            route = Screen.Dialogs.StorageFull.route,
            inclusive = true,
        )
    }
}

@ExperimentalCoroutinesApi
fun NavGraphBuilder.addSendFile(navController: NavHostController) = dialog(
    route = Screen.SendFile.route,
    arguments = listOf(
        navArgument(Screen.Files.USER_ID) { type = NavType.StringType },
        navArgument(Screen.SendFile.SHARE_ID) { type = NavType.StringType },
        navArgument(Screen.SendFile.FILE_ID) { type = NavType.StringType },
    ),
) {
    SendFileDialog {
        navController.popBackStack(
            route = Screen.SendFile.route,
            inclusive = true,
        )
    }
}

@ExperimentalAnimationApi
fun NavGraphBuilder.addShareViaLink(navController: NavHostController) = composable(
    route = Screen.ShareViaLink.route,
    enterTransition = defaultEnterSlideTransition(towards = AnimatedContentTransitionScope.SlideDirection.Up) { true },
    exitTransition = { ExitTransition.None },
    popEnterTransition = { EnterTransition.None },
    popExitTransition = defaultPopExitSlideTransition(towards = AnimatedContentTransitionScope.SlideDirection.Down) { true },
    arguments = listOf(
        navArgument(Screen.ShareViaLink.USER_ID) { type = NavType.StringType },
        navArgument(Screen.ShareViaLink.LINK_ID) { type = NavType.StringType },
    ),
) { navBackStackEntry ->
    val userId = UserId(navBackStackEntry.require(Screen.ShareViaLink.USER_ID))
    ShareViaLink(
        navigateToStopSharing = { linkId ->
            navController.navigate(Screen.Files.Dialogs.ConfirmStopSharing(userId, linkId, Screen.ShareViaLink.route))
        },
        navigateToDiscardChanges = { linkId ->
            navController.navigate(Screen.ShareViaLink.Dialogs.DiscardChanges(userId, linkId))
        },
        navigateBack = {
            navController.popBackStack(
                route = Screen.ShareViaLink.route,
                inclusive = true,
            )
        },
    )
}

@ExperimentalCoroutinesApi
fun NavGraphBuilder.addDiscardShareViaLinkChanges(navController: NavHostController) = dialog(
    route = Screen.ShareViaLink.Dialogs.DiscardChanges.route,
    arguments = listOf(
        navArgument(Screen.ShareViaLink.USER_ID) { type = NavType.StringType },
        navArgument(Screen.ShareViaLink.LINK_ID) { type = NavType.StringType },
    ),
) {
    DiscardChangesDialog(
        onDismiss = {
            navController.popBackStack(
                route = Screen.ShareViaLink.Dialogs.DiscardChanges.route,
                inclusive = true,
            )
        },
        onConfirm = {
            navController.popBackStack(
                route = Screen.ShareViaLink.route,
                inclusive = true,
            )
        }
    )
}

@ExperimentalAnimationApi
fun NavGraphBuilder.addUploadTo(
    navController: NavHostController,
    deepLinkBaseUrl: String,
    exitApp: () -> Unit,
) = composable(
    route = Screen.Upload.route,
    arguments = listOf(
        navArgument(Screen.Upload.USER_ID) { type = NavType.StringType },
        navArgument(Screen.Upload.URIS) {
            type = UploadType
            defaultValue = UploadParameters(emptyList())
        },
        navArgument(Screen.Upload.PARENT_SHARE_ID) {
            type = NavType.StringType
            nullable = true
            defaultValue = null
        },
        navArgument(Screen.Upload.PARENT_ID) {
            type = NavType.StringType
            nullable = true
            defaultValue = null
        },
    ),
    deepLinks = listOf(
        navDeepLink { uriPattern = Screen.Upload.deepLink(deepLinkBaseUrl) }
    )
) { navBackStackEntry ->
    val userId = UserId(navBackStackEntry.require(Screen.Upload.USER_ID))
    UploadToScreen(
        navigateToStorageFull = {
            navController.navigate(Screen.Dialogs.StorageFull(userId))
        },
        navigateToCreateFolder = { parentId ->
            navController.navigate(Screen.Files.Dialogs.CreateFolder(userId, parentId))
        },
        exitApp = exitApp,
    )
}

@ExperimentalAnimationApi
fun NavGraphBuilder.addAppAccess(navController: NavHostController) = composable(
    route = Screen.Settings.AppAccess.route,
    enterTransition = defaultEnterSlideTransition { true },
    exitTransition = { ExitTransition.None },
    popEnterTransition = { EnterTransition.None },
    popExitTransition = defaultPopExitSlideTransition { true },
    arguments = listOf(
        navArgument(Screen.Settings.USER_ID) { type = NavType.StringType },
    ),
) { navBackStackEntry ->
    val userId = UserId(navBackStackEntry.require(Screen.Settings.USER_ID))
    AppAccessScreen(
        navigateToSystemAccess = {
            navController.navigate(Screen.Settings.AppAccess.Dialogs.SystemAccess(userId))
        },
        navigateBack = {
            navController.popBackStack(
                route = Screen.Settings.AppAccess.route,
                inclusive = true,
            )
        },
    )
}

@ExperimentalCoroutinesApi
fun NavGraphBuilder.addSystemAccessDialog(navController: NavHostController) = dialog(
    route = Screen.Settings.AppAccess.Dialogs.SystemAccess.route,
    arguments = listOf(
        navArgument(Screen.Settings.USER_ID) { type = NavType.StringType },
    ),
) {
    SystemAccessDialog(
        onDismiss = {
            navController.popBackStack(
                route = Screen.Settings.AppAccess.Dialogs.SystemAccess.route,
                inclusive = true,
            )
        }
    )
}

fun NavGraphBuilder.addAutoLockDurations(
    navController: NavHostController,
) = modalBottomSheet(
    route = Screen.Settings.AutoLockDurations.route,
    viewState = ModalBottomSheetViewState(dismissOnAction = false),
) { _, runAction ->
    AutoLockDurations(
        runAction = runAction,
        dismiss = {
            navController.popBackStack(
                route = Screen.Settings.AutoLockDurations.route,
                inclusive = true,
            )
        }
    )
}

@ExperimentalAnimationApi
fun NavGraphBuilder.addPhotosBackup(navController: NavHostController) = composable(
    route = Screen.Settings.PhotosBackup.route,
    enterTransition = defaultEnterSlideTransition { true },
    exitTransition = { ExitTransition.None },
    popEnterTransition = { EnterTransition.None },
    popExitTransition = defaultPopExitSlideTransition { true },
    arguments = listOf(
        navArgument(Screen.Settings.USER_ID) { type = NavType.StringType },
    ),
) { navBackStackEntry ->
    val userId = UserId(navBackStackEntry.require(Screen.Settings.USER_ID))
    PhotosBackupScreen(
        navigateToPhotosPermissionRationale = {
            navController.navigate(
                Screen.PhotosPermissionRationale(userId)
            )
        },
        navigateToConfirmStopSyncFolder = { folderId, id ->
            navController.navigate(
                Screen.Settings.PhotosBackup.Dialogs.ConfirmStopSyncFolder.invoke(folderId, id)
            )
        },
        navigateBack = {
            navController.popBackStack(
                route = Screen.Settings.PhotosBackup.route,
                inclusive = true,
            )
        },
    )
}
