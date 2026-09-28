package com.inferra.ui.navigation

import java.net.URLEncoder

sealed class Screen(val route: String) {
    object Discovery : Screen("discovery")
    object Search : Screen("search")
    object ModelDetail : Screen("model_detail/{modelId}") {
        fun createRoute(modelId: String): String = "model_detail/${URLEncoder.encode(modelId, "UTF-8")}"
    }
    object Compare : Screen("compare")
    object Hardware : Screen("hardware")
    object Downloads : Screen("downloads")
    object Watchlist : Screen("watchlist")
    object Settings : Screen("settings")
}
