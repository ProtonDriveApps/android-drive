/*
 * Copyright (c) 2023-2024 Proton AG.
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

import android.os.Bundle
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.runtime.Composable
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import androidx.navigation.navDeepLink
import kotlinx.coroutines.ExperimentalCoroutinesApi
import me.proton.android.drive.extension.get
import me.proton.android.drive.ui.navigation.animation.defaultEnterSlideTransition
import me.proton.android.drive.ui.navigation.animation.defaultExitSlideTransition
import me.proton.android.drive.ui.navigation.animation.defaultPopEnterSlideTransition
import me.proton.android.drive.ui.navigation.animation.defaultPopExitSlideTransition
import me.proton.android.drive.ui.navigation.internal.DriveNavHost
import me.proton.android.drive.ui.screen.ComputersScreen
import me.proton.android.drive.ui.screen.FilesScreen
import me.proton.android.drive.ui.screen.PhotosAndAlbumsScreen
import me.proton.android.drive.ui.screen.PhotosScreen
import me.proton.android.drive.ui.screen.SharedTabsScreen
import me.proton.android.drive.ui.screen.SyncedFoldersScreen
import me.proton.android.drive.ui.viewstate.HomeScaffoldState
import me.proton.core.domain.entity.UserId
import me.proton.core.drive.device.domain.entity.DeviceId
import me.proton.core.drive.link.domain.entity.AlbumId
import me.proton.core.drive.link.domain.entity.FileId
import me.proton.core.drive.link.domain.entity.FolderId
import me.proton.core.drive.link.domain.entity.LinkId
import me.proton.core.drive.link.domain.entity.PhotoTag
import me.proton.core.drive.link.selection.domain.entity.SelectionId
import me.proton.core.drive.share.domain.entity.ShareId
import me.proton.core.drive.sorting.domain.entity.Sorting

@Composable
@ExperimentalCoroutinesApi
@ExperimentalAnimationApi
fun HomeNavGraph(
    homeNavController: NavHostController,
    deepLinkBaseUrl: String,
    arguments: Bundle,
    startDestination: String,
    homeScaffoldState: HomeScaffoldState,
    navigateToPreview: (fileId: FileId, pagerType: PagerType) -> Unit,
    navigateToPreviewWithTag: (fileId: FileId, pagerType: PagerType, photoTag: PhotoTag) -> Unit,
    navigateToSorting: (sorting: Sorting) -> Unit,
    navigateToFileOrFolderOptions: (LinkId, SelectionId?) -> Unit,
    navigateToMultipleFileOrFolderOptions: (selectionId: SelectionId) -> Unit,
    navigateToParentFolderOptions: (folderId: FolderId) -> Unit,
    navigateToPhotosPermissionRationale: () -> Unit,
    navigateToSubscription: () -> Unit,
    navigateToPhotosIssues: (FolderId) -> Unit,
    navigateToPhotosUpsell: () -> Unit,
    navigateToBackupSettings: () -> Unit,
    navigateToComputerOptions: (deviceId: DeviceId) -> Unit,
    navigateToNotificationPermissionRationale: () -> Unit,
    navigateToUserInvitation: (Boolean) -> Unit,
    navigateToCreateNewAlbum: () -> Unit,
    navigateToAlbum: (AlbumId) -> Unit,
    navigateToBlackFridayPromo: () -> Unit,
) = DriveNavHost(
    navController = homeNavController,
    startDestination = startDestination
) {
    addFiles(
        homeNavController,
        deepLinkBaseUrl,
        arguments,
        homeScaffoldState,
        { fileId -> navigateToPreview(fileId, PagerType.FOLDER) },
        navigateToSorting,
        { linkId -> navigateToFileOrFolderOptions(linkId, null) },
        { selectionId -> navigateToMultipleFileOrFolderOptions(selectionId) },
        navigateToParentFolderOptions,
        navigateToSubscription,
        navigateToBlackFridayPromo,
    )
    addPhotos(
        homeNavController,
        deepLinkBaseUrl,
        arguments,
        homeScaffoldState,
        navigateToPhotosPermissionRationale,
        navigateToPhotosPreview = { fileId, photoTag ->
            if (photoTag == null) {
                navigateToPreview(fileId, PagerType.PHOTO)
            } else {
                navigateToPreviewWithTag(fileId, PagerType.PHOTO, photoTag)
            }
        },
        navigateToPhotosOptions = { fileId, selectionId -> navigateToFileOrFolderOptions(fileId, selectionId) },
        navigateToMultiplePhotosOptions = { selectionId ->
            navigateToMultipleFileOrFolderOptions(selectionId)
        },
        navigateToSubscription = navigateToSubscription,
        navigateToPhotosIssues = navigateToPhotosIssues,
        navigateToPhotosUpsell = navigateToPhotosUpsell,
        navigateToBackupSettings = navigateToBackupSettings,
        navigateToNotificationPermissionRationale = navigateToNotificationPermissionRationale,
        navigateToBlackFridayPromo = navigateToBlackFridayPromo,
    )
    addPhotosAndAlbums(
        homeNavController,
        deepLinkBaseUrl,
        arguments,
        homeScaffoldState,
        navigateToPhotosPermissionRationale,
        navigateToPhotosPreview = { fileId, photoTag ->
            if (photoTag == null) {
                navigateToPreview(fileId, PagerType.PHOTO)
            } else {
                navigateToPreviewWithTag(fileId, PagerType.PHOTO, photoTag)
            }
        },
        navigateToPhotosOptions = { fileId, selectionId -> navigateToFileOrFolderOptions(fileId, selectionId) },
        navigateToMultiplePhotosOptions = { selectionId ->
            navigateToMultipleFileOrFolderOptions(selectionId)
        },
        navigateToSubscription = navigateToSubscription,
        navigateToPhotosIssues = navigateToPhotosIssues,
        navigateToPhotosUpsell = navigateToPhotosUpsell,
        navigateToBackupSettings = navigateToBackupSettings,
        navigateToNotificationPermissionRationale = navigateToNotificationPermissionRationale,
        navigateToCreateNewAlbum = navigateToCreateNewAlbum,
        navigateToAlbum = navigateToAlbum,
        navigateToUserInvitation = navigateToUserInvitation,
        navigateToBlackFridayPromo = navigateToBlackFridayPromo,
    )
    addComputers(
        homeNavController,
        deepLinkBaseUrl,
        arguments,
        homeScaffoldState,
        { fileId -> navigateToPreview(fileId, PagerType.FOLDER) },
        navigateToSorting,
        { linkId -> navigateToFileOrFolderOptions(linkId, null) },
        { selectionId -> navigateToMultipleFileOrFolderOptions(selectionId) },
        navigateToParentFolderOptions,
        navigateToComputerOptions,
        navigateToSubscription,
        navigateToBlackFridayPromo,
    )
    addSharedTabs(
        navController = homeNavController,
        deepLinkBaseUrl = deepLinkBaseUrl,
        arguments = arguments,
        homeScaffoldState = homeScaffoldState,
        navigateToFolderPreview = { fileId -> navigateToPreview(fileId, PagerType.FOLDER) },
        navigateToSinglePreview = { fileId -> navigateToPreview(fileId, PagerType.SINGLE) },
        navigateToAlbum = navigateToAlbum,
        navigateToSorting = navigateToSorting,
        navigateToFileOrFolderOptions = { linkId -> navigateToFileOrFolderOptions(linkId, null) },
        navigateToMultipleFileOrFolderOptions = { selectionId ->
            navigateToMultipleFileOrFolderOptions(selectionId)
        },
        navigateToParentFolderOptions = navigateToParentFolderOptions,
        navigateToUserInvitation = navigateToUserInvitation,
        navigateToSubscription,
        navigateToBlackFridayPromo,
    )
}

@ExperimentalCoroutinesApi
@ExperimentalAnimationApi
fun NavGraphBuilder.addFiles(
    navController: NavHostController,
    deepLinkBaseUrl: String,
    arguments: Bundle,
    homeScaffoldState: HomeScaffoldState,
    navigateToPreview: (linkId: FileId) -> Unit,
    navigateToSorting: (sorting: Sorting) -> Unit,
    navigateToFileOrFolderOptions: (LinkId) -> Unit,
    navigateToMultipleFileOrFolderOptions: (SelectionId) -> Unit,
    navigateToParentFolderOptions: (folderId: FolderId) -> Unit,
    navigateToSubscription: () -> Unit,
    navigateToBlackFridayPromo: () -> Unit,
) = composable(
    route = Screen.Files.route,
    enterTransition = defaultEnterSlideTransition {
        targetState.get<String>(Screen.Files.FOLDER_ID) != null &&
                initialState.get<String>(Screen.Files.FOLDER_ID) != targetState.get<String>(Screen.Files.FOLDER_ID)
    },
    exitTransition = defaultExitSlideTransition {
        initialState.get<String>(Screen.Files.FOLDER_ID) != targetState.get<String>(Screen.Files.FOLDER_ID)
    },
    popEnterTransition = defaultPopEnterSlideTransition {
        initialState.get<String>(Screen.Files.FOLDER_ID) != targetState.get<String>(Screen.Files.FOLDER_ID)
    },
    popExitTransition = defaultPopExitSlideTransition {
        initialState.get<String>(Screen.Files.FOLDER_ID) != null &&
                initialState.get<String>(Screen.Files.FOLDER_ID) != targetState.get<String>(Screen.Files.FOLDER_ID)
    },
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
        }
    ),
    deepLinks = listOf(
        navDeepLink { uriPattern = Screen.Files.deepLink(deepLinkBaseUrl) }
    )
) { navBackStackEntry ->
    navBackStackEntry.get<String>(Screen.Files.USER_ID)?.let { userId ->
        FilesScreen(
            homeScaffoldState,
            navigateToFiles = { folderId, folderName ->
                navController.navigate(Screen.Files(UserId(userId), folderId, folderName))
            },
            navigateToPreview = navigateToPreview,
            navigateToSortingDialog = navigateToSorting,
            navigateBack = { navController.popBackStack() },
            navigateToFileOrFolderOptions = navigateToFileOrFolderOptions,
            navigateToMultipleFileOrFolderOptions = navigateToMultipleFileOrFolderOptions,
            navigateToParentFolderOptions = navigateToParentFolderOptions,
            navigateToSubscription = navigateToSubscription,
            navigateToBlackFridayPromo = navigateToBlackFridayPromo,
        )
    } ?: let {
        val userId = UserId(requireNotNull(arguments.getString(Screen.Files.USER_ID)))
        val folderId = arguments.getString(Screen.Files.SHARE_ID)?.let { shareId ->
            arguments.getString(Screen.Files.FOLDER_ID)?.let { folderId ->
                FolderId(ShareId(userId, shareId), folderId)
            }
        }
        val folderName = arguments.getString(Screen.Files.FOLDER_NAME)
        navController.navigate(Screen.Files(userId, folderId, folderName)) {
            popUpTo(navController.graph.findStartDestination().id) {
                inclusive = true
            }
        }
    }
}

@ExperimentalAnimationApi
fun NavGraphBuilder.addPhotos(
    navController: NavHostController,
    deepLinkBaseUrl: String,
    arguments: Bundle,
    homeScaffoldState: HomeScaffoldState,
    navigateToPhotosPermissionRationale: () -> Unit,
    navigateToPhotosPreview: (fileId: FileId, photoTag: PhotoTag?) -> Unit,
    navigateToPhotosOptions: (fileId: FileId, SelectionId?) -> Unit,
    navigateToMultiplePhotosOptions: (selectionId: SelectionId) -> Unit,
    navigateToSubscription: () -> Unit,
    navigateToPhotosIssues: (FolderId) -> Unit,
    navigateToPhotosUpsell: () -> Unit,
    navigateToBackupSettings: () -> Unit,
    navigateToNotificationPermissionRationale: () -> Unit,
    navigateToBlackFridayPromo: () -> Unit,
) = composable(
    route = Screen.Photos.route,
    arguments = listOf(
        navArgument(Screen.Photos.USER_ID) { type = NavType.StringType },
        navArgument(Screen.Photos.SHARE_ID) {
            type = NavType.StringType
            nullable = true
            defaultValue = null
        },
    ),
    deepLinks = listOf(
        navDeepLink { uriPattern = Screen.Photos.deepLink(deepLinkBaseUrl) }
    )
) { navBackStackEntry ->
    navBackStackEntry.get<String>(Screen.Photos.USER_ID)?.let { _ ->
        PhotosScreen(
            homeScaffoldState = homeScaffoldState,
            navigateToPhotosPermissionRationale = navigateToPhotosPermissionRationale,
            navigateToPhotosPreview = navigateToPhotosPreview,
            navigateToPhotosOptions = navigateToPhotosOptions,
            navigateToMultiplePhotosOptions = navigateToMultiplePhotosOptions,
            navigateToSubscription = navigateToSubscription,
            navigateToPhotosIssues = navigateToPhotosIssues,
            navigateToPhotosUpsell = navigateToPhotosUpsell,
            navigateToBackupSettings = navigateToBackupSettings,
            navigateToNotificationPermissionRationale = navigateToNotificationPermissionRationale,
            navigateToBlackFridayPromo = navigateToBlackFridayPromo,
        )
    } ?: let {
        val userId = UserId(requireNotNull(arguments.getString(Screen.Photos.USER_ID)))
        val shareId = arguments.getString(Screen.Photos.SHARE_ID)?.let { shareId ->
            ShareId(userId, shareId)
        }
        navController.navigate(Screen.Photos(userId, shareId)) {
            popUpTo(navController.graph.findStartDestination().id) {
                inclusive = true
            }
        }
    }
}

@ExperimentalAnimationApi
fun NavGraphBuilder.addPhotosAndAlbums(
    navController: NavHostController,
    deepLinkBaseUrl: String,
    arguments: Bundle,
    homeScaffoldState: HomeScaffoldState,
    navigateToPhotosPermissionRationale: () -> Unit,
    navigateToPhotosPreview: (fileId: FileId, PhotoTag?) -> Unit,
    navigateToPhotosOptions: (fileId: FileId, SelectionId?) -> Unit,
    navigateToMultiplePhotosOptions: (selectionId: SelectionId) -> Unit,
    navigateToSubscription: () -> Unit,
    navigateToPhotosIssues: (FolderId) -> Unit,
    navigateToPhotosUpsell: () -> Unit,
    navigateToBackupSettings: () -> Unit,
    navigateToNotificationPermissionRationale: () -> Unit,
    navigateToCreateNewAlbum: () -> Unit,
    navigateToAlbum: (AlbumId) -> Unit,
    navigateToUserInvitation: (Boolean) -> Unit,
    navigateToBlackFridayPromo: () -> Unit,
) = composable(
    route = Screen.PhotosAndAlbums.route,
    arguments = listOf(
        navArgument(Screen.PhotosAndAlbums.USER_ID) { type = NavType.StringType },
    ),
    deepLinks = listOf(
        navDeepLink { uriPattern = Screen.PhotosAndAlbums.deepLink(deepLinkBaseUrl) }
    )
) { navBackStackEntry ->
    navBackStackEntry.get<String>(Screen.Photos.USER_ID)?.let { _ ->
        PhotosAndAlbumsScreen(
            homeScaffoldState = homeScaffoldState,
            navigateToPhotosPermissionRationale = navigateToPhotosPermissionRationale,
            navigateToPhotosPreview = navigateToPhotosPreview,
            navigateToPhotosOptions = navigateToPhotosOptions,
            navigateToMultiplePhotosOptions = navigateToMultiplePhotosOptions,
            navigateToSubscription = navigateToSubscription,
            navigateToPhotosIssues = navigateToPhotosIssues,
            navigateToPhotosUpsell = navigateToPhotosUpsell,
            navigateToBackupSettings = navigateToBackupSettings,
            navigateToNotificationPermissionRationale = navigateToNotificationPermissionRationale,
            navigateToCreateNewAlbum = navigateToCreateNewAlbum,
            navigateToAlbum = navigateToAlbum,
            navigateToUserInvitation = navigateToUserInvitation,
            navigateToBlackFridayPromo = navigateToBlackFridayPromo,
        )
    } ?: let {
        val userId = UserId(requireNotNull(arguments.getString(Screen.PhotosAndAlbums.USER_ID)))
        navController.navigate(Screen.PhotosAndAlbums(userId)) {
            popUpTo(navController.graph.findStartDestination().id) {
                inclusive = true
            }
        }
    }
}

@ExperimentalCoroutinesApi
@ExperimentalAnimationApi
fun NavGraphBuilder.addComputers(
    navController: NavHostController,
    deepLinkBaseUrl: String,
    arguments: Bundle,
    homeScaffoldState: HomeScaffoldState,
    navigateToPreview: (linkId: FileId) -> Unit,
    navigateToSorting: (sorting: Sorting) -> Unit,
    navigateToFileOrFolderOptions: (linkId: LinkId) -> Unit,
    navigateToMultipleFileOrFolderOptions: (SelectionId) -> Unit,
    navigateToParentFolderOptions: (folderId: FolderId) -> Unit,
    navigateToComputerOptions: (deviceId: DeviceId) -> Unit,
    navigateToSubscription: () -> Unit,
    navigateToBlackFridayPromo: () -> Unit,
) = composable(
    route = Screen.Computers.route,
    enterTransition = defaultEnterSlideTransition {
        targetState.get<String>(Screen.Files.FOLDER_ID) != null &&
                initialState.get<String>(Screen.Files.FOLDER_ID) != targetState.get<String>(Screen.Files.FOLDER_ID)
    },
    exitTransition = defaultExitSlideTransition {
        initialState.get<String>(Screen.Files.FOLDER_ID) != targetState.get<String>(Screen.Files.FOLDER_ID)
    },
    popEnterTransition = defaultPopEnterSlideTransition {
        initialState.get<String>(Screen.Files.FOLDER_ID) != targetState.get<String>(Screen.Files.FOLDER_ID)
    },
    popExitTransition = defaultPopExitSlideTransition {
        initialState.get<String>(Screen.Files.FOLDER_ID) != null &&
                initialState.get<String>(Screen.Files.FOLDER_ID) != targetState.get<String>(Screen.Files.FOLDER_ID)
    },
    arguments = listOf(
        navArgument(Screen.Computers.USER_ID) { type = NavType.StringType },
        navArgument(Screen.Files.SHARE_ID) {
            type = NavType.StringType
            nullable = true
            defaultValue = null
        },
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
        navArgument(Screen.Computers.SYNCED_FOLDERS) {
            type = NavType.BoolType
            nullable = false
            defaultValue = false
        }
    ),
    deepLinks = listOf(
        navDeepLink { uriPattern = Screen.Computers.deepLink(deepLinkBaseUrl) }
    ),
) { navBackStackEntry ->
    navBackStackEntry.get<String>(Screen.Computers.USER_ID)?.let { userId ->
        val argShareId = navBackStackEntry.get<String>(Screen.Files.SHARE_ID)
        val argFolderId = navBackStackEntry.get<String>(Screen.Files.FOLDER_ID)
        val argSyncedFolders =
            navBackStackEntry.arguments?.getBoolean(Screen.Computers.SYNCED_FOLDERS, false) ?: false
        if (argShareId != null && argFolderId != null) {
            if (argSyncedFolders) {
                SyncedFoldersScreen(
                    homeScaffoldState = homeScaffoldState,
                    navigateToFiles = { folderId, folderName ->
                        navController.navigate(
                            Screen.Computers(
                                userId = UserId(userId),
                                folderId = folderId,
                                folderName = folderName,
                                syncedFolders = false
                            )
                        )
                    },
                    navigateToSortingDialog = navigateToSorting,
                    navigateBack = {
                        navController.popBackStack(
                            route = Screen.Computers.route,
                            inclusive = true,
                        )
                    },
                )
            } else {
                FilesScreen(
                    homeScaffoldState = homeScaffoldState,
                    navigateToFiles = { folderId, folderName ->
                        navController.navigate(
                            Screen.Computers(
                                UserId(userId),
                                folderId,
                                folderName,
                                false
                            )
                        )
                    },
                    navigateToPreview = navigateToPreview,
                    navigateToSortingDialog = navigateToSorting,
                    navigateToFileOrFolderOptions = navigateToFileOrFolderOptions,
                    navigateToMultipleFileOrFolderOptions = navigateToMultipleFileOrFolderOptions,
                    navigateToParentFolderOptions = navigateToParentFolderOptions,
                    navigateToSubscription = navigateToSubscription,
                    navigateToBlackFridayPromo = navigateToBlackFridayPromo,
                    navigateBack = { navController.popBackStack() },
                )
            }
        } else {
            ComputersScreen(
                homeScaffoldState = homeScaffoldState,
                navigateToSyncedFolders = { folderId, folderName ->
                    navController.navigate(
                        Screen.Computers(
                            UserId(userId),
                            folderId,
                            folderName,
                            true
                        )
                    )
                },
                navigateToComputerOptions = navigateToComputerOptions,
            )
        }
    } ?: let {
        val userId = UserId(requireNotNull(arguments.getString(Screen.Files.USER_ID)))
        val folderId = arguments.getString(Screen.Files.SHARE_ID)?.let { shareId ->
            arguments.getString(Screen.Files.FOLDER_ID)?.let { folderId ->
                FolderId(ShareId(userId, shareId), folderId)
            }
        }
        val folderName = arguments.getString(Screen.Files.FOLDER_NAME)
        navController.navigate(Screen.Computers(userId, folderId, folderName, false)) {
            popUpTo(navController.graph.findStartDestination().id) {
                inclusive = true
            }
        }
    }
}

@ExperimentalCoroutinesApi
@ExperimentalAnimationApi
fun NavGraphBuilder.addSharedTabs(
    navController: NavHostController,
    deepLinkBaseUrl: String,
    arguments: Bundle,
    homeScaffoldState: HomeScaffoldState,
    navigateToFolderPreview: (fileId: FileId) -> Unit,
    navigateToSinglePreview: (fileId: FileId) -> Unit,
    navigateToAlbum: (AlbumId) -> Unit,
    navigateToSorting: (sorting: Sorting) -> Unit,
    navigateToFileOrFolderOptions: (linkId: LinkId) -> Unit,
    navigateToMultipleFileOrFolderOptions: (SelectionId) -> Unit,
    navigateToParentFolderOptions: (folderId: FolderId) -> Unit,
    navigateToUserInvitation: (Boolean) -> Unit,
    navigateToSubscription: () -> Unit,
    navigateToBlackFridayPromo: () -> Unit,
) = composable(
    route = Screen.SharedTabs.route,
    enterTransition = defaultEnterSlideTransition {
        targetState.get<String>(Screen.Files.FOLDER_ID) != null &&
                initialState.get<String>(Screen.Files.FOLDER_ID) != targetState.get<String>(Screen.Files.FOLDER_ID)
    },
    exitTransition = defaultExitSlideTransition {
        initialState.get<String>(Screen.Files.FOLDER_ID) != targetState.get<String>(Screen.Files.FOLDER_ID)
    },
    popEnterTransition = defaultPopEnterSlideTransition {
        initialState.get<String>(Screen.Files.FOLDER_ID) != targetState.get<String>(Screen.Files.FOLDER_ID)
    },
    popExitTransition = defaultPopExitSlideTransition {
        initialState.get<String>(Screen.Files.FOLDER_ID) != null &&
                initialState.get<String>(Screen.Files.FOLDER_ID) != targetState.get<String>(Screen.Files.FOLDER_ID)
    },
    arguments = listOf(
        navArgument(Screen.SharedTabs.USER_ID) { type = NavType.StringType },
        navArgument(Screen.Files.SHARE_ID) {
            type = NavType.StringType
            nullable = true
            defaultValue = null
        },
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
    deepLinks = listOf(
        navDeepLink { uriPattern = Screen.SharedTabs.deepLink(deepLinkBaseUrl) }
    ),
) { navBackStackEntry ->
    navBackStackEntry.get<String>(Screen.SharedTabs.USER_ID)?.let { userId ->
        val argShareId = navBackStackEntry.get<String>(Screen.Files.SHARE_ID)
        val argFolderId = navBackStackEntry.get<String>(Screen.Files.FOLDER_ID)
        if (argShareId != null && argFolderId != null) {
            FilesScreen(
                homeScaffoldState = homeScaffoldState,
                navigateToFiles = { folderId, folderName ->
                    navController.navigate(
                        Screen.SharedTabs(
                            UserId(userId),
                            folderId,
                            folderName,
                        )
                    )
                },
                navigateToPreview = navigateToFolderPreview,
                navigateToSortingDialog = navigateToSorting,
                navigateToFileOrFolderOptions = navigateToFileOrFolderOptions,
                navigateToMultipleFileOrFolderOptions = navigateToMultipleFileOrFolderOptions,
                navigateToParentFolderOptions = navigateToParentFolderOptions,
                navigateToSubscription = navigateToSubscription,
                navigateToBlackFridayPromo = navigateToBlackFridayPromo,
                navigateBack = { navController.popBackStack() },
            )
        } else {
            SharedTabsScreen(
                homeScaffoldState = homeScaffoldState,
                navigateToFiles = { folderId, folderName ->
                    navController.navigate(
                        Screen.SharedTabs(
                            UserId(userId),
                            folderId,
                            folderName,
                        )
                    )
                },
                navigateToPreview = navigateToSinglePreview,
                navigateToFileOrFolderOptions = navigateToFileOrFolderOptions,
                navigateToAlbum = navigateToAlbum,
                navigateToUserInvitation = navigateToUserInvitation,
            )
        }
    } ?: let {
        val userId = UserId(requireNotNull(arguments.getString(Screen.SharedTabs.USER_ID)))
        val folderId = arguments.getString(Screen.Files.SHARE_ID)?.let { shareId ->
            arguments.getString(Screen.Files.FOLDER_ID)?.let { folderId ->
                FolderId(ShareId(userId, shareId), folderId)
            }
        }
        val folderName = arguments.getString(Screen.Files.FOLDER_NAME)
        navController.navigate(Screen.SharedTabs(userId, folderId, folderName)) {
            popUpTo(navController.graph.findStartDestination().id) {
                inclusive = true
            }
        }
    }
}
