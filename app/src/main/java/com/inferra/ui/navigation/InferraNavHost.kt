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
import com.inferra.data.repository.DownloadRepository
import com.inferra.data.repository.HubRepository
import com.inferra.data.repository.SettingsRepository
import com.inferra.domain.model.RepoType
import com.inferra.ui.screens.account.AccountSettingsScreen
import com.inferra.ui.screens.account.AccountSettingsViewModel
import com.inferra.ui.screens.bookmarks.BookmarksScreen
import com.inferra.ui.screens.bookmarks.BookmarksViewModel
import com.inferra.ui.screens.detail.RepoDetailScreen
import com.inferra.ui.screens.detail.RepoDetailViewModel
import com.inferra.ui.screens.discovery.DiscoveryScreen
import com.inferra.ui.screens.discovery.DiscoveryViewModel
import com.inferra.ui.screens.downloads.DownloadsScreen
import com.inferra.ui.screens.downloads.DownloadsViewModel
import com.inferra.ui.screens.repo.RepoListScreen
import com.inferra.ui.screens.search.SearchScreen
import com.inferra.ui.screens.search.SearchViewModel
import java.net.URLDecoder

@Composable
fun InferraNavHost(
    navController: NavHostController = rememberNavController(),
) {
    val context = LocalContext.current
    val db = remember { AppDatabase.getDatabase(context) }

    // Repositories
    val settingsRepository = remember { SettingsRepository(context) }
    val hubRepository = remember {
        HubRepository(
            api = HuggingFaceClient.api,
            cachedRepoDao = db.cachedRepoDao(),
            bookmarkDao = db.bookmarkDao(),
            context = context
        )
    }
    val downloadRepository = remember { DownloadRepository(db.downloadJobDao(), context) }

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

    // Shared ViewModels
    val discoveryViewModel = remember { DiscoveryViewModel(hubRepository) }
    val searchViewModel = remember { SearchViewModel(hubRepository) }
    val downloadsViewModel = remember { DownloadsViewModel(downloadRepository) }
    val bookmarksViewModel = remember { BookmarksViewModel(hubRepository) }
    val accountSettingsViewModel = remember { AccountSettingsViewModel(hubRepository, settingsRepository) }

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    val isDetailRoute = currentRoute?.startsWith("repo_detail") == true

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
                    onNavigateToRepo = { repoId, type -> navController.navigate(Screen.RepoDetail.createRoute(repoId, type)) },
                    onNavigateToSearch = { query, type -> navController.navigate(Screen.Search.createRoute(query, type)) },
                    onNavigateToType = { type ->
                        when (type) {
                            RepoType.MODEL -> navController.navigate(Screen.Models.route)
                            RepoType.DATASET -> navController.navigate(Screen.Datasets.route)
                            RepoType.SPACE -> navController.navigate(Screen.Spaces.route)
                        }
                    }
                )
            }

            composable(Screen.Models.route) {
                RepoListScreen(
                    repoType = RepoType.MODEL,
                    viewModel = searchViewModel,
                    onNavigateToRepo = { repoId, type -> navController.navigate(Screen.RepoDetail.createRoute(repoId, type)) }
                )
            }

            composable(Screen.Datasets.route) {
                RepoListScreen(
                    repoType = RepoType.DATASET,
                    viewModel = searchViewModel,
                    onNavigateToRepo = { repoId, type -> navController.navigate(Screen.RepoDetail.createRoute(repoId, type)) }
                )
            }

            composable(Screen.Spaces.route) {
                RepoListScreen(
                    repoType = RepoType.SPACE,
                    viewModel = searchViewModel,
                    onNavigateToRepo = { repoId, type -> navController.navigate(Screen.RepoDetail.createRoute(repoId, type)) }
                )
            }

            composable(
                route = "search?q={q}&type={type}",
                arguments = listOf(
                    navArgument("q") { type = NavType.StringType; defaultValue = "" },
                    navArgument("type") { type = NavType.StringType; defaultValue = "MODEL" }
                )
            ) { backStack ->
                val rawQuery = backStack.arguments?.getString("q") ?: ""
                val rawType = backStack.arguments?.getString("type") ?: "MODEL"
                val query = try { URLDecoder.decode(rawQuery, "UTF-8") } catch (_: Exception) { rawQuery }
                val repoType = try { RepoType.valueOf(rawType) } catch (_: Exception) { RepoType.MODEL }

                SearchScreen(
                    viewModel = searchViewModel,
                    initialQuery = query,
                    initialType = repoType,
                    onNavigateToRepo = { repoId, type -> navController.navigate(Screen.RepoDetail.createRoute(repoId, type)) }
                )
            }

            composable(Screen.Search.route) {
                SearchScreen(
                    viewModel = searchViewModel,
                    onNavigateToRepo = { repoId, type -> navController.navigate(Screen.RepoDetail.createRoute(repoId, type)) }
                )
            }

            composable(
                route = "repo_detail?id={id}&type={type}",
                arguments = listOf(
                    navArgument("id") { type = NavType.StringType; defaultValue = "" },
                    navArgument("type") { type = NavType.StringType; defaultValue = "MODEL" }
                )
            ) { backStack ->
                val rawRepoId = backStack.arguments?.getString("id") ?: ""
                val rawType = backStack.arguments?.getString("type") ?: "MODEL"
                val repoId = try { URLDecoder.decode(rawRepoId, "UTF-8") } catch (_: Exception) { rawRepoId }
                val repoType = try { RepoType.valueOf(rawType) } catch (_: Exception) { RepoType.MODEL }

                val detailViewModel = remember(repoId, repoType) {
                    RepoDetailViewModel(
                        repoId = repoId,
                        repoType = repoType,
                        hubRepository = hubRepository,
                        downloadRepository = downloadRepository
                    )
                }

                RepoDetailScreen(
                    viewModel = detailViewModel,
                    onBackClick = { navController.popBackStack() },
                    onLaunchUrl = launchUrl
                )
            }

            composable(Screen.Downloads.route) {
                DownloadsScreen(
                    viewModel = downloadsViewModel
                )
            }

            composable(Screen.Bookmarks.route) {
                BookmarksScreen(
                    viewModel = bookmarksViewModel,
                    onNavigateToRepo = { repoId, type -> navController.navigate(Screen.RepoDetail.createRoute(repoId, type)) }
                )
            }

            composable(Screen.AccountSettings.route) {
                AccountSettingsScreen(
                    viewModel = accountSettingsViewModel
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
                        navController.navigate(route) {
                            popUpTo(startId) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                }
            )
        }
    }
}
