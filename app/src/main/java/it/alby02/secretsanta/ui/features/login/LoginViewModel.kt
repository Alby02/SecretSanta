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
import it.alby02.secretsanta.data.security.CryptoManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class LoginViewModel(application: Application) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow(LoginUiState())
    val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()

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

    fun onLoginClicked() {
        val email = _uiState.value.email
        val password = _uiState.value.password // Get password for key derivation

        if (email.isBlank() || password.isBlank()) {
            _uiState.update { it.copy(errorMessage = "Email and password cannot be empty.") }
            return
        }

        _uiState.update { it.copy(isLoading = true, errorMessage = null) }

        viewModelScope.launch {
            try {
                // 1. Authenticate with Firebase
                val authResult = auth.signInWithEmailAndPassword(email, password).await()
                val userId = authResult.user?.uid ?: throw Exception("User ID not found.")

                // 2. Check if keys already exist locally (Android Keystore)
                if (cryptoManager.doesPrivateKeyExist()) {
                    // Keys exist! User is logged in and ready.
                    _uiState.update { it.copy(isLoading = false, isLoginSuccessful = true) }
                } else {
                    // 3. KEY RECOVERY FLOW (Flow 2)
                    // Keys do not exist. Fetch from Firestore and decrypt.
                    _uiState.update { it.copy(errorMessage = "Restoring your keys...") }

                    val userDoc = firestore.collection("users").document(userId).get().await()
                    val encryptedPrivateKey = userDoc.getString("encryptedPrivateKey")
                        ?: throw Exception("Encrypted private key not found in database.")
                    val pbkdfSalt = userDoc.getString("pbkdfSalt")
                        ?: throw Exception("Key salt not found in database.")

                    // 4. Decrypt and store key in Android Keystore
                    // This is the critical step that uses the password!
                    cryptoManager.recoverAndStorePrivateKey(password, pbkdfSalt, encryptedPrivateKey)

                    // 5. Success
                    _uiState.update { it.copy(isLoading = false, isLoginSuccessful = true) }
                }

            } catch (e: Exception) {
                // Handle all errors (Firebase auth, Firestore fetch, Crypto failure)
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = "Login failed: ${e.message}"
                    )
                }
            } finally {
                // Clear password from state for security
                _uiState.update { it.copy(password = "") }
            }
        }
    }
}