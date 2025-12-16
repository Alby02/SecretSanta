/*
 * Licensed under the EUPL, Version 1.2.
 * You may obtain a copy of the Licence at:
 * https://joinup.ec.europa.eu/collection/eupl/eupl-text-eupl-12
 */

package it.alby02.secretsanta.domain.usecase

import it.alby02.secretsanta.domain.repository.GroupRepository
import org.koin.core.annotation.Factory

@Factory
class JoinGroupUseCase(private val groupRepository: GroupRepository) {

    suspend operator fun invoke(joinCode: String): Result<Unit> {
        if (joinCode.isBlank()) {
            return Result.failure(IllegalArgumentException("Join code cannot be empty."))
        }
        return groupRepository.joinGroup(joinCode)
    }
}