package com.inferra.ui.navigation

import android.content.ActivityNotFoundException
import android.content.Intent
import android.widget.Toast
import androidx.browser.customtabs.CustomTabsIntent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.core.net.toUri
import androidx.navigation.NavGraph.Companion.findStartDestination
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
import com.inferra.data.repository.DatasetRepository
import com.inferra.data.repository.DownloadRepository
import com.inferra.data.repository.HardwareRepository
import com.inferra.data.repository.ModelRepository
import com.inferra.data.repository.ProviderRepository
import com.inferra.data.repository.SettingsRepository
import com.inferra.ui.screens.compare.CompareScreen
import com.inferra.ui.screens.compare.CompareViewModel
import com.inferra.ui.screens.datasets.DatasetDetailScreen
import com.inferra.ui.screens.datasets.DatasetDetailViewModel
import com.inferra.ui.screens.datasets.DatasetsScreen
import com.inferra.ui.screens.datasets.DatasetsViewModel
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
    navController: NavHostController = rememberNavController(),
) {
    val context = LocalContext.current
    val db = remember { AppDatabase.getDatabase(context) }

    // Repositories
    val settingsRepository = remember { SettingsRepository(context) }
    val modelRepository = remember {
        ModelRepository(
            api = HuggingFaceClient.api,
            modelDao = db.modelDao(),
            androidModelDao = db.androidModelDao(),
            watchlistDao = db.watchlistDao(),
            settingsRepository = settingsRepository
        )
    }
    val datasetRepository = remember {
        DatasetRepository(
            api = HuggingFaceClient.datasetApi,
            datasetDao = db.datasetDao(),
            settingsRepository = settingsRepository
        )
    }
    val hardwareRepository = remember { HardwareRepository(context, db.hardwareProfileDao()) }
    val downloadRepository = remember { DownloadRepository(db.downloadJobDao()) }
    val companionRepository = remember { CompanionRepository(db.deviceTargetDao()) }
    val providerRepository = remember { ProviderRepository(db.providerDao()) }

    val launchUrl: (String) -> Unit = remember(context) {
        { url ->
            try {
                val customTabsIntent = CustomTabsIntent.Builder().build()
                customTabsIntent.launchUrl(context, url.toUri())
            } catch (_: ActivityNotFoundException) {
                try {
                    val fallbackIntent = Intent(Intent.ACTION_VIEW, url.toUri())
                    context.startActivity(fallbackIntent)
                } catch (_: Exception) {
                    Toast.makeText(context, "Unable to open browser for $url", Toast.LENGTH_SHORT).show()
                }
            } catch (_: SecurityException) {
                Toast.makeText(context, "Permission error launching URL", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Toast.makeText(context, "Failed to launch link: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // Seed default providers on launch
    LaunchedEffect(Unit) {
        providerRepository.seedDefaultProviders()
    }

    // Shared ViewModels
    val discoveryViewModel = remember { DiscoveryViewModel(modelRepository) }
    val searchViewModel = remember { SearchViewModel(modelRepository, hardwareRepository) }
    val datasetsViewModel = remember { DatasetsViewModel(datasetRepository) }
    val compareViewModel = remember { CompareViewModel(modelRepository) }
    val hardwareViewModel = remember { HardwareViewModel(hardwareRepository) }
    val downloadsViewModel = remember { DownloadsViewModel(downloadRepository, companionRepository, db.localModelDao()) }
    val watchlistViewModel = remember { WatchlistViewModel(modelRepository, hardwareRepository) }
    val settingsViewModel = remember { SettingsViewModel(settingsRepository) }

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    val isDetailRoute = (currentRoute?.startsWith("model_detail") == true) || (currentRoute?.startsWith("dataset_detail") == true)

    Box(modifier = Modifier.fillMaxSize()) {
        NavHost(
            navController = navController,
            startDestination = Screen.Discovery.route,
            enterTransition = { fadeIn(animationSpec = tween(220)) + slideInHorizontally(animationSpec = tween(220)) { it / 6 } },
            exitTransition = { fadeOut(animationSpec = tween(180)) + slideOutHorizontally(animationSpec = tween(180)) { -it / 6 } },
            popEnterTransition = { fadeIn(animationSpec = tween(220)) + slideInHorizontally(animationSpec = tween(220)) { -it / 6 } },
            popExitTransition = { fadeOut(animationSpec = tween(180)) + slideOutHorizontally(animationSpec = tween(180)) { it / 6 } },
            modifier = Modifier.fillMaxSize()
        ) {
            composable(Screen.Discovery.route) {
                DiscoveryScreen(
                    viewModel = discoveryViewModel,
                    onNavigateToModel = { modelId -> navController.navigate(Screen.ModelDetail.createRoute(modelId)) },
                    onNavigateToSearch = { query -> navController.navigate(Screen.Search.createRoute(query)) },
                    onNavigateToHardware = { navController.navigate(Screen.Hardware.route) },
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

            composable(Screen.Datasets.route) {
                DatasetsScreen(
                    viewModel = datasetsViewModel,
                    onNavigateToDataset = { datasetId -> navController.navigate(Screen.DatasetDetail.createRoute(datasetId)) }
                )
            }

            composable(
                route = "dataset_detail?id={id}",
                arguments = listOf(navArgument("id") { type = NavType.StringType; defaultValue = "" })
            ) { backStack ->
                val rawDatasetId = backStack.arguments?.getString("id") ?: ""
                val datasetId = try { URLDecoder.decode(rawDatasetId, "UTF-8") } catch (_: Exception) { rawDatasetId }

                val detailViewModel = remember(datasetId) {
                    DatasetDetailViewModel(
                        datasetId = datasetId,
                        datasetRepository = datasetRepository
                    )
                }

                DatasetDetailScreen(
                    viewModel = detailViewModel,
                    onBackClick = { navController.popBackStack() },
                    onLaunchUrl = launchUrl
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
                        modelRepository = modelRepository
                    )
                }

                ModelDetailScreen(
                    viewModel = detailViewModel,
                    onBackClick = { navController.popBackStack() },
                    onLaunchUrl = launchUrl
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

        AnimatedVisibility(
            visible = !isDetailRoute,
            enter = slideInVertically(animationSpec = tween(300)) { it },
            exit = slideOutVertically(animationSpec = tween(300)) { it },
            modifier = Modifier.align(Alignment.BottomCenter)
        ) {
            InferraBottomBar(
                currentRoute = currentRoute?.split("?")?.get(0),
                onNavigate = { route ->
                    val cleanCurrent = currentRoute?.split("?")?.get(0)
                    if (cleanCurrent != route) {
                        val startId = navController.graph.findStartDestination().id
                        if (route == Screen.Discovery.route) {
                            navController.navigate(route) {
                                popUpTo(startId) {
                                    inclusive = false
                                }
                                launchSingleTop = true
                            }
                        } else {
                            navController.navigate(route) {
                                popUpTo(startId) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    }
                }
            )
        }
    }
}
