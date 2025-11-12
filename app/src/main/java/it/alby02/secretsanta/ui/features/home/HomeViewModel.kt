/*
 * Licensed under the EUPL, Version 1.2.
 * You may obtain a copy of the Licence at:
 * https://joinup.ec.europa.eu/collection/eupl/eupl-text-eupl-12
 */

package it.alby02.secretsanta.ui.features.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import it.alby02.secretsanta.data.model.Group
import it.alby02.secretsanta.data.repository.GroupRepositoryImpl
import it.alby02.secretsanta.domain.repository.GroupRepository
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

    // TODO: Use Dependency Injection to provide this
    private val groupRepository: GroupRepository = GroupRepositoryImpl()

    init {
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
            try {
                groupRepository.createGroup(groupName)
                _uiState.update { it.copy(showCreateDialog = false) }
            } catch (e: Exception) {
                _uiState.update { it.copy(errorMessage = e.message) }
            }
        }
    }

    fun joinGroup(joinCode: String) {
        viewModelScope.launch {
            try {
                groupRepository.joinGroup(joinCode)
                _uiState.update { it.copy(showJoinDialog = false) }
            } catch (e: Exception) {
                _uiState.update { it.copy(errorMessage = e.message) }
            }
        }
    }
}