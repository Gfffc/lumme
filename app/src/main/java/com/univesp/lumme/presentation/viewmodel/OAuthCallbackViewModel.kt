package com.univesp.lumme.presentation.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.univesp.lumme.domain.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class OAuthCallbackViewModel @Inject constructor(
    private val authRepo: AuthRepository
) : ViewModel() {

    private val _state = MutableStateFlow<AuthState>(AuthState.Idle)
    val state: StateFlow<AuthState> = _state.asStateFlow()

    fun onCallback(code: String?, state: String?, error: String?) {
        Log.d("LummeOAuth", "VM.onCallback code=${code?.take(20)} state=$state error=$error")
        when {
            error != null -> _state.value = AuthState.Error("Autorização negada: $error")
            code.isNullOrBlank() || state.isNullOrBlank() ->
                _state.value = AuthState.Error("Resposta inválida do provedor OAuth")
            else -> exchange(code, state)
        }
    }

    private fun exchange(code: String, state: String) {
        _state.value = AuthState.Loading
        viewModelScope.launch {
            authRepo.exchangeCode(code, state)
                .onSuccess {
                    Log.d("LummeOAuth", "exchange OK — JWT salvo")
                    _state.value = AuthState.Authenticated
                }
                .onFailure {
                    Log.e("LummeOAuth", "exchange falhou", it)
                    _state.value = AuthState.Error(it.message ?: "Falha ao autenticar")
                }
        }
    }

    fun reset() { _state.value = AuthState.Idle }
}

sealed interface AuthState {
    data object Idle : AuthState
    data object Loading : AuthState
    data object Authenticated : AuthState
    data class Error(val message: String) : AuthState
}
