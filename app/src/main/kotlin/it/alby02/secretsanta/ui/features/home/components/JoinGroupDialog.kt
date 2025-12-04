/*
 * Licensed under the EUPL, Version 1.2.
 * You may obtain a copy of the Licence at:
 * https://joinup.ec.europa.eu/collection/eupl/eupl-text-eupl-12
 */

package it.alby02.secretsanta.ui.features.home.components

import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import it.alby02.secretsanta.ui.theme.SecretSantaTheme

@Composable
fun JoinGroupDialog(
    onDismissRequest: () -> Unit,
    onConfirm: (joinCode: String) -> Unit
) {
    var joinCode by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismissRequest,
        title = { Text("Join Group") },
        text = {
            OutlinedTextField(
                value = joinCode,
                onValueChange = { joinCode = it },
                label = { Text("Join Code (e.g., ABC-123)") },
                singleLine = true
            )
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(joinCode) },
                enabled = joinCode.isNotBlank()
            ) {
                Text("Join")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismissRequest) {
                Text("Cancel")
            }
        }
    )
}

@Preview
@Composable
fun JoinGroupDialogPreview() {
    SecretSantaTheme {
        JoinGroupDialog(onDismissRequest = {}, onConfirm = {})
    }
}