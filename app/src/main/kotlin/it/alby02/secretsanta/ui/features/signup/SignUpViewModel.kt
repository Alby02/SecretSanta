/*
 * Licensed under the EUPL, Version 1.2.
 * You may obtain a copy of the Licence at:
 * https://joinup.ec.europa.eu/collection/eupl/eupl-text-eupl-12
 */

package it.alby02.secretsanta.ui.features.signup

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.firestore
import com.google.firebase.Firebase
import it.alby02.secretsanta.data.repository.UserRepositoryImpl
import it.alby02.secretsanta.data.security.CryptoManager
import it.alby02.secretsanta.domain.usecase.SignUpUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class SignUpViewModel(application: Application) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow(SignUpUiState())
    val uiState: StateFlow<SignUpUiState> = _uiState.asStateFlow()

    private val signUpUseCase: SignUpUseCase

    init {
        // In a real app, you would use Hilt/Dagger for dependency injection
        val auth: FirebaseAuth = Firebase.auth
        val firestore: FirebaseFirestore = Firebase.firestore
        val cryptoManager = CryptoManager(application.applicationContext)
        val userRepository = UserRepositoryImpl(firestore)
        signUpUseCase = SignUpUseCase(auth, userRepository, cryptoManager)
    }

    fun onEmailChange(email: String) {
        _uiState.update { it.copy(email = email) }
    }

    fun onPasswordChange(password: String) {
        _uiState.update { it.copy(password = password) }
    }

    fun onConfirmPasswordChange(confirmPassword: String) {
        _uiState.update { it.copy(confirmPassword = confirmPassword) }
    }

    fun onUsernameChange(username: String) {
        _uiState.update { it.copy(username = username) }
    }

    fun onSignUpClicked() {
        val state = _uiState.value

        // --- Validation ---
        if (state.email.isBlank() || state.password.isBlank() || state.username.isBlank()) {
            _uiState.update { it.copy(errorMessage = "All fields are required.") }
            return
        }
        if (state.password.length < 6) {
            _uiState.update { it.copy(errorMessage = "Password must be at least 6 characters.") }
            return
        }
        if (state.password != state.confirmPassword) {
            _uiState.update { it.copy(errorMessage = "Passwords do not match.") }
            return
        }

        _uiState.update { it.copy(isLoading = true, errorMessage = null) }

        viewModelScope.launch {
            val result = signUpUseCase(state.email, state.password, state.username)

            result.onSuccess {
                _uiState.update { it.copy(isLoading = false, isSignUpSuccessful = true) }
            }.onFailure {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = "Sign-up failed: ${it.errorMessage}"
                    )
                }
            }
            // Clear passwords from state for security
            _uiState.update { it.copy(password = "", confirmPassword = "") }
        }
    }
}