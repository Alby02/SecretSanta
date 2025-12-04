/*
 * Licensed under the EUPL, Version 1.2.
 * You may obtain a copy of the Licence at:
 * https://joinup.ec.europa.eu/collection/eupl/eupl-text-eupl-12
 */

package it.alby02.secretsanta.data.repository

import com.google.firebase.firestore.FirebaseFirestore
import it.alby02.secretsanta.data.model.UserAccountData
import it.alby02.secretsanta.data.model.UserLoginData
import it.alby02.secretsanta.domain.repository.UserRepository
import kotlinx.coroutines.tasks.await

class UserRepositoryImpl(private val firestore: FirebaseFirestore) : UserRepository {

    override suspend fun getUserLoginData(userId: String): UserLoginData {
        val document = firestore.collection("usersLogin").document(userId).get().await()
        val encryptedPrivateKey = document.getString("encryptedPrivateKey")
            ?: throw Exception("Encrypted private key not found in database.")
        val pbkdfSalt = document.getString("pbkdfSalt")
            ?: throw Exception("Key salt not found in database.")
        return UserLoginData(encryptedPrivateKey, pbkdfSalt)
    }

    override suspend fun createUser(userAccountData: UserAccountData) {
        firestore.batch().apply {
            // Public user data
            val userPublicRef = firestore.collection("usersPublic").document(userAccountData.userId)
            set(userPublicRef, mapOf(
                "username" to userAccountData.username,
                "publicKey" to userAccountData.publicKey
            ))

            // Private (login) data
            val userLoginRef = firestore.collection("usersLogin").document(userAccountData.userId)
            set(userLoginRef, mapOf(
                "encryptedPrivateKey" to userAccountData.encryptedPrivateKey,
                "pbkdfSalt" to userAccountData.pbkdfSalt
            ))

            // User groups data
            val userGroupsRef = firestore.collection("usersGroups").document(userAccountData.userId)
            set(userGroupsRef, mapOf("groupIds" to emptyList<String>()))

        }.commit().await()
    }
}