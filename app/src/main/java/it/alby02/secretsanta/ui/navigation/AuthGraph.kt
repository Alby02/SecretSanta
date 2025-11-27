/*
 * Licensed under the EUPL, Version 1.2.
 * You may obtain a copy of the Licence at:
 * https://joinup.ec.europa.eu/collection/eupl/eupl-text-eupl-12
 */

package it.alby02.secretsanta.ui.navigation

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import androidx.navigation.navigation
import it.alby02.secretsanta.ui.features.login.LoginRoute
import it.alby02.secretsanta.ui.features.signup.SignUpRoute

object AuthDestinations {
    const val AUTH_GRAPH = "auth_graph"
    const val LOGIN_ROUTE = "login"
    const val SIGNUP_ROUTE = "signup"
}

fun NavGraphBuilder.authGraph(navController: NavController, onAccessGranted: () -> Unit) {
    navigation(
        startDestination = AuthDestinations.LOGIN_ROUTE,
        route = AuthDestinations.AUTH_GRAPH
    ) {
        composable(AuthDestinations.LOGIN_ROUTE) {
            LoginRoute(
                onLoginSuccess = onAccessGranted,
                onNavigateToSignUp = {
                    navController.navigate(AuthDestinations.SIGNUP_ROUTE)
                }
            )
        }
        composable(AuthDestinations.SIGNUP_ROUTE) {
            SignUpRoute(
                onSignUpSuccess = onAccessGranted,
                onNavigateToLogin = {
                    navController.popBackStack()
                }
            )
        }
    }
}
