/*
 * Licensed under the EUPL, Version 1.2.
 * You may obtain a copy of the Licence at:
 * https://joinup.ec.europa.eu/collection/eupl/eupl-text-eupl-12
 */

package it.alby02.secretsanta.ui.features.signup

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import it.alby02.secretsanta.ui.theme.SecretSantaTheme

// --- STATE ---
data class SignUpUiState(
    val email: String = "",
    val password: String = "",
    val confirmPassword: String = "",
    val username: String = "",
    val isLoading: Boolean = false,
    val isSignUpSuccessful: Boolean = false,
    val errorMessage: String? = null,
    val loadingMessage: String? = null,
)

// --- SCREEN ---
@Composable
fun SignUpScreen(
    uiState: SignUpUiState,
    onEmailChange: (String) -> Unit,
    onUsernameChange: (String) -> Unit, // Added this
    onPasswordChange: (String) -> Unit,
    onConfirmPasswordChange: (String) -> Unit,
    onSignUpClick: () -> Unit,
    onLoginClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(text = "Create Account", style = androidx.compose.material3.MaterialTheme.typography.headlineMedium)

        Spacer(modifier = Modifier.height(32.dp))

        OutlinedTextField(
            value = uiState.email,
            onValueChange = onEmailChange,
            label = { Text("Email") },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = uiState.username,
            onValueChange = onUsernameChange, // Connected this
            label = { Text("Username") },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = uiState.password,
            onValueChange = onPasswordChange,
            label = { Text("Password") },
            visualTransformation = PasswordVisualTransformation(),
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = uiState.confirmPassword,
            onValueChange = onConfirmPasswordChange,
            label = { Text("Confirm Password") },
            visualTransformation = PasswordVisualTransformation(),
            modifier = Modifier.fillMaxWidth()
        )

        if (uiState.isLoading) {
            Spacer(modifier = Modifier.height(16.dp))
            CircularProgressIndicator()
            uiState.loadingMessage?.let {
                Spacer(modifier = Modifier.height(8.dp))
                Text(text = it)
            }
        } else {
            uiState.errorMessage?.let {
                Text(text = it, color = androidx.compose.material3.MaterialTheme.colorScheme.error)
            }
            Spacer(modifier = Modifier.height(16.dp))
            Button(onClick = onSignUpClick, modifier = Modifier.fillMaxWidth()) {
                Text("Sign Up")
            }
        }

        TextButton(onClick = onLoginClick) {
            Text("Already have an account? Log in")
        }
    }
}

@Composable
@Preview(showBackground = true)
fun SignUpScreenPreview() {
    SecretSantaTheme {
        SignUpScreen(
            uiState = SignUpUiState(),
            onEmailChange = {},
            onUsernameChange = {}, // Connected this
            onPasswordChange = {},
            onConfirmPasswordChange = {},
            onSignUpClick = {},
            onLoginClick = {}
        )
    }
}

@Composable
@Preview(showBackground = true)
fun SignUpScreenLoadingPreview() {
    SecretSantaTheme {
        SignUpScreen(
            uiState = SignUpUiState(
                isLoading = true,
                loadingMessage = "Signing up..."
            ),
            onEmailChange = {},
            onUsernameChange = {}, // Connected this
            onPasswordChange = {},
            onConfirmPasswordChange = {},
            onSignUpClick = {},
            onLoginClick = {}
        )
    }
}

@Composable
@Preview(showBackground = true)
fun SignUpScreenErrorPreview() {
    SecretSantaTheme {
        SignUpScreen(
            uiState = SignUpUiState(
                errorMessage = "An error occurred"
            ),
            onEmailChange = {},
            onUsernameChange = {}, // Connected this
            onPasswordChange = {},
            onConfirmPasswordChange = {},
            onSignUpClick = {},
            onLoginClick = {}
        )
    }
}