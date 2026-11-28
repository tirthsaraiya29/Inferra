package com.inferra.ui.navigation

import com.inferra.domain.model.RepoType
import java.net.URLEncoder

sealed class Screen(val route: String) {
    object Discovery : Screen("discovery")
    object Models : Screen("models")
    object Datasets : Screen("datasets")
    object Spaces : Screen("spaces")

    object Search : Screen("search?q={q}&type={type}") {
        fun createRoute(query: String = "", type: RepoType = RepoType.MODEL): String {
            val encodedQ = try { URLEncoder.encode(query, "UTF-8") } catch (_: Exception) { query }
            return "search?q=$encodedQ&type=${type.name}"
        }
    }

    object RepoDetail : Screen("repo_detail?id={id}&type={type}") {
        fun createRoute(repoId: String, type: RepoType): String {
            val encodedId = try { URLEncoder.encode(repoId, "UTF-8") } catch (_: Exception) { repoId }
            return "repo_detail?id=$encodedId&type=${type.name}"
        }
    }

    object UserProfile : Screen("profile?user={user}") {
        fun createRoute(username: String): String {
            val encodedUser = try { URLEncoder.encode(username, "UTF-8") } catch (_: Exception) { username }
            return "profile?user=$encodedUser"
        }
    }

    object Downloads : Screen("downloads")
    object Bookmarks : Screen("bookmarks")
    object AccountSettings : Screen("account_settings")
}
