/*
 * Licensed under the EUPL, Version 1.2.
 * You may obtain a copy of the Licence at:
 * https://joinup.ec.europa.eu/collection/eupl/eupl-text-eupl-12
 */

package it.alby02.secretsanta.ui.features.groupdetail

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import it.alby02.secretsanta.data.model.Group
import it.alby02.secretsanta.ui.theme.SecretSantaTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GroupDetailScreen(
    onNavigateBack: () -> Unit,
    uiState: GroupDetailUiState,
    startMatching: () -> Unit = {},
    viewAssignment: () -> Unit = {},
    initiateRecovery: () -> Unit = {},
    contributeToRecovery: () -> Unit = {},
    finalizeRecovery: () -> Unit
) {
    val currentUser = Firebase.auth.currentUser

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(uiState.group?.groupName ?: "Loading...") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { paddingValues ->
        if (uiState.isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else if (uiState.group != null) {
            val group = uiState.group
            val isAdmin = group.adminId == currentUser?.uid
            
            Column(modifier = Modifier.padding(paddingValues)) {
                GroupDetailContent(
                    modifier = Modifier.weight(1f),
                    group = group,
                    participants = uiState.participants
                )
                
                // Actions
                Column(modifier = Modifier.padding(16.dp).fillMaxWidth()) {
                    if (uiState.errorMessage != null) {
                        Text(uiState.errorMessage, color = MaterialTheme.colorScheme.error)
                        Spacer(modifier = Modifier.height(8.dp))
                    }

                    when (group.state) {
                        "pending" -> {
                            if (isAdmin) {
                                Button(
                                    onClick = startMatching,
                                    enabled = !uiState.isMatching && group.members.size >= 2,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    if (uiState.isMatching) {
                                        CircularProgressIndicator(modifier = Modifier.size(24.dp))
                                    } else {
                                        Text("Start Matching")
                                    }
                                }
                            } else {
                                Text("Waiting for admin to start matching...", style = MaterialTheme.typography.bodyMedium)
                            }
                        }
                        "assigned", "completed" -> {
                            if (uiState.assignment == null) {
                                Button(
                                    onClick = viewAssignment,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text("View My Assignment")
                                }
                            } else {
                                Card(
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(modifier = Modifier.padding(16.dp)) {
                                        Text("You are the Secret Santa for:", style = MaterialTheme.typography.titleMedium)
                                        Text(uiState.assignment, style = MaterialTheme.typography.headlineMedium)
                                    }
                                }
                            }
                            
                            Spacer(modifier = Modifier.height(16.dp))
                            
                            if (isAdmin && group.state == "assigned") {
                                OutlinedButton(
                                    onClick = initiateRecovery,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text("Initiate Key Recovery (Emergency)")
                                }
                            }
                        }
                        "recovery" -> {
                            Text("Recovery Mode Active", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.error)
                            Spacer(modifier = Modifier.height(8.dp))
                            
                            if (isAdmin) {
                                Text("Progress: ${uiState.recoveryProgress + 1} / ${uiState.recoveryThreshold} shares")
                                LinearProgressIndicator(
                                    progress = { (uiState.recoveryProgress + 1).toFloat() / uiState.recoveryThreshold },
                                    modifier = Modifier.fillMaxWidth(),
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Button(
                                    onClick = finalizeRecovery,
                                    enabled = (uiState.recoveryProgress + 1) >= uiState.recoveryThreshold && !uiState.isRecovering,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    if (uiState.isRecovering) {
                                        CircularProgressIndicator(modifier = Modifier.size(24.dp))
                                    } else {
                                        Text("Finalize Recovery")
                                    }
                                }
                            } else {
                                Button(
                                    onClick = contributeToRecovery,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text("Contribute My Share")
                                }
                            }
                        }
                    }
                }
            }
        } else {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Error: ${uiState.errorMessage ?: "Group not found"}")
            }
        }
    }
}

@Composable
fun GroupDetailContent(
    modifier: Modifier = Modifier,
    group: Group,
    participants: List<String> // TODO: Change to List<UserProfile>
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Group Info
        Text("Group Details", style = MaterialTheme.typography.headlineSmall)
        Spacer(modifier = Modifier.height(8.dp))
        Text("Group Name: ${group.groupName}", style = MaterialTheme.typography.bodyLarge)
        Text("Join Code: ${group.joinCode}", style = MaterialTheme.typography.bodyLarge)

        Spacer(modifier = Modifier.height(24.dp))

        // Horizontal Participant List
        Text("Participants", style = MaterialTheme.typography.headlineSmall)
        Spacer(modifier = Modifier.height(8.dp))
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(participants) { participantId ->
                // TODO: Show a real participant card
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer
                    )
                ) {
                    Text(
                        text = "User ID: ${participantId.take(4)}...", // Show a placeholder
                        modifier = Modifier.padding(16.dp)
                    )
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun GroupDetailScreenPreview() {
    SecretSantaTheme {
        GroupDetailContent(
            group = Group(
                id = "1",
                groupName = "Preview Group",
                joinCode = "PRE-123"
            ),
            participants = listOf("user1", "user2", "user3")
        )
    }
}