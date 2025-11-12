/*
 * Licensed under the EUPL, Version 1.2.
 * You may obtain a copy of the Licence at:
 * https://joinup.ec.europa.eu/collection/eupl/eupl-text-eupl-12
 */

package it.alby02.secretsanta.ui.features.groupdetail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import it.alby02.secretsanta.data.model.Group
import it.alby02.secretsanta.data.repository.GroupRepositoryImpl
import it.alby02.secretsanta.domain.repository.GroupRepository
import it.alby02.secretsanta.ui.navigation.AppDestinations
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class GroupDetailUiState(
    val isLoading: Boolean = true,
    val group: Group? = null,
    val participants: List<String> = emptyList(), // TODO: Use UserProfile
    val errorMessage: String? = null
)

class GroupDetailViewModel(
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val groupId: String = savedStateHandle.get<String>(AppDestinations.GROUP_ID_ARG)!!

    private val _uiState = MutableStateFlow(GroupDetailUiState())
    val uiState: StateFlow<GroupDetailUiState> = _uiState.asStateFlow()

    // TODO: Use Dependency Injection
    private val groupRepository: GroupRepository = GroupRepositoryImpl()

    init {
        loadGroupDetails()
    }

    private fun loadGroupDetails() {
        viewModelScope.launch {
            groupRepository.getGroupDetails(groupId)
                .catch { e ->
                    _uiState.update { it.copy(isLoading = false, errorMessage = e.message) }
                }
                .collect { group ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            group = group,
                            participants = group.members // TODO: Fetch full profiles
                        )
                    }
                }
        }
    }
}