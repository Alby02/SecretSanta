package it.alby02.secretsanta.domain.usecase

import it.alby02.secretsanta.data.security.CryptoManager
import it.alby02.secretsanta.domain.repository.GroupRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import android.util.Base64

class AdminKeyRecoveryUseCase(
    private val groupRepository: GroupRepository,
    private val cryptoManager: CryptoManager
) {

    suspend fun initiateRecovery(groupId: String) {
        // In a real app, we might want a specific method in repo to set state to recovery,
        // but currently we can use a generic update or add a method to repo.
        // For now, let's assume we need to add initiateRecovery to repo or just use a workaround.
        // The plan said "Update GroupRepository (getGroupDetails, initiateMatching, recovery methods)".
        // I missed adding `initiateRecovery` to repo interface explicitly in the previous step, 
        // but I can add it now or just use `finalizeRecovery` logic partially? 
        // No, `finalizeRecovery` sets state to completed.
        // I should probably add `setGroupState(groupId, state)` to repo or `initiateRecovery`.
        // Let's add `initiateRecovery` to repo in a separate step if needed, or just assume it exists?
        // No, I can't assume. I'll implement it here if I can, or modify repo.
        // Actually, I'll just use a direct firestore call in Repo if I had access, but I don't here.
        // I'll add `initiateRecovery` to GroupRepository interface and impl in the next step or 
        // I can just add it to this file if I modify Repo first. 
        // Let's modify Repo first to be clean.
        // Wait, I can't modify Repo in the middle of writing this file.
        // I'll write this file assuming `initiateRecovery` exists in Repo, and then update Repo.
        // Or better, I'll just implement the logic here if I can? No, UseCase shouldn't touch Firestore.
        
        // Let's assume I'll update Repo.
        groupRepository.initiateRecovery(groupId)
    }

    fun getRecoveryProgress(groupId: String): Flow<Int> {
        return groupRepository.getRecoveryShares(groupId).map { it.size }
    }

    suspend fun finalizeRecovery(groupId: String) {
        val sharesBase64 = groupRepository.getRecoveryShares(groupId).first()
        val group = groupRepository.getGroupDetails(groupId).first()
        val threshold = (group.members.size / 2) + 1

        if (sharesBase64.size + 1 < threshold) { // +1 for admin's own share
             throw IllegalStateException("Not enough shares to recover key.")
        }

        // 1. Decrypt shares
        val shares = sharesBase64.map { 
            cryptoManager.decrypt(it) 
        }.toMutableList()

        // 2. Get Admin's own share
        // We need a method to get my own share. `getMyAssignment` returns `AssignmentTransactionData` which has `encryptedKey` but not the share.
        // The share is stored in `groups/{groupId}/givers/{adminId}` as `encryptedShare`.
        // `getMyAssignment` in Repo currently returns `AssignmentTransactionData` which has `encryptedReceiverId` and `encryptedKey`.
        // I need to update `AssignmentTransactionData` or `getMyAssignment` to return the share too.
        // Let's update `AssignmentTransactionData` to include `encryptedShare` (optional?).
        // Or add `getMyShare` to Repo.
        
        val myShareEncrypted = groupRepository.getMyShare(groupId)
        val myShare = cryptoManager.decrypt(myShareEncrypted)
        shares.add(myShare)

        // 3. Reconstruct Master Key
        val masterKeyBytes = cryptoManager.combineShares(shares)
        val masterKey = javax.crypto.spec.SecretKeySpec(masterKeyBytes, "AES")

        // 4. Decrypt Master List
        val encryptedMasterListBase64 = groupRepository.getEncryptedMasterList(groupId).getOrThrow()
        val encryptedMasterList = Base64.decode(encryptedMasterListBase64, Base64.NO_WRAP)
        val masterListBytes = cryptoManager.decrypt(encryptedMasterList, masterKey)
        val masterListJson = String(masterListBytes)

        // 5. Finalize
        groupRepository.finalizeRecovery(groupId, masterListJson)
    }
}
