package com.univesp.lumme.presentation.ui.devices

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.univesp.lumme.domain.model.Device
import com.univesp.lumme.domain.model.DeviceType
import com.univesp.lumme.domain.repository.DeviceRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class DevicesViewModel @Inject constructor(
    private val repo: DeviceRepository
) : ViewModel() {

    private val _loading = MutableStateFlow(false)
    private val _error = MutableStateFlow<String?>(null)
    private val _togglingIds = MutableStateFlow<Set<String>>(emptySet())
    private val _adding = MutableStateFlow(false)

    val uiState: StateFlow<DevicesUiState> = combine(
        repo.observeDevices(),
        _loading,
        _error,
        _togglingIds,
        _adding
    ) { devices, loading, error, toggling, adding ->
        DevicesUiState(
            devicesByRoom = devices.groupBy { it.roomName ?: "Sem cômodo" },
            loading = loading,
            error = error,
            togglingIds = toggling,
            adding = adding
        )
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        DevicesUiState()
    )

    init { refresh() }

    fun refresh() {
        _loading.value = true
        viewModelScope.launch {
            repo.refreshDevices().onFailure { e ->
                // Silencia 401 — ocorre quando a chamada começa antes do JWT ser salvo
                if (e.message?.contains("401") != true) {
                    _error.value = e.message
                }
            }
            _loading.value = false
        }
    }

    fun toggle(device: Device) {
        _togglingIds.update { it + device.id }
        viewModelScope.launch {
            repo.toggleDevice(device.id, !device.status.isOn)
                .onFailure { e ->
                    _error.value = "Falha ao alterar ${device.label}: ${e.message}"
                }
            _togglingIds.update { it - device.id }
        }
    }

    fun addDevice(label: String, type: DeviceType, roomName: String?) {
        if (label.isBlank()) {
            _error.value = "Informe o nome do dispositivo"
            return
        }
        _adding.value = true
        viewModelScope.launch {
            repo.addDevice(label.trim(), type, roomName?.trim())
                .onSuccess {
                    // Atualiza a lista após criar
                    repo.refreshDevices()
                }
                .onFailure { e ->
                    _error.value = "Falha ao adicionar dispositivo: ${e.message}"
                }
            _adding.value = false
        }
    }

    fun deleteDevice(deviceId: String) {
        viewModelScope.launch {
            repo.deleteDevice(deviceId).onFailure { e ->
                _error.value = "Falha ao remover: ${e.message}"
            }
        }
    }

    fun onErrorShown() = _error.update { null }
}

data class DevicesUiState(
    val devicesByRoom: Map<String, List<Device>> = emptyMap(),
    val loading: Boolean = false,
    val error: String? = null,
    val togglingIds: Set<String> = emptySet(),
    val adding: Boolean = false
)
