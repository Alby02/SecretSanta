/*
 * Licensed under the EUPL, Version 1.2.
 * You may obtain a copy of the Licence at:
 * https://joinup.ec.europa.eu/collection/eupl/eupl-text-eupl-12
 */

package it.alby02.secretsanta.ui.features.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.auth
import com.google.firebase.firestore.firestore
import com.google.firebase.Firebase
import it.alby02.secretsanta.data.model.Group
import it.alby02.secretsanta.data.repository.GroupRepositoryImpl
import it.alby02.secretsanta.domain.repository.GroupRepository
import it.alby02.secretsanta.domain.usecase.CreateGroupUseCase
import it.alby02.secretsanta.domain.usecase.JoinGroupUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class HomeUiState(
    val isLoading: Boolean = true,
    val groups: List<Group> = emptyList(),
    val isFabExpanded: Boolean = false,
    val showCreateDialog: Boolean = false,
    val showJoinDialog: Boolean = false,
    val errorMessage: String? = null
)

class HomeViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    private val groupRepository: GroupRepository
    private val createGroupUseCase: CreateGroupUseCase
    private val joinGroupUseCase: JoinGroupUseCase

    init {
        // In a real app, you would use Hilt/Dagger for dependency injection
        val auth = Firebase.auth
        val firestore = Firebase.firestore
        groupRepository = GroupRepositoryImpl(firestore, auth)
        createGroupUseCase = CreateGroupUseCase(groupRepository)
        joinGroupUseCase = JoinGroupUseCase(groupRepository)

        loadGroups()
    }

    private fun loadGroups() {
        viewModelScope.launch {
            groupRepository.getMyGroups()
                .catch { e ->
                    _uiState.update { it.copy(isLoading = false, errorMessage = e.message) }
                }
                .collect { groups ->
                    _uiState.update { it.copy(isLoading = false, groups = groups) }
                }
        }
    }

    fun onFabClicked() {
        _uiState.update { it.copy(isFabExpanded = !it.isFabExpanded) }
    }

    fun onCreateGroupClicked() {
        _uiState.update { it.copy(isFabExpanded = false, showCreateDialog = true) }
    }

    fun onJoinGroupClicked() {
        _uiState.update { it.copy(isFabExpanded = false, showJoinDialog = true) }
    }

    fun onCreateDialogDismiss() {
        _uiState.update { it.copy(showCreateDialog = false) }
    }

    fun onJoinDialogDismiss() {
        _uiState.update { it.copy(showJoinDialog = false) }
    }

    fun createGroup(groupName: String) {
        viewModelScope.launch {
            createGroupUseCase(groupName)
                .onSuccess {
                    // Dialog is dismissed, and list will auto-update thanks to the flow
                    _uiState.update { it.copy(showCreateDialog = false) }
                }
                .onFailure {
                    _uiState.update { it.copy(errorMessage = it.errorMessage, showCreateDialog = false) }
                }
        }
    }

    fun joinGroup(joinCode: String) {
        viewModelScope.launch {
            joinGroupUseCase(joinCode)
                .onSuccess {
                    // Dialog is dismissed, and list will auto-update thanks to the flow
                    _uiState.update { it.copy(showJoinDialog = false) }
                }
                .onFailure {
                    _uiState.update { it.copy(errorMessage = it.errorMessage, showJoinDialog = false) }
                }
        }
    }
}