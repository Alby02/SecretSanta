/*
 * Licensed under the EUPL, Version 1.2.
 * You may obtain a copy of the Licence at:
 * https://joinup.ec.europa.eu/collection/eupl/eupl-text-eupl-12
 */

package it.alby02.secretsanta.domain.usecase

import it.alby02.secretsanta.data.security.CryptoManager
import it.alby02.secretsanta.domain.repository.AuthRepository
import it.alby02.secretsanta.domain.repository.UserRepository
import org.koin.core.annotation.Factory

@Factory
class LoginUseCase(
    private val authRepository: AuthRepository,
    private val userRepository: UserRepository,
    private val cryptoManager: CryptoManager
) {

    suspend operator fun invoke(email: String, password: String): Result<Unit> {
        return try {
            val userId = authRepository.login(email, password).getOrThrow()

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