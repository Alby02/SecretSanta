/*
 * Licensed under the EUPL, Version 1.2.
 * You may obtain a copy of the Licence at:
 * https://joinup.ec.europa.eu/collection/eupl/eupl-text-eupl-12
 */

package it.alby02.secretsanta.domain.usecase

import it.alby02.secretsanta.data.security.CryptoManager
import it.alby02.secretsanta.domain.repository.GroupRepository
import kotlinx.coroutines.flow.first
import org.koin.core.annotation.Factory
import javax.crypto.spec.SecretKeySpec

@Factory
class ViewAssignmentUseCase(
    private val groupRepository: GroupRepository,
    private val cryptoManager: CryptoManager
) {
    suspend operator fun invoke(groupId: String): String {
        val assignmentData = groupRepository.getMyAssignment(groupId).getOrThrow()
        
        // 1. Decrypt the UserKey using private RSA key
        val userKeyBytes = cryptoManager.decrypt(assignmentData.encryptedKey)
        val userKey = SecretKeySpec(userKeyBytes, "AES")

        // 2. Decrypt the ReceiverId using UserKey
        val receiverIdBytes = cryptoManager.decrypt(assignmentData.encryptedReceiverId, userKey)
        val receiverId = String(receiverIdBytes)

        // 3. Fetch receiver's profile to get the name
        // We need a way to get a single user profile. `getUsers` returns a Flow<List>.
        // Ideally we should have `getUser(userId)`.
        // For now, we can use `getUsers(listOf(receiverId)).first().firstOrNull()?.username`
        
        val users = groupRepository.getUsers(listOf(receiverId))
        // We need to collect the flow.
        // Since `getUsers` returns a Flow, we can't just call it synchronously easily without `first()`.
        // But `invoke` is suspend.
        
        val receiverProfile = users.first().firstOrNull()
            ?: return "Unknown User ($receiverId)"
            
        return receiverProfile.id
    }
}
