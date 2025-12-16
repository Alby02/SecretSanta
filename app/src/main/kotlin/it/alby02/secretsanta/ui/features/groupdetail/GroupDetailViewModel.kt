/*
 * Licensed under the EUPL, Version 1.2.
 * You may obtain a copy of the Licence at:
 * https://joinup.ec.europa.eu/collection/eupl/eupl-text-eupl-12
 */

package it.alby02.secretsanta.ui.features.groupdetail


import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.Firebase
import com.google.firebase.auth.auth

import it.alby02.secretsanta.data.model.Group
import it.alby02.secretsanta.data.security.CryptoManager
import it.alby02.secretsanta.domain.repository.GroupRepository
import it.alby02.secretsanta.domain.usecase.AdminKeyRecoveryUseCase
import it.alby02.secretsanta.domain.usecase.InitiateMatchingUseCase
import it.alby02.secretsanta.domain.usecase.ViewAssignmentUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.koin.android.annotation.KoinViewModel
import org.koin.core.annotation.InjectedParam

data class GroupDetailUiState(
    val isLoading: Boolean = true,
    val group: Group? = null,
    val participants: List<String> = emptyList(), // TODO: Use UserProfile
    val errorMessage: String? = null,
    val assignment: String? = null,
    val isMatching: Boolean = false,
    val isRecovering: Boolean = false,
    val recoveryProgress: Int = 0,
    val recoveryThreshold: Int = 0
)

@KoinViewModel
class GroupDetailViewModel(
    @InjectedParam private val groupId: String,
    private val groupRepository: GroupRepository,
    private val initiateMatchingUseCase: InitiateMatchingUseCase,
    private val adminKeyRecoveryUseCase: AdminKeyRecoveryUseCase,
    private val viewAssignmentUseCase: ViewAssignmentUseCase,
    private val cryptoManager: CryptoManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(GroupDetailUiState())
    val uiState: StateFlow<GroupDetailUiState> = _uiState.asStateFlow()

    private val currentUser = Firebase.auth.currentUser

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
                            participants = group.members,
                            recoveryThreshold = (group.members.size / 2) + 1
                        )
                    }
                    
                    if (group.state == "recovery" && group.adminId == currentUser?.uid) {
                        monitorRecoveryProgress()
                    }
                }
        }
    }

    fun startMatching() {
        viewModelScope.launch {
            _uiState.update { it.copy(isMatching = true) }
            try {
                initiateMatchingUseCase(groupId)
            } catch (e: Exception) {
                _uiState.update { it.copy(errorMessage = "Matching failed: ${e.message}") }
            } finally {
                _uiState.update { it.copy(isMatching = false) }
            }
        }
    }

    fun viewAssignment() {
        viewModelScope.launch {
            try {
                val receiverName = viewAssignmentUseCase(groupId)
                _uiState.update { it.copy(assignment = receiverName) }
            } catch (e: Exception) {
                _uiState.update { it.copy(errorMessage = "Failed to load assignment: ${e.message}") }
            }
        }
    }

    fun initiateRecovery() {
        viewModelScope.launch {
            try {
                adminKeyRecoveryUseCase.initiateRecovery(groupId)
            } catch (e: Exception) {
                _uiState.update { it.copy(errorMessage = "Failed to initiate recovery: ${e.message}") }
            }
        }
    }

    private fun monitorRecoveryProgress() {
        viewModelScope.launch {
            adminKeyRecoveryUseCase.getRecoveryProgress(groupId).collect { progress ->
                _uiState.update { it.copy(recoveryProgress = progress) }
            }
        }
    }

    fun finalizeRecovery() {
        viewModelScope.launch {
            _uiState.update { it.copy(isRecovering = true) }
            try {
                adminKeyRecoveryUseCase.finalizeRecovery(groupId)
            } catch (e: Exception) {
                _uiState.update { it.copy(errorMessage = "Recovery failed: ${e.message}") }
            } finally {
                _uiState.update { it.copy(isRecovering = false) }
            }
        }
    }

    fun contributeToRecovery() {
        viewModelScope.launch {
            try {
                // 1. Get my share
                val myShareEncrypted = groupRepository.getMyShare(groupId)
                val myShare = cryptoManager.decrypt(myShareEncrypted)
                
                // 2. Get Admin's public key
                val adminId = uiState.value.group?.adminId ?: return@launch
                val adminProfile = groupRepository.getUsers(listOf(adminId)).first().firstOrNull() ?: throw Exception("Admin not found")
                
                // 3. Re-encrypt for admin
                val encryptedForAdmin = cryptoManager.encrypt(myShare, adminProfile.publicKey)
                
                // 4. Submit
                groupRepository.submitRecoveryShare(groupId, encryptedForAdmin)
            } catch (e: Exception) {
                _uiState.update { it.copy(errorMessage = "Failed to contribute: ${e.message}") }
            }
        }
    }
}