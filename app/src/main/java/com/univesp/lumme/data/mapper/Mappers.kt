package com.univesp.lumme.data.mapper

import com.univesp.lumme.data.local.DeviceEntity
import com.univesp.lumme.data.remote.ConsumptionDto
import com.univesp.lumme.data.remote.DeviceDto
import com.univesp.lumme.data.remote.GoalDto
import com.univesp.lumme.domain.model.ConsumptionReading
import com.univesp.lumme.domain.model.Device
import com.univesp.lumme.domain.model.DeviceStatus
import com.univesp.lumme.domain.model.DeviceType
import com.univesp.lumme.domain.model.Goal
import com.univesp.lumme.domain.model.GoalPeriod
import com.univesp.lumme.domain.model.Granularity
import java.time.Instant

fun DeviceDto.toEntity(): DeviceEntity = DeviceEntity(
    id = id,
    label = label,
    roomName = roomName,
    type = type,
    isOn = status.isOn,
    powerWatts = status.powerWatts,
    online = status.online,
    capabilities = capabilities,
    lastUpdate = lastUpdate?.let { runCatching { Instant.parse(it).toEpochMilli() }.getOrNull() }
)

fun DeviceEntity.toDomain(): Device = Device(
    id = id,
    label = label,
    roomName = roomName,
    type = runCatching { DeviceType.valueOf(type) }.getOrDefault(DeviceType.UNKNOWN),
    status = DeviceStatus(isOn = isOn, powerWatts = powerWatts, online = online),
    capabilities = capabilities,
    lastUpdate = lastUpdate?.let { Instant.ofEpochMilli(it) }
)

fun ConsumptionDto.toDomain(): ConsumptionReading = ConsumptionReading(
    periodStart = Instant.parse(periodStart),
    periodEnd = Instant.parse(periodEnd),
    energyKwh = energyKwh,
    estimatedCost = estimatedCost,
    granularity = runCatching { Granularity.valueOf(granularity.uppercase()) }
        .getOrDefault(Granularity.DAY)
)

fun GoalDto.toDomain(): Goal = Goal(
    id = id,
    targetKwh = targetKwh,
    period = runCatching { GoalPeriod.valueOf(period.uppercase()) }
        .getOrDefault(GoalPeriod.MONTHLY),
    progressKwh = progressKwh
)
