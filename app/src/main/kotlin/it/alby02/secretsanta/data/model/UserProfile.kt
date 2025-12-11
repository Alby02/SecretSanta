/*
 * Licensed under the EUPL, Version 1.2.
 * You may obtain a copy of the Licence at:
 * https://joinup.ec.europa.eu/collection/eupl/eupl-text-eupl-12
 */

package it.alby02.secretsanta.data.model

import com.google.firebase.firestore.DocumentId

// Firestore User Data Class
data class UserProfile(
    @DocumentId val id: String = "",
    val email: String = "",
    val publicKey: String = "",
    val encryptedPrivateKey: String,
    val pbkdfSalt: String
)