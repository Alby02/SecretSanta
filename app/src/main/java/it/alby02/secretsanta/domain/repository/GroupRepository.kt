/*
 * Licensed under the EUPL, Version 1.2.
 * You may obtain a copy of the Licence at:
 * https://joinup.ec.europa.eu/collection/eupl/eupl-text-eupl-12
 */

package it.alby02.secretsanta.domain.repository

import it.alby02.secretsanta.data.model.Group
import kotlinx.coroutines.flow.Flow

interface GroupRepository {
    fun getMyGroups(): Flow<List<Group>>
    fun getGroupDetails(groupId: String): Flow<Group>
    suspend fun createGroup(groupName: String)
    suspend fun joinGroup(joinCode: String)
}