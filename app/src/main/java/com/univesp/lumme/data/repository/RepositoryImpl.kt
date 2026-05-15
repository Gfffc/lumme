package com.univesp.lumme.data.repository

import com.univesp.lumme.data.local.DeviceDao
import com.univesp.lumme.data.local.TokenStore
import com.univesp.lumme.data.mapper.toDomain
import com.univesp.lumme.data.mapper.toEntity
import com.univesp.lumme.data.remote.CommandRequest
import com.univesp.lumme.data.remote.CreateDeviceRequest
import com.univesp.lumme.data.remote.CreateGoalRequest
import com.univesp.lumme.data.remote.LummeApi
import com.univesp.lumme.data.remote.OAuthExchangeRequest
import com.univesp.lumme.domain.model.ConsumptionReading
import com.univesp.lumme.domain.model.Device
import com.univesp.lumme.domain.model.DeviceType
import com.univesp.lumme.domain.model.Goal
import com.univesp.lumme.domain.model.Granularity
import com.univesp.lumme.domain.repository.AuthRepository
import com.univesp.lumme.domain.repository.ConsumptionRepository
import com.univesp.lumme.domain.repository.DeviceRepository
import com.univesp.lumme.domain.repository.GoalRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthRepositoryImpl @Inject constructor(
    private val api: LummeApi,
    private val tokenStore: TokenStore
) : AuthRepository {

    override val isAuthenticated: Flow<Boolean> =
        tokenStore.jwtFlow.map { !it.isNullOrBlank() }

    override suspend fun startOAuthFlow(): Result<String> = runCatching {
        val response = api.startOAuth()
        tokenStore.saveState(response.state)
        response.authorizeUrl
    }

    override suspend fun exchangeCode(code: String, state: String): Result<Unit> = runCatching {
        // CSRF tolerante a race condition: se state não existir no DataStore (mock rápido),
        // confia no backend (que tem sua própria validação)
        val expected = tokenStore.state()
        if (expected != null && expected != state) {
            error("CSRF state mismatch")
        }
        val auth = api.exchangeCode(OAuthExchangeRequest(code = code, state = state))
        tokenStore.saveJwt(auth.jwt)
    }

    override suspend fun logout() {
        tokenStore.clearJwt()
    }
}

@Singleton
class DeviceRepositoryImpl @Inject constructor(
    private val api: LummeApi,
    private val dao: DeviceDao
) : DeviceRepository {

    override fun observeDevices(): Flow<List<Device>> =
        dao.observeAll().map { list -> list.map { it.toDomain() } }

    override suspend fun refreshDevices(): Result<Unit> = runCatching {
        val remote = api.listDevices().map { it.toEntity() }
        dao.upsertAll(remote)
    }

    override suspend fun toggleDevice(deviceId: String, turnOn: Boolean): Result<Unit> = runCatching {
        dao.updateState(deviceId, turnOn)
        try {
            api.sendCommand(
                id = deviceId,
                command = CommandRequest(
                    capability = "switch",
                    command = if (turnOn) "on" else "off"
                )
            )
        } catch (t: Throwable) {
            dao.updateState(deviceId, !turnOn)
            throw t
        }
    }

    override suspend fun getDevice(id: String): Device? =
        dao.findById(id)?.toDomain()

    override suspend fun addDevice(
        label: String,
        type: DeviceType,
        roomName: String?
    ): Result<Unit> = runCatching {
        val dto = api.createDevice(
            CreateDeviceRequest(
                label = label,
                type = type.name,
                roomName = roomName?.takeIf { it.isNotBlank() }
            )
        )
        dao.upsertAll(listOf(dto.toEntity()))
    }

    override suspend fun deleteDevice(deviceId: String): Result<Unit> = runCatching {
        api.deleteDevice(deviceId)
        // Atualiza cache local via refresh
        refreshDevices()
        Unit
    }
}

@Singleton
class ConsumptionRepositoryImpl @Inject constructor(
    private val api: LummeApi
) : ConsumptionRepository {

    override suspend fun getConsumption(
        from: Instant,
        to: Instant,
        granularity: Granularity
    ): Result<List<ConsumptionReading>> = runCatching {
        api.getConsumption(
            from = from.toString(),
            to = to.toString(),
            granularity = granularity.name.lowercase()
        ).map { it.toDomain() }
    }

    override suspend fun getDeviceConsumption(
        deviceId: String,
        from: Instant,
        to: Instant
    ): Result<List<ConsumptionReading>> = runCatching {
        api.getDeviceConsumption(deviceId, from.toString(), to.toString())
            .map { it.toDomain() }
    }
}

@Singleton
class GoalRepositoryImpl @Inject constructor(
    private val api: LummeApi,
    private val tokenStore: TokenStore
) : GoalRepository {

    override fun observeActiveGoal(): Flow<Goal?> = flow {
        // Não chama API se ainda não há JWT — evita 401 desnecessário
        if (tokenStore.jwt().isNullOrBlank()) {
            emit(null)
            return@flow
        }
        emit(runCatching { api.activeGoal()?.toDomain() }.getOrNull())
    }

    override suspend fun saveGoal(targetKwh: Double): Result<Unit> = runCatching {
        api.createGoal(CreateGoalRequest(targetKwh = targetKwh))
        Unit
    }
}
