package it.alby02.secretsanta.domain.usecase

import it.alby02.secretsanta.data.model.Group
import it.alby02.secretsanta.data.security.CryptoManager
import it.alby02.secretsanta.domain.repository.AssignmentTransactionData
import it.alby02.secretsanta.domain.repository.GroupRepository
import it.alby02.secretsanta.domain.repository.KeyShareTransactionData
import kotlinx.coroutines.flow.first
import org.koin.core.annotation.Factory

@Factory
class InitiateMatchingUseCase(
    private val groupRepository: GroupRepository,
    private val cryptoManager: CryptoManager
) {

    suspend operator fun invoke(groupId: String) {
        val group = groupRepository.getGroupDetails(groupId).first()
        if (group.members.size < 2) {
            throw IllegalStateException("Cannot run matching with fewer than 2 members.")
        }

        val users = groupRepository.getUsers(group.members).first()
        val userMap = users.associateBy { it.id }

        // 1. Perform the matching
        val assignmentsMap = performMatching(group)

        // 2. Generate master key and shares for recovery
        val masterKey = cryptoManager.generateAesKey()
        val masterListString = assignmentsMap.entries.joinToString("\n") { "${it.key} -> ${it.value}" }
        val encryptedMasterList = cryptoManager.encrypt(masterListString.toByteArray(), masterKey)

        // Threshold: (N / 2) + 1
        val threshold = (group.members.size / 2) + 1
        val shares = cryptoManager.splitSecret(masterKey.encoded, group.members.size, threshold)

        val keyShares = group.members.mapIndexed { index, memberId ->
            val user = userMap[memberId] ?: throw IllegalStateException("User not found for key share creation")
            val encryptedShare = cryptoManager.encrypt(shares[index], user.publicKey)
            KeyShareTransactionData(memberId, encryptedShare)
        }

        // 3. Create assignment data for each user
        val assignments = assignmentsMap.map { (giverId, receiverId) ->
            val giver = userMap[giverId] ?: throw IllegalStateException("Giver not found")
            val userKey = cryptoManager.generateAesKey()
            val encryptedReceiverId = cryptoManager.encrypt(receiverId.toByteArray(), userKey)
            val encryptedKey = cryptoManager.encrypt(userKey.encoded, giver.publicKey)
            AssignmentTransactionData(giverId, encryptedReceiverId, encryptedKey)
        }

        // 4. Call the repository to execute the transaction
        groupRepository.initiateMatching(groupId, assignments, keyShares, encryptedMasterList)
    }

    private fun performMatching(group: Group): Map<String, String> {
        val members = group.members.toMutableList()
        val assignments = mutableMapOf<String, String>()
        val rules = group.rules.associate { it.giverId to it.receiverId }

        // Simple matching logic: shuffle and assign. A more robust implementation
        // would handle cycles of 1 and respect the rules.
        var shuffledReceivers = members.shuffled()
        
        // Retry logic for simple shuffle to avoid self-assignment
        var attempts = 0
        while (attempts < 100) {
             var valid = true
             for (i in members.indices) {
                 if (members[i] == shuffledReceivers[i]) {
                     valid = false
                     break
                 }
                 // Check rules if any
                 if (rules[members[i]] != null && rules[members[i]] != shuffledReceivers[i]) {
                     // This rule check is simplistic, it assumes strict adherence. 
                     // If rule says A->B, and shuffle gives A->C, it's invalid.
                     // But if rule is exclusion, we need different logic.
                     // SPEC says: rules: [{ "giverId": "userIdA", "receiverId": "userIdB" }]
                     // This looks like forced assignment.
                     valid = false 
                     break
                 }
             }
             
             if (valid) {
                 for (i in members.indices) {
                     assignments[members[i]] = shuffledReceivers[i]
                 }
                 return assignments
             }
             
             shuffledReceivers = members.shuffled()
             attempts++
        }
        
        throw IllegalStateException("Could not find valid assignment after 100 attempts")
    }
}
