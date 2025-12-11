/*
 * Licensed under the EUPL, Version 1.2.
 * You may obtain a copy of the Licence at:
 * https://joinup.ec.europa.eu/collection/eupl/eupl-text-eupl-12
 */

package it.alby02.secretsanta.data.model

import com.google.firebase.firestore.DocumentId

// Represents a group in Firestore
data class Group(
    @DocumentId val id: String = "", // Firestore document ID
    val groupName: String = "",
    val adminId: String = "",
    val joinCode: String = "",
    val members: List<String> = emptyList(),
    val rules: List<Rule> = emptyList(),
    val state: String = "pending"
)

data class Rule (val giverId: String, val receiverId: String)