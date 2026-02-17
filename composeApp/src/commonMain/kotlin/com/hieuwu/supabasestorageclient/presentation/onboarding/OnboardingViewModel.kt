package com.hieuwu.supabasestorageclient.presentation.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hieuwu.supabasestorageclient.domain.repository.OnboardingRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * ViewModel for managing onboarding flow state and actions
 */
class OnboardingViewModel(
    private val onboardingRepository: OnboardingRepository
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(OnboardingUiState())
    val uiState: StateFlow<OnboardingUiState> = _uiState.asStateFlow()
    
    private val _isCompleted = MutableStateFlow(false)
    val isCompleted: StateFlow<Boolean> = _isCompleted.asStateFlow()
    
    init {
        checkOnboardingStatus()
    }
    
    private fun checkOnboardingStatus() {
        _isCompleted.value = onboardingRepository.isOnboardingCompleted()
    }
    
    fun onPageChanged(page: Int) {
        _uiState.update { it.copy(currentPage = page) }
    }
    
    fun onSecurityWarningAcknowledged(acknowledged: Boolean) {
        _uiState.update { it.copy(securityWarningAcknowledged = acknowledged) }
    }
    
    fun completeOnboarding() {
        viewModelScope.launch {
            onboardingRepository.markOnboardingCompleted()
            _isCompleted.value = true
        }
    }
    
    fun canProceedFromCurrentPage(): Boolean {
        return when (_uiState.value.currentPage) {
            2 -> _uiState.value.securityWarningAcknowledged // Security warning page
            else -> true
        }
    }
}
