package com.univesp.lumme.presentation.ui.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.univesp.lumme.domain.model.ConsumptionReading
import com.univesp.lumme.domain.model.Device
import com.univesp.lumme.domain.model.Goal
import com.univesp.lumme.domain.model.Granularity
import com.univesp.lumme.domain.repository.ConsumptionRepository
import com.univesp.lumme.domain.repository.DeviceRepository
import com.univesp.lumme.domain.repository.GoalRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.temporal.ChronoUnit
import javax.inject.Inject

@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val deviceRepo: DeviceRepository,
    private val consumptionRepo: ConsumptionRepository,
    private val goalRepo: GoalRepository
) : ViewModel() {

    private val _consumption = MutableStateFlow<List<ConsumptionReading>>(emptyList())
    private val _loading = MutableStateFlow(true)
    private val _error = MutableStateFlow<String?>(null)

    val uiState: StateFlow<DashboardUiState> = combine(
        deviceRepo.observeDevices(),
        goalRepo.observeActiveGoal(),
        _consumption,
        _loading,
        _error
    ) { devices, goal, consumption, loading, error ->
        DashboardUiState(
            loading = loading,
            devices = devices,
            goal = goal,
            weeklyConsumption = consumption,
            totalKwhWeek = consumption.sumOf { it.energyKwh },
            estimatedCostWeek = consumption.sumOf { it.estimatedCost ?: 0.0 },
            devicesOnline = devices.count { it.status.online },
            devicesOn = devices.count { it.status.isOn },
            error = error
        )
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        DashboardUiState(loading = true)
    )

    init { refresh() }

    fun refresh() {
        _loading.value = true
        _error.value = null
        viewModelScope.launch {
            val now = Instant.now()
            val weekAgo = now.minus(7, ChronoUnit.DAYS)

            // Silencia 401 — acontece quando a chamada inicia antes do JWT estar salvo
            deviceRepo.refreshDevices().onFailure { e ->
                if (e.message?.contains("401") != true) {
                    _error.value = e.message
                }
            }

            consumptionRepo.getConsumption(weekAgo, now, Granularity.DAY)
                .onSuccess { _consumption.value = it }
                .onFailure { e ->
                    if (e.message?.contains("401") != true) {
                        _error.value = e.message
                    }
                }

            _loading.value = false
        }
    }

    fun onErrorShown() = _error.update { null }
}

data class DashboardUiState(
    val loading: Boolean = false,
    val devices: List<Device> = emptyList(),
    val goal: Goal? = null,
    val weeklyConsumption: List<ConsumptionReading> = emptyList(),
    val totalKwhWeek: Double = 0.0,
    val estimatedCostWeek: Double = 0.0,
    val devicesOnline: Int = 0,
    val devicesOn: Int = 0,
    val error: String? = null
)
