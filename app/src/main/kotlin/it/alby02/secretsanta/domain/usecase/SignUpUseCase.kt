/*
 * Licensed under the EUPL, Version 1.2.
 * You may obtain a copy of the Licence at:
 * https://joinup.ec.europa.eu/collection/eupl/eupl-text-eupl-12
 */

package it.alby02.secretsanta.domain.usecase

import com.google.firebase.auth.FirebaseAuth
import it.alby02.secretsanta.data.model.UserAccountData
import it.alby02.secretsanta.data.security.CryptoManager
import it.alby02.secretsanta.domain.repository.UserRepository
import kotlinx.coroutines.tasks.await

class SignUpUseCase(
    private val auth: FirebaseAuth,
    private val userRepository: UserRepository,
    private val cryptoManager: CryptoManager
) {

    suspend operator fun invoke(email: String, password: String, username: String): Result<Unit> {
        return try {
            // 1. Create user in Firebase Auth
            val authResult = auth.createUserWithEmailAndPassword(email, password).await()
            val userId = authResult.user?.uid ?: throw Exception("User ID not found after creation.")

            // 2. Generate new key pair and store private key in Android Keystore for the session
            val publicKeyString = cryptoManager.generateAndStoreNewKeyPair()

            // 3. Encrypt the private key using the password for Firestore backup
            val (encryptedKey, salt) = cryptoManager.encryptPrivateKeyForBackup(password)

            // 4. Prepare user data and create all documents in a single transaction
            val userAccountData = UserAccountData(
                userId = userId,
                username = username,
                publicKey = publicKeyString,
                encryptedPrivateKey = encryptedKey,
                pbkdfSalt = salt
            )
            userRepository.createUser(userAccountData)

            Result.success(Unit)
        } catch (e: Exception) {
            // If any step fails, delete the partially created Firebase Auth user to allow a clean retry.
            auth.currentUser?.delete()?.await()
            Result.failure(e)
        }
    }
}