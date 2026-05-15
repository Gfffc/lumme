package com.univesp.lumme.domain.repository

import com.univesp.lumme.domain.model.ConsumptionReading
import com.univesp.lumme.domain.model.Device
import com.univesp.lumme.domain.model.DeviceType
import com.univesp.lumme.domain.model.Goal
import com.univesp.lumme.domain.model.Granularity
import kotlinx.coroutines.flow.Flow
import java.time.Instant

interface AuthRepository {
    val isAuthenticated: Flow<Boolean>
    suspend fun startOAuthFlow(): Result<String>
    suspend fun exchangeCode(code: String, state: String): Result<Unit>
    suspend fun logout()
}

interface DeviceRepository {
    fun observeDevices(): Flow<List<Device>>
    suspend fun refreshDevices(): Result<Unit>
    suspend fun toggleDevice(deviceId: String, turnOn: Boolean): Result<Unit>
    suspend fun getDevice(id: String): Device?
    suspend fun addDevice(label: String, type: DeviceType, roomName: String?): Result<Unit>
    suspend fun deleteDevice(deviceId: String): Result<Unit>
}

interface ConsumptionRepository {
    suspend fun getConsumption(from: Instant, to: Instant, granularity: Granularity): Result<List<ConsumptionReading>>
    suspend fun getDeviceConsumption(deviceId: String, from: Instant, to: Instant): Result<List<ConsumptionReading>>
}

interface GoalRepository {
    fun observeActiveGoal(): Flow<Goal?>
    suspend fun saveGoal(targetKwh: Double): Result<Unit>
}
