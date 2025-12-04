/*
 * Licensed under the EUPL, Version 1.2.
 * You may obtain a copy of the Licence at:
 * https://joinup.ec.europa.eu/collection/eupl/eupl-text-eupl-12
 */

package it.alby02.secretsanta.domain.repository

import it.alby02.secretsanta.data.model.UserAccountData
import it.alby02.secretsanta.data.model.UserLoginData

interface UserRepository {

    /**
     * Fetches the user's encrypted private key and salt from Firestore.
     */
    suspend fun getUserLoginData(userId: String): UserLoginData

    /**
     * Creates all the necessary user documents in Firestore.
     */
    suspend fun createUser(userAccountData: UserAccountData)
}