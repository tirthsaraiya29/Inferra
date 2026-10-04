package com.inferra.ui.navigation

import java.net.URLEncoder

sealed class Screen(val route: String) {
    object Discovery : Screen("discovery")
    object Search : Screen("search") {
        fun createRoute(query: String): String {
            val encoded = try { URLEncoder.encode(query, "UTF-8") } catch (_: Exception) { query }
            return "search?q=$encoded"
        }
    }
    object ModelDetail : Screen("model_detail?id={id}") {
        fun createRoute(modelId: String): String {
            val encoded = try { URLEncoder.encode(modelId, "UTF-8") } catch (_: Exception) { modelId }
            return "model_detail?id=$encoded"
        }
    }
    object Datasets : Screen("datasets")
    object DatasetDetail : Screen("dataset_detail?id={id}") {
        fun createRoute(datasetId: String): String {
            val encoded = try { URLEncoder.encode(datasetId, "UTF-8") } catch (_: Exception) { datasetId }
            return "dataset_detail?id=$encoded"
        }
    }
    object Compare : Screen("compare")
    object Hardware : Screen("hardware")
    object Downloads : Screen("downloads")
    object Watchlist : Screen("watchlist")
    object Settings : Screen("settings")
}
