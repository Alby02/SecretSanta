/*
 * Licensed under the EUPL, Version 1.2.
 * You may obtain a copy of the Licence at:
 * https://joinup.ec.europa.eu/collection/eupl/eupl-text-eupl-12
 */

package it.alby02.secretsanta.ui.navigation

import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import it.alby02.secretsanta.ui.features.login.LoginRoute
import it.alby02.secretsanta.ui.features.signup.SignUpRoute

fun EntryProviderScope<NavKey>.authGraph(navigator: Navigator) {
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
}
