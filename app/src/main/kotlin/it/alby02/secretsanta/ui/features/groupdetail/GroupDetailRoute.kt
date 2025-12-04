/*
 * Licensed under the EUPL, Version 1.2.
 * You may obtain a copy of the Licence at:
 * https://joinup.ec.europa.eu/collection/eupl/eupl-text-eupl-12
 */

package it.alby02.secretsanta.ui.features.groupdetail

import androidx.compose.runtime.Composable

@Composable
fun GroupDetailRoute(
    groupId: String,
    onNavigateBack: () -> Unit,
) {
    GroupDetailScreen(
        groupId = groupId,
        onNavigateBack = onNavigateBack
    )
}
