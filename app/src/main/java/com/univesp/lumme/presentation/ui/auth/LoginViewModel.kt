package com.univesp.lumme.presentation.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.univesp.lumme.domain.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class LoginViewModel @Inject constructor(
    private val authRepo: AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(LoginUiState())
    val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()

    fun onConnectClick() {
        _uiState.update { it.copy(loading = true, error = null) }
        viewModelScope.launch {
            authRepo.startOAuthFlow()
                .onSuccess { url ->
                    _uiState.update { it.copy(loading = false, authorizeUrl = url) }
                }
                .onFailure { e ->
                    _uiState.update { it.copy(loading = false, error = e.message ?: "Erro desconhecido") }
                }
        }
    }

    /** Chamar depois que a Activity lançar o CustomTab — limpa a URL para não reabrir. */
    fun onUrlConsumed() {
        _uiState.update { it.copy(authorizeUrl = null) }
    }

    fun clearError() = _uiState.update { it.copy(error = null) }
}

data class LoginUiState(
    val loading: Boolean = false,
    val authorizeUrl: String? = null,
    val error: String? = null
)
