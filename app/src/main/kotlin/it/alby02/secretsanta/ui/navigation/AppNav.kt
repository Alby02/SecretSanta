/*
 * Licensed under the EUPL, Version 1.2.
 * You may obtain a copy of the Licence at:
 * https://joinup.ec.europa.eu/collection/eupl/eupl-text-eupl-12
 */

package it.alby02.secretsanta.ui.navigation

import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.ui.NavDisplay
import it.alby02.secretsanta.ui.features.groupdetail.GroupDetailRoute
import it.alby02.secretsanta.ui.features.home.HomeRoute
import it.alby02.secretsanta.ui.features.login.LoginRoute
import it.alby02.secretsanta.ui.features.signup.SignUpRoute
import kotlinx.serialization.Serializable

@Serializable
data object Login : NavKey

@Serializable
data object SignUp : NavKey

@Serializable
data object Home : NavKey

@Serializable
data class GroupDetail(val groupId: String) : NavKey

@Composable
fun AppNavHost() {
    val navigationState = rememberNavigationState(
        startRoute = Login,
        topLevelRoutes = setOf(Login, Home)
    )
    val navigator = remember(navigationState) { Navigator(navigationState) }

    val entryProvider = entryProvider {
        entry<Login> {
            LoginRoute(
                onLoginSuccess = { navigator.navigate(Home) },
                onNavigateToSignUp = { navigator.navigate(SignUp) }
            )
        }
        entry<SignUp> {
            SignUpRoute(
                onSignUpSuccess = { navigator.navigate(Home) },
                onNavigateToLogin = { navigator.goBack() }
            )
        }
        entry<Home> {
            HomeRoute(
                onNavigateToGroup = { groupId -> navigator.navigate(GroupDetail(groupId)) },
                onLogout = { navigator.navigate(Login) }
            )
        }
        entry<GroupDetail> { key ->
            GroupDetailRoute(
                groupId = key.groupId,
                onNavigateBack = { navigator.goBack() }
            )
        }
    }

    NavDisplay(
        entries = navigationState.toEntries(entryProvider),
        onBack = { navigator.goBack() },
        transitionSpec = {
            slideInHorizontally { it } togetherWith slideOutHorizontally { -it }
        }
    )
}