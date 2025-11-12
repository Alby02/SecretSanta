/*
 * Licensed under the EUPL, Version 1.2.
 * You may obtain a copy of the Licence at:
 * https://joinup.ec.europa.eu/collection/eupl/eupl-text-eupl-12
 */

package it.alby02.secretsanta.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import it.alby02.secretsanta.ui.features.groupdetail.GroupDetailScreen
import it.alby02.secretsanta.ui.features.home.HomeScreen

// Defines the routes
object AppDestinations {
    const val HOME = "home"
    const val GROUP_DETAIL = "group_detail"
    const val GROUP_ID_ARG = "groupId"
}

@Composable
fun AppNavigation() {
    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = AppDestinations.HOME) {
        // Home Screen (Your "First Fragment")
        composable(AppDestinations.HOME) {
            HomeScreen(
                onNavigateToGroup = { groupId ->
                    navController.navigate("${AppDestinations.GROUP_DETAIL}/$groupId")
                }
            )
        }

        // Group Detail Screen (Your "Second Fragment")
        composable(
            route = "${AppDestinations.GROUP_DETAIL}/{${AppDestinations.GROUP_ID_ARG}}",
            arguments = listOf(navArgument(AppDestinations.GROUP_ID_ARG) { type = NavType.StringType })
        ) { backStackEntry ->
            val groupId = backStackEntry.arguments?.getString(AppDestinations.GROUP_ID_ARG)
            requireNotNull(groupId) { "groupId argument is missing" }

            GroupDetailScreen(
                groupId = groupId,
                onNavigateBack = { navController.popBackStack() }
            )
        }
    }
}