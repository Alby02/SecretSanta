/*
 * Licensed under the EUPL, Version 1.2.
 * You may obtain a copy of the Licence at:
 * https://joinup.ec.europa.eu/collection/eupl/eupl-text-eupl-12
 */

package it.alby02.secretsanta.ui.features.login

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
import it.alby02.secretsanta.domain.usecase.LoginUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class LoginViewModel(application: Application) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow(LoginUiState())
    val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()

    private val loginUseCase: LoginUseCase

    init {
        // In a real app, you would use Hilt/Dagger for dependency injection
        val auth: FirebaseAuth = Firebase.auth
        val firestore: FirebaseFirestore = Firebase.firestore
        val cryptoManager = CryptoManager(application.applicationContext)
        val userRepository = UserRepositoryImpl(firestore)
        loginUseCase = LoginUseCase(auth, userRepository, cryptoManager)
    }

    fun onEmailChange(email: String) {
        _uiState.update { it.copy(email = email) }
    }

    fun onPasswordChange(password: String) {
        _uiState.update { it.copy(password = password) }
    }

    fun onLoginClicked() {
        val email = _uiState.value.email
        val password = _uiState.value.password

        if (email.isBlank() || password.isBlank()) {
            _uiState.update { it.copy(errorMessage = "Email and password cannot be empty.") }
            return
        }

        _uiState.update { it.copy(isLoading = true, errorMessage = null) }

        viewModelScope.launch {
            val result = loginUseCase(email, password)

            result.onSuccess {
                _uiState.update { it.copy(isLoading = false, isLoginSuccessful = true) }
            }.onFailure {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = "Login failed: ${it.errorMessage}"
                    )
                }
            }
            // Clear password from state for security
            _uiState.update { it.copy(password = "") }
        }
    }
}