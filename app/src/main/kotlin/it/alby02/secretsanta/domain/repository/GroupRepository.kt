/*
 * Licensed under the EUPL, Version 1.2.
 * You may obtain a copy of the Licence at:
 * https://joinup.ec.europa.eu/collection/eupl/eupl-text-eupl-12
 */

package it.alby02.secretsanta.domain.repository

import it.alby02.secretsanta.data.model.Group
import kotlinx.coroutines.flow.Flow

interface GroupRepository {

    /**
     * Fetches a real-time stream of the current user's groups.
     */
    fun getMyGroups(): Flow<List<Group>>

    /**
     * Fetches a real time stream of the selected group.
     */
    fun getGroupDetails(groupId: String): Flow<Group>

    /**
     * Creates a new group with the current user as the admin.
     */
    suspend fun createGroup(groupName: String): Result<Unit>

    /**
     * Allows the current user to join an existing group using a join code.
     */
    suspend fun joinGroup(joinCode: String): Result<Unit>

    /**
     * Initiates the matching process.
     */
    suspend fun initiateMatching(
        groupId: String,
        assignments: List<AssignmentTransactionData>,
        keyShares: List<KeyShareTransactionData>,
        encryptedMasterList: ByteArray
    ): Result<Unit>

    /**
     * Submits a recovery share for a group.
     */
    suspend fun submitRecoveryShare(groupId: String, encryptedShareForAdmin: String): Result<Unit>

    /**
     * Fetches all recovery shares for a group.
     */
    fun getRecoveryShares(groupId: String): Flow<List<String>>

    /**
     * Finalizes the recovery process by updating the master list and setting state to completed.
     */
    suspend fun finalizeRecovery(groupId: String, decryptedMasterListJSON: String): Result<Unit>

    /**
     * Fetches the encrypted assignment for the current user.
     */
    suspend fun getMyAssignment(groupId: String): Result<AssignmentTransactionData>

    /**
     * Fetches the encrypted master list (admin only).
     */
    suspend fun getEncryptedMasterList(groupId: String): Result<String>
    
    /**
     * Fetches users public keys
     */
    fun getUsers(userIds: List<String>): Flow<List<it.alby02.secretsanta.data.model.UserProfile>>

    /**
     * Initiates the recovery process (Admin only).
     */
    suspend fun initiateRecovery(groupId: String): Result<Unit>

    /**
     * Fetches the current user's encrypted share for the group.
     */
    suspend fun getMyShare(groupId: String): String
}

data class AssignmentTransactionData(
    val giverId: String,
    val encryptedReceiverId: ByteArray,
    val encryptedKey: String // Encrypted with giver's public RSA key
)

data class KeyShareTransactionData(
    val memberId: String,
    val encryptedShare: String // Encrypted with member's public RSA key
)