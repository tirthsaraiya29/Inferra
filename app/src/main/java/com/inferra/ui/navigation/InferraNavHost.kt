package com.inferra.ui.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.inferra.data.local.AppDatabase
import com.inferra.data.network.HuggingFaceClient
import com.inferra.data.repository.CompanionRepository
import com.inferra.data.repository.DownloadRepository
import com.inferra.data.repository.HardwareRepository
import com.inferra.data.repository.ModelDownloader
import com.inferra.data.repository.ModelRepository
import com.inferra.data.repository.QuantDiscoveryRepository
import com.inferra.ui.screens.compare.CompareScreen
import com.inferra.ui.screens.compare.CompareViewModel
import com.inferra.ui.screens.detail.ModelDetailScreen
import com.inferra.ui.screens.detail.ModelDetailViewModel
import com.inferra.ui.screens.discovery.DiscoveryScreen
import com.inferra.ui.screens.discovery.DiscoveryViewModel
import com.inferra.ui.screens.downloads.DownloadsScreen
import com.inferra.ui.screens.downloads.DownloadsViewModel
import com.inferra.ui.screens.hardware.HardwareScreen
import com.inferra.ui.screens.hardware.HardwareViewModel
import com.inferra.ui.screens.search.SearchScreen
import com.inferra.ui.screens.search.SearchViewModel
import com.inferra.ui.screens.settings.SettingsScreen
import com.inferra.ui.screens.settings.SettingsViewModel
import com.inferra.ui.screens.watchlist.WatchlistScreen
import com.inferra.ui.screens.watchlist.WatchlistViewModel
import java.net.URLDecoder

@Composable
fun InferraNavHost(
    navController: NavHostController = rememberNavController()
) {
    val context = LocalContext.current
    val db = remember { AppDatabase.getDatabase(context) }

    // Repositories
    val modelRepository = remember { ModelRepository(HuggingFaceClient.api, db.modelDao(), db.watchlistDao()) }
    val hardwareRepository = remember { HardwareRepository(context, db.hardwareProfileDao()) }
    val downloadRepository = remember { DownloadRepository(db.downloadJobDao()) }
    val companionRepository = remember { CompanionRepository(db.deviceTargetDao()) }
    val quantDiscoveryRepository = remember { QuantDiscoveryRepository(HuggingFaceClient.api) }
    val modelDownloader = remember { ModelDownloader(context) }

    // Shared ViewModels
    val discoveryViewModel = remember { DiscoveryViewModel(modelRepository, hardwareRepository) }
    val searchViewModel = remember { SearchViewModel(modelRepository, hardwareRepository) }
    val compareViewModel = remember { CompareViewModel(modelRepository, hardwareRepository) }
    val hardwareViewModel = remember { HardwareViewModel(hardwareRepository) }
    val downloadsViewModel = remember { DownloadsViewModel(downloadRepository, companionRepository) }
    val watchlistViewModel = remember { WatchlistViewModel(modelRepository, hardwareRepository) }
    val settingsViewModel = remember { SettingsViewModel() }

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    val isDetailRoute = currentRoute?.startsWith("model_detail") == true

    Box(modifier = Modifier.fillMaxSize()) {
        NavHost(
            navController = navController,
            startDestination = Screen.Discovery.route,
            modifier = Modifier.fillMaxSize()
        ) {
            composable(Screen.Discovery.route) {
                DiscoveryScreen(
                    viewModel = discoveryViewModel,
                    onNavigateToModel = { modelId -> navController.navigate(Screen.ModelDetail.createRoute(modelId)) },
                    onNavigateToSearch = { query -> navController.navigate(Screen.Search.createRoute(query)) },
                    onNavigateToHardware = { navController.navigate(Screen.Hardware.route) }
                )
            }

            composable(
                route = "search?q={q}",
                arguments = listOf(navArgument("q") { type = NavType.StringType; defaultValue = "" })
            ) { backStack ->
                val rawQuery = backStack.arguments?.getString("q") ?: ""
                val query = try { URLDecoder.decode(rawQuery, "UTF-8") } catch (_: Exception) { rawQuery }
                SearchScreen(
                    viewModel = searchViewModel,
                    initialQuery = query,
                    onNavigateToModel = { modelId -> navController.navigate(Screen.ModelDetail.createRoute(modelId)) }
                )
            }

            composable(Screen.Search.route) {
                SearchScreen(
                    viewModel = searchViewModel,
                    onNavigateToModel = { modelId -> navController.navigate(Screen.ModelDetail.createRoute(modelId)) }
                )
            }

            composable(
                route = "model_detail?id={id}",
                arguments = listOf(navArgument("id") { type = NavType.StringType; defaultValue = "" })
            ) { backStack ->
                val rawModelId = backStack.arguments?.getString("id") ?: ""
                val modelId = try { URLDecoder.decode(rawModelId, "UTF-8") } catch (_: Exception) { rawModelId }

                val detailViewModel = remember(modelId) {
                    ModelDetailViewModel(
                        modelId = modelId,
                        modelRepository = modelRepository,
                        hardwareRepository = hardwareRepository,
                        downloadRepository = downloadRepository,
                        companionRepository = companionRepository,
                        quantDiscoveryRepository = quantDiscoveryRepository,
                        modelDownloader = modelDownloader
                    )
                }

                ModelDetailScreen(
                    viewModel = detailViewModel,
                    onBack = { navController.popBackStack() },
                    onNavigateToModel = { newModelId -> navController.navigate(Screen.ModelDetail.createRoute(newModelId)) }
                )
            }

            composable(Screen.Compare.route) {
                CompareScreen(
                    viewModel = compareViewModel,
                    onNavigateToModel = { modelId -> navController.navigate(Screen.ModelDetail.createRoute(modelId)) }
                )
            }

            composable(Screen.Hardware.route) {
                HardwareScreen(
                    viewModel = hardwareViewModel
                )
            }

            composable(Screen.Downloads.route) {
                DownloadsScreen(
                    viewModel = downloadsViewModel
                )
            }

            composable(Screen.Watchlist.route) {
                WatchlistScreen(
                    viewModel = watchlistViewModel,
                    onNavigateToModel = { modelId -> navController.navigate(Screen.ModelDetail.createRoute(modelId)) }
                )
            }

            composable(Screen.Settings.route) {
                SettingsScreen(
                    viewModel = settingsViewModel
                )
            }
        }

        if (!isDetailRoute) {
            GlassBottomBar(
                currentRoute = currentRoute?.split("?")?.get(0),
                onNavigate = { route ->
                    val cleanCurrent = currentRoute?.split("?")?.get(0)
                    if (cleanCurrent != route) {
                        navController.navigate(route) {
                            popUpTo(navController.graph.startDestinationId) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                },
                modifier = Modifier.align(Alignment.BottomCenter)
            )
        }
    }
}
