/*
 * Licensed under the EUPL, Version 1.2.
 * You may obtain a copy of the Licence at:
 * https://joinup.ec.europa.eu/collection/eupl/eupl-text-eupl-12
 */

package it.alby02.secretsanta.ui.features.login

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import it.alby02.secretsanta.MainActivity
import it.alby02.secretsanta.ui.features.signup.SignUpActivity
import it.alby02.secretsanta.ui.theme.SecretSantaTheme

class LoginActivity : ComponentActivity() {

    private val viewModel: LoginViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            SecretSantaTheme {
                // Collect the login state
                val uiState by viewModel.uiState.collectAsState()
                val context = LocalContext.current

                // Observe the login success state
                LaunchedEffect(uiState.isLoginSuccessful) {
                    if (uiState.isLoginSuccessful) {
                        // On successful login (and key recovery), navigate to main app
                        val intent = Intent(context, MainActivity::class.java).apply {
                            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                        }
                        context.startActivity(intent)
                    }
                }

                LoginScreen(
                    uiState = uiState,
                    onEmailChange = viewModel::onEmailChange,
                    onPasswordChange = viewModel::onPasswordChange,
                    onLoginClick = viewModel::onLoginClicked,
                    onSignUpClick = {
                        // Navigate to your SignUpActivity (where Flow 1 happens)
                        val intent = Intent(context, SignUpActivity::class.java)
                        context.startActivity(intent)
                    }
                )
            }
        }
    }
}