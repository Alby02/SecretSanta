/*
 * Licensed under the EUPL, Version 1.2.
 * You may obtain a copy of the Licence at:
 * https://joinup.ec.europa.eu/collection/eupl/eupl-text-eupl-12
 */

package it.alby02.secretsanta.domain.repository

interface AuthRepository {
    suspend fun login(email: String, password: String): Result<String>
    suspend fun signUp(email: String, password: String): Result<String>
    fun getCurrentUserId(): String?
    suspend fun deleteCurrentUser(): Result<Unit>
}
