/*
 * Licensed under the EUPL, Version 1.2.
 * You may obtain a copy of the Licence at:
 * https://joinup.ec.europa.eu/collection/eupl/eupl-text-eupl-12
 */

package it.alby02.secretsanta.ui.features.groupdetail

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.koin.androidx.compose.koinViewModel
import org.koin.core.parameter.parametersOf

@Composable
fun GroupDetailRoute(
    groupId: String,
    onNavigateBack: () -> Unit,
    viewModel: GroupDetailViewModel = koinViewModel { parametersOf(groupId) }
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    GroupDetailScreen(
        onNavigateBack = onNavigateBack,
        uiState = uiState,
        startMatching = viewModel::startMatching,
        viewAssignment = viewModel::viewAssignment,
        initiateRecovery = viewModel::initiateRecovery,
        contributeToRecovery = viewModel::contributeToRecovery,
        finalizeRecovery = viewModel::finalizeRecovery
    )
}
