/*
 * Licensed under the EUPL, Version 1.2.
 * You may obtain a copy of the Licence at:
 * https://joinup.ec.europa.eu/collection/eupl/eupl-text-eupl-12
 */

package it.alby02.secretsanta.ui.navigation

import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import androidx.navigation.navigation
import it.alby02.secretsanta.ui.features.groupdetail.GroupDetailScreen
import it.alby02.secretsanta.ui.features.home.HomeScreen

object HomeDestinations {

    const val HOME_GRAPH = "home_graph"
    const val HOME_ROUTE = "home"
    object Group {
        private const val ROUTE_PREFIX = "group"
        const val ARG_GROUP_ID = "groupId"

        const val ROUTE = "$ROUTE_PREFIX/{$ARG_GROUP_ID}"

        val arguments = listOf(
            navArgument(ARG_GROUP_ID) { type = NavType.StringType }
        )

        fun createRoute(groupId: String) = "$ROUTE_PREFIX/$groupId"
    }
}

fun NavGraphBuilder.homeGraph(navController: NavHostController, onLogout: () -> Unit) {
    navigation(startDestination = HomeDestinations.HOME_ROUTE, route = HomeDestinations.HOME_GRAPH) {
        composable(HomeDestinations.HOME_ROUTE) {
            HomeScreen(
                onNavigateToGroup = { groupId ->
                    navController.navigate(HomeDestinations.Group.createRoute(groupId))
                },
                onLogout = onLogout
            )
        }
        composable(
            route = HomeDestinations.Group.ROUTE,
            arguments = HomeDestinations.Group.arguments
        ) {
            GroupDetailScreen ( onNavigateBack = {
                navController.popBackStack()
            })
        }
    }
}