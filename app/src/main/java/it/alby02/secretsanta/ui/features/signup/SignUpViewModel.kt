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
import it.alby02.secretsanta.data.model.UserProfile
import it.alby02.secretsanta.data.security.CryptoManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class SignUpViewModel(application: Application) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow(SignUpUiState())
    val uiState: StateFlow<SignUpUiState> = _uiState.asStateFlow()

    private val auth: FirebaseAuth = Firebase.auth
    private val firestore: FirebaseFirestore = Firebase.firestore

    // You MUST implement this CryptoManager
    private val cryptoManager = CryptoManager(application.applicationContext)

    fun onEmailChange(email: String) {
        _uiState.update { it.copy(email = email) }
    }

    fun onPasswordChange(password: String) {
        _uiState.update { it.copy(password = password) }
    }

    fun onConfirmPasswordChange(confirmPassword: String) {
        _uiState.update { it.copy(confirmPassword = confirmPassword) }
    }

    fun onSignUpClicked() {
        val state = _uiState.value
        val email = state.email
        val password = state.password

        // --- Validation ---
        if (email.isBlank() || password.isBlank() || state.confirmPassword.isBlank()) {
            _uiState.update { it.copy(errorMessage = "All fields are required.") }
            return
        }
        if (password.length < 6) {
            _uiState.update { it.copy(errorMessage = "Password must be at least 6 characters.") }
            return
        }
        if (password != state.confirmPassword) {
            _uiState.update { it.copy(errorMessage = "Passwords do not match.") }
            return
        }

        _uiState.update { it.copy(isLoading = true, errorMessage = null, loadingMessage = "Creating user...") }

        viewModelScope.launch {
            try {
                // 1. Create user in Firebase Auth
                val authResult = auth.createUserWithEmailAndPassword(email, password).await()
                val userId = authResult.user?.uid ?: throw Exception("User ID not found.")

                _uiState.update { it.copy(loadingMessage = "Generating keys...") }

                // 2. Generate new key pair and store private key in Android Keystore
                val publicKeyString = cryptoManager.generateAndStoreNewKeyPair()

                // 3. Encrypt the private key using the password for Firestore backup
                val (encryptedKey, salt) = cryptoManager.encryptPrivateKeyForBackup(password)

                _uiState.update { it.copy(loadingMessage = "Saving profile...") }

                // 4. Create the user profile document in Firestore
                val userProfile = UserProfile(
                    email = email,
                    publicKey = publicKeyString,
                    encryptedPrivateKey = encryptedKey,
                    pbkdfSalt = salt
                )

                firestore.collection("users").document(userId).set(userProfile).await()

                // 5. Success
                _uiState.update { it.copy(isLoading = false, isSignUpSuccessful = true) }

            } catch (e: Exception)
            {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = "Sign-up failed: ${e.message}"
                    )
                }
            } finally {
                // Clear passwords from state for security
                _uiState.update { it.copy(password = "", confirmPassword = "") }
            }
        }
    }
}