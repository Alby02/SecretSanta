/*
 * Licensed under the EUPL, Version 1.2.
 * You may obtain a copy of the Licence at:
 * https://joinup.ec.europa.eu/collection/eupl/eupl-text-eupl-12
 */

package it.alby02.secretsanta.domain.usecase

import com.google.firebase.auth.FirebaseAuth
import it.alby02.secretsanta.data.security.CryptoManager
import it.alby02.secretsanta.domain.repository.UserRepository
import kotlinx.coroutines.tasks.await

class LoginUseCase(
    private val auth: FirebaseAuth,
    private val userRepository: UserRepository,
    private val cryptoManager: CryptoManager
) {

    suspend operator fun invoke(email: String, password: String): Result<Unit> {
        return try {
            val authResult = auth.signInWithEmailAndPassword(email, password).await()
            val userId = authResult.user?.uid ?: throw Exception("User ID not found.")

            if (!cryptoManager.doesPrivateKeyExist()) {
                val userLoginData = userRepository.getUserLoginData(userId)
                cryptoManager.recoverAndStorePrivateKey(
                    password,
                    userLoginData.pbkdfSalt,
                    userLoginData.encryptedPrivateKey
                )
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}