/*
 * Licensed under the EUPL, Version 1.2.
 * You may obtain a copy of the Licence at:
 * https://joinup.ec.europa.eu/collection/eupl/eupl-text-eupl-12
 */

package it.alby02.secretsanta.domain.usecase

import it.alby02.secretsanta.data.model.UserAccountData
import it.alby02.secretsanta.data.security.CryptoManager
import it.alby02.secretsanta.domain.repository.AuthRepository
import it.alby02.secretsanta.domain.repository.UserRepository
import org.koin.core.annotation.Factory

@Factory
class SignUpUseCase(
    private val authRepository: AuthRepository,
    private val userRepository: UserRepository,
    private val cryptoManager: CryptoManager
) {

    suspend operator fun invoke(email: String, password: String, username: String): Result<Unit> {
        return try {
            // 1. Create user in Firebase Auth
            val userId = authRepository.signUp(email, password).getOrThrow()

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
            authRepository.deleteCurrentUser()
            Result.failure(e)
        }
    }
}