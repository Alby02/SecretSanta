/*
 * Licensed under the EUPL, Version 1.2.
 * You may obtain a copy of the Licence at:
 * https://joinup.ec.europa.eu/collection/eupl/eupl-text-eupl-12
 */

package it.alby02.secretsanta.data.repository

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.snapshots
import com.google.firebase.firestore.toObject
import it.alby02.secretsanta.data.model.Group
import it.alby02.secretsanta.data.model.UserProfile
import it.alby02.secretsanta.domain.repository.GroupRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

@OptIn(ExperimentalCoroutinesApi::class)
class GroupRepositoryImpl(
    private val firestore: FirebaseFirestore,
    private val auth: FirebaseAuth
) : GroupRepository {

    private val currentUser by lazy { auth.currentUser ?: throw IllegalStateException("User not logged in") }

    override fun getMyGroups(): Flow<List<Group>> {
        val userGroupsDoc = firestore.collection("usersGroups").document(currentUser.uid)

        return userGroupsDoc.snapshots().flatMapLatest { userDocSnapshot ->
            val groupIds = userDocSnapshot.get("groupIds") as? List<String> ?: emptyList()
            if (groupIds.isEmpty()) {
                flow { emit(emptyList()) }
            } else {
                firestore.collection("groups")
                    .whereIn("id", groupIds)
                    .snapshots()
                    .map { querySnapshot ->
                        querySnapshot.documents.mapNotNull { it.toObject<Group>()?.copy(id = it.id) }
                    }
            }
        }
    }

    override fun getGroupDetails(groupId: String): Flow<Group> {
        return firestore.collection("groups").document(groupId).snapshots().map { snapshot ->
            snapshot.toObject<Group>()?.copy(id = snapshot.id) ?: throw Exception("Group not found")
        }
    }

    override suspend fun createGroup(groupName: String): Result<Unit> = runCatching {
        val newGroupRef = firestore.collection("groups").document()
        val joinCode = generateJoinCode() // Simple random code

        val newGroup = Group(
            id = newGroupRef.id,
            groupName = groupName,
            adminId = currentUser.uid,
            joinCode = joinCode,
            members = listOf(currentUser.uid),
            state = "pending"
        )

        firestore.batch().apply {
            // Create the new group
            set(newGroupRef, newGroup)
            // Add the group ID to the user's group list
            val userGroupsRef = firestore.collection("usersGroups").document(currentUser.uid)
            update(userGroupsRef, "groupIds", FieldValue.arrayUnion(newGroupRef.id))
        }.commit().await()
    }

    override suspend fun joinGroup(joinCode: String): Result<Unit> = runCatching {
        val query = firestore.collection("groups").whereEqualTo("joinCode", joinCode).limit(1)
        val snapshot = query.get().await()
        val groupDoc = snapshot.documents.firstOrNull() ?: throw Exception("Group not found.")

        val groupId = groupDoc.id
        val group = groupDoc.toObject<Group>()!!

        if (group.members.contains(currentUser.uid)) {
            throw Exception("You are already a member of this group.")
        }

        firestore.batch().apply {
            // Add the user to the group's member list
            update(groupDoc.reference, "members", FieldValue.arrayUnion(currentUser.uid))
            // Add the group ID to the user's group list
            val userGroupsRef = firestore.collection("usersGroups").document(currentUser.uid)
            update(userGroupsRef, "groupIds", FieldValue.arrayUnion(groupId))
        }.commit().await()
    }

    override suspend fun initiateMatching(
        groupId: String,
        assignments: List<it.alby02.secretsanta.domain.repository.AssignmentTransactionData>,
        keyShares: List<it.alby02.secretsanta.domain.repository.KeyShareTransactionData>,
        encryptedMasterList: ByteArray
    ): Result<Unit> = runCatching {
        val groupRef = firestore.collection("groups").document(groupId)
        val masterListRef = firestore.collection("groupsMasterLists").document(groupId)

        firestore.runBatch { batch ->
            // 1. Update group state
            batch.update(groupRef, "state", "assigned")

            // 2. Save encrypted master list
            val masterListData = mapOf("masterList" to android.util.Base64.encodeToString(encryptedMasterList, android.util.Base64.NO_WRAP))
            batch.set(masterListRef, masterListData)

            // 3. Save individual assignments and key shares
            assignments.forEach { assignment ->
                val giverRef = groupRef.collection("givers").document(assignment.giverId)
                val share = keyShares.find { it.memberId == assignment.giverId }
                    ?: throw IllegalStateException("Missing key share for giver ${assignment.giverId}")

                val data = mapOf(
                    "encryptedReceiverId" to android.util.Base64.encodeToString(assignment.encryptedReceiverId, android.util.Base64.NO_WRAP),
                    "encryptedKey" to assignment.encryptedKey,
                    "encryptedShare" to share.encryptedShare
                )
                batch.set(giverRef, data)
            }
        }.await()
    }

    override suspend fun submitRecoveryShare(groupId: String, encryptedShareForAdmin: String): Result<Unit> = runCatching {
        val recoveryRef = firestore.collection("groupsMasterLists").document(groupId)
            .collection("recoverySubmissions").document(currentUser.uid)
        
        val data = mapOf("encryptedShareForAdmin" to encryptedShareForAdmin)
        recoveryRef.set(data).await()
    }

    override fun getRecoveryShares(groupId: String): Flow<List<String>> {
        return firestore.collection("groupsMasterLists").document(groupId)
            .collection("recoverySubmissions")
            .snapshots()
            .map { snapshot ->
                snapshot.documents.mapNotNull { it.getString("encryptedShareForAdmin") }
            }
    }

    override suspend fun finalizeRecovery(groupId: String, decryptedMasterListJSON: String): Result<Unit> = runCatching {
        val groupRef = firestore.collection("groups").document(groupId)
        val masterListRef = firestore.collection("groupsMasterLists").document(groupId)
        val recoveryCollectionRef = masterListRef.collection("recoverySubmissions")

        firestore.runBatch { batch ->
            // 1. Update group state
            batch.update(groupRef, "state", "completed")

            // 2. Update master list with decrypted JSON
            batch.update(masterListRef, "masterList", decryptedMasterListJSON)

            // 3. Delete recovery submissions (optional cleanup, but good practice)
            // Note: Firestore batch has a limit of 500 operations. If there are many members, this might need a separate delete loop.
            // For now, we'll assume a reasonable group size.
        }.await()
        
        // Delete collection manually since batch delete of collection isn't direct
        val recoveryDocs = recoveryCollectionRef.get().await()
        recoveryDocs.documents.forEach { it.reference.delete() }
    }

    override suspend fun getMyAssignment(groupId: String): Result<it.alby02.secretsanta.domain.repository.AssignmentTransactionData> = runCatching {
        val doc = firestore.collection("groups").document(groupId)
            .collection("givers").document(currentUser.uid)
            .get().await()

        if (!doc.exists()) throw Exception("Assignment not found")

        val encryptedReceiverIdBase64 = doc.getString("encryptedReceiverId") ?: throw Exception("Invalid data")
        val encryptedKey = doc.getString("encryptedKey") ?: throw Exception("Invalid data")
        // We don't need the share here
        
        it.alby02.secretsanta.domain.repository.AssignmentTransactionData(
            giverId = currentUser.uid,
            encryptedReceiverId = android.util.Base64.decode(encryptedReceiverIdBase64, android.util.Base64.NO_WRAP),
            encryptedKey = encryptedKey
        )
    }

    override suspend fun getEncryptedMasterList(groupId: String): Result<String> = runCatching {
        val doc = firestore.collection("groupsMasterLists").document(groupId).get().await()
        doc.getString("masterList") ?: throw Exception("Master list not found")
    }

    override fun getUsers(userIds: List<String>): Flow<List<UserProfile>> {
        if (userIds.isEmpty()) return flow { emit(emptyList()) }
        // Firestore 'in' query is limited to 10 items. We need to chunk it.
        // For simplicity in this demo, we'll assume < 10 or just fetch all (inefficient) or chunk it.
        // Let's do a simple chunking.
        
        return flow {
             val chunks = userIds.chunked(10)
             val allUsers = mutableListOf<UserProfile>()
             
             for (chunk in chunks) {
                 val snapshot = firestore.collection("usersPublic").whereIn(com.google.firebase.firestore.FieldPath.documentId(), chunk).get().await()
                 allUsers.addAll(snapshot.toObjects(UserProfile::class.java)) // This might fail if toObject isn't perfect with IDs, let's map manually if needed or trust toObject with @DocumentId if used
             }
             emit(allUsers)
        }
    }

    override suspend fun initiateRecovery(groupId: String): Result<Unit> = runCatching {
        val groupRef = firestore.collection("groups").document(groupId)
        groupRef.update("state", "recovery").await()
    }

    override suspend fun getMyShare(groupId: String): String {
        val doc = firestore.collection("groups").document(groupId)
            .collection("givers").document(currentUser.uid)
            .get().await()
        
        return doc.getString("encryptedShare") ?: throw Exception("Share not found")
    }

    private fun generateJoinCode(): String {
        val chars = ('A'..'Z') + ('0'..'9')
        return (1..6)
            .map { chars.random() }
            .joinToString("")
    }
}