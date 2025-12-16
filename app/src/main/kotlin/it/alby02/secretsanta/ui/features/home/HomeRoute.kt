/*
 * Licensed under the EUPL, Version 1.2.
 * You may obtain a copy of the Licence at:
 * https://joinup.ec.europa.eu/collection/eupl/eupl-text-eupl-12
 */

package it.alby02.secretsanta.ui.features.home

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.koin.androidx.compose.koinViewModel

@Composable
fun HomeRoute(
    onNavigateToGroup: (String) -> Unit,
    onLogout: () -> Unit,
    viewModel: HomeViewModel = koinViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    HomeScreen(
        onNavigateToGroup = onNavigateToGroup,
        onLogout = onLogout,
        uiState = uiState,
        onCreateGroupClicked = viewModel::onCreateGroupClicked,
        onJoinGroupClicked = viewModel::onJoinGroupClicked,
        createGroup = viewModel::createGroup,
        joinGroup = viewModel::joinGroup,
        onCreateDialogDismiss = viewModel::onCreateDialogDismiss,
        onJoinDialogDismiss = viewModel::onJoinDialogDismiss,
        onFabClicked = viewModel::onFabClicked
    )
}
