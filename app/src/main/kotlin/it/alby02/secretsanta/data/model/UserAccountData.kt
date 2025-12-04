/*
 * Licensed under the EUPL, Version 1.2.
 * You may obtain a copy of the Licence at:
 * https://joinup.ec.europa.eu/collection/eupl/eupl-text-eupl-12
 */

package it.alby02.secretsanta.data.model

/**
 * A data class holding all the necessary information to create a new user in Firestore.
 */
data class UserAccountData(
    val userId: String,
    val username: String,
    val publicKey: String,
    val encryptedPrivateKey: String,
    val pbkdfSalt: String
)