/*
 * Licensed under the EUPL, Version 1.2.
 * You may obtain a copy of the Licence at:
 * https://joinup.ec.europa.eu/collection/eupl/eupl-text-eupl-12
 */

package it.alby02.secretsanta.data.repository

import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import it.alby02.secretsanta.domain.repository.AuthRepository
import kotlinx.coroutines.tasks.await
import org.koin.core.annotation.Single

@Single
class FirebaseAuthRepository() : AuthRepository {
    private val auth = Firebase.auth

    override suspend fun login(email: String, password: String): Result<String> {
        return try {
            val authResult = auth.signInWithEmailAndPassword(email, password).await()
            val userId = authResult.user?.uid ?: throw Exception("User ID not found after login.")
            Result.success(userId)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun signUp(email: String, password: String): Result<String> {
        return try {
            val authResult = auth.createUserWithEmailAndPassword(email, password).await()
            val userId = authResult.user?.uid ?: throw Exception("User ID not found after creation.")
            Result.success(userId)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override fun getCurrentUserId(): String? {
        return auth.currentUser?.uid
    }

    override suspend fun deleteCurrentUser(): Result<Unit> {
        return try {
            auth.currentUser?.delete()?.await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
