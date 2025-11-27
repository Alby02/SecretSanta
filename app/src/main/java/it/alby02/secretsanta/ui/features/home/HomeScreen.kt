/*
 * Licensed under the EUPL, Version 1.2.
 * You may obtain a copy of the Licence at:
 * https://joinup.ec.europa.eu/collection/eupl/eupl-text-eupl-12
 */

@file:OptIn(ExperimentalMaterial3Api::class)

package it.alby02.secretsanta.ui.features.home

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Create
import androidx.compose.material.icons.filled.GroupAdd
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import it.alby02.secretsanta.data.model.Group
import it.alby02.secretsanta.ui.features.home.components.CreateGroupDialog
import it.alby02.secretsanta.ui.features.home.components.JoinGroupDialog
import it.alby02.secretsanta.ui.theme.SecretSantaTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onNavigateToGroup: (String) -> Unit,
    onLogout: () -> Unit,
    viewModel: HomeViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    // --- Dialogs ---
    if (uiState.showCreateDialog) {
        CreateGroupDialog(
            onDismissRequest = viewModel::onCreateDialogDismiss,
            onConfirm = { groupName ->
                viewModel.createGroup(groupName)
            }
        )
    }

    if (uiState.showJoinDialog) {
        JoinGroupDialog(
            onDismissRequest = viewModel::onJoinDialogDismiss,
            onConfirm = { joinCode ->
                viewModel.joinGroup(joinCode)
            }
        )
    }

    // --- UI ---
    Scaffold(
        topBar = {
            TopAppBar(title = { Text("My Secret Santa Groups") })
        },
        floatingActionButton = {
            ExpandableFab(
                isExpanded = uiState.isFabExpanded,
                onFabClicked = viewModel::onFabClicked,
                onCreateClicked = viewModel::onCreateGroupClicked,
                onJoinClicked = viewModel::onJoinGroupClicked
            )
        }
    ) { paddingValues ->
        if (uiState.isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(horizontal = 16.dp)
            ) {
                items(uiState.groups) { group ->
                    GroupListItem(
                        group = group,
                        onClick = { onNavigateToGroup(group.id) }
                    )
                }
            }
        }
    }
}

@Composable
fun GroupListItem(
    group: Group,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
            .clickable(onClick = onClick),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(text = group.groupName, style = MaterialTheme.typography.titleLarge)
            Text(text = "Join Code: ${group.joinCode}", style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Composable
fun ExpandableFab(
    isExpanded: Boolean,
    onFabClicked: () -> Unit,
    onCreateClicked: () -> Unit,
    onJoinClicked: () -> Unit
) {
    Column(horizontalAlignment = Alignment.End) {
        // "Create Group" mini-FAB
        AnimatedVisibility(visible = isExpanded) {
            FloatingActionButton(
                onClick = onCreateClicked,
                modifier = Modifier.padding(bottom = 8.dp),
                containerColor = MaterialTheme.colorScheme.secondaryContainer
            ) {
                Icon(Icons.Default.Create, contentDescription = "Create Group")
            }
        }

        // "Join Group" mini-FAB
        AnimatedVisibility(visible = isExpanded) {
            FloatingActionButton(
                onClick = onJoinClicked,
                modifier = Modifier.padding(bottom = 16.dp),
                containerColor = MaterialTheme.colorScheme.secondaryContainer
            ) {
                Icon(Icons.Default.GroupAdd, contentDescription = "Join Group")
            }
        }

        // Main "+" FAB
        FloatingActionButton(
            onClick = onFabClicked,
            containerColor = MaterialTheme.colorScheme.primary
        ) {
            Icon(Icons.Default.Add, contentDescription = "Add")
        }
    }
}

@Preview(showBackground = true)
@Composable
fun HomeScreenPreview() {
    SecretSantaTheme {
        val groups = listOf(
            Group(id = "1", groupName = "Office Party", joinCode = "ABC-123"),
            Group(id = "2", groupName = "Family Christmas", joinCode = "XYZ-789")
        )
        // This is a fake ViewModel for preview
        val previewState = HomeUiState(isLoading = false, groups = groups)
        // Since we can't use a real VM, we'll just show the Scaffold
        Scaffold { padding ->
            LazyColumn(modifier = Modifier.padding(padding)) {
                items(previewState.groups) { group ->
                    GroupListItem(group = group, onClick = {})
                }
            }
        }
    }
}