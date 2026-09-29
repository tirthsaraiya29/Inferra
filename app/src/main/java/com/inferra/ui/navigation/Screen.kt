package com.inferra.ui.navigation

import java.net.URLEncoder

sealed class Screen(val route: String) {
    object Discovery : Screen("discovery")
    object Search : Screen("search")
    object ModelDetail : Screen("model_detail?id={modelId}") {
        fun createRoute(modelId: String): String = "model_detail?id=${URLEncoder.encode(modelId, "UTF-8")}"
    }
    object Compare : Screen("compare")
    object Hardware : Screen("hardware")
    object Downloads : Screen("downloads")
    object Watchlist : Screen("watchlist")
    object Settings : Screen("settings")
}
