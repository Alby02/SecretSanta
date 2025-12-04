/*
 * Licensed under the EUPL, Version 1.2.
 * You may obtain a copy of the Licence at:
 * https://joinup.ec.europa.eu/collection/eupl/eupl-text-eupl-12
 */

package it.alby02.secretsanta.ui.features.home

import androidx.compose.runtime.Composable

@Composable
fun HomeRoute(
    onNavigateToGroup: (String) -> Unit,
    onLogout: () -> Unit,
) {
    HomeScreen(
        onNavigateToGroup = onNavigateToGroup,
        onLogout = onLogout
    )
}
