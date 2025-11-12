/*
 * Licensed under the EUPL, Version 1.2.
 * You may obtain a copy of the Licence at:
 * https://joinup.ec.europa.eu/collection/eupl/eupl-text-eupl-12
 */

package it.alby02.secretsanta.data.repository

import com.google.firebase.auth.auth
import com.google.firebase.firestore.firestore
import com.google.firebase.Firebase
import it.alby02.secretsanta.data.model.Group
import it.alby02.secretsanta.domain.repository.GroupRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.tasks.await

// This is a STUBBED repository.
class GroupRepositoryImpl : GroupRepository {

    private val auth = Firebase.auth
    private val firestore = Firebase.firestore

    override fun getMyGroups(): Flow<List<Group>> {
        // TODO: Implement a real-time Firestore query
        // "where 'members' array-contains auth.currentUser.uid"
        return flow {
            // Emit a hardcoded list for preview
            emit(
                listOf(
                    Group(id = "1", groupName = "Office Party", joinCode = "ABC-123"),
                    Group(id = "2", groupName = "Family Christmas", joinCode = "XYZ-789")
                )
            )
        }
    }

    override fun getGroupDetails(groupId: String): Flow<Group> {
        // TODO: Implement a real-time Firestore document listener
        return flow {
            emit(Group(id = groupId, groupName = "Office Party", joinCode = "ABC-123"))
        }
    }

    override suspend fun createGroup(groupName: String) {
        // TODO: Implement this logic (this replaces CreateGroupViewModel)
        val adminId = auth.currentUser?.uid ?: return
        val joinCode = generateJoinCode()
        val newGroup = Group(
            groupName = groupName,
            adminId = adminId,
            joinCode = joinCode,
            members = listOf(adminId)
        )
        firestore.collection("groups").add(newGroup).await()
        println("STUB: Creating group '$groupName'")
    }

    override suspend fun joinGroup(joinCode: String) {
        // TODO: Implement this logic
        // 1. Query firestore for "where 'joinCode' == joinCode"
        // 2. Get the group document
        // 3. Add current user's UID to the 'members' array
        println("STUB: Joining group with code '$joinCode'")
    }

    private fun generateJoinCode(): String {
        val chars = ('A'..'Z').toList()
        val digits = ('0'..'9').toList()
        return (1..3).map { chars.random() }.joinToString("") +
                "-" +
                (1..3).map { digits.random() }.joinToString("")
    }
}