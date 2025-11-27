/*
 * Licensed under the EUPL, Version 1.2.
 * You may obtain a copy of the Licence at:
 * https://joinup.ec.europa.eu/collection/eupl/eupl-text-eupl-12
 */

package it.alby02.secretsanta.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.rememberNavController

@Composable
fun AppNavHost() {
    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = AuthDestinations.AUTH_GRAPH) {
        authGraph(navController = navController){
            navController.navigate(HomeDestinations.HOME_GRAPH) {
                popUpTo(AuthDestinations.AUTH_GRAPH) { inclusive = true }
            }
        }

        homeGraph(navController){
            navController.navigate(AuthDestinations.AUTH_GRAPH) {
                popUpTo(HomeDestinations.HOME_GRAPH) { inclusive = true }
            }
        }
    }
}