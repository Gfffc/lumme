package com.univesp.lumme.domain.model

import java.time.Instant

/** Representa um dispositivo SmartThings. */
data class Device(
    val id: String,
    val label: String,
    val roomName: String?,
    val type: DeviceType,
    val status: DeviceStatus,
    val capabilities: List<String>,
    val lastUpdate: Instant?
)

enum class DeviceType { SWITCH, LIGHT_BULB, THERMOSTAT, OUTLET, SENSOR, UNKNOWN }

/** Estado corrente do dispositivo (on/off + potência se disponível). */
data class DeviceStatus(
    val isOn: Boolean,
    val powerWatts: Double?,   // null quando o dispositivo não tem medidor
    val online: Boolean
)

/** Leitura de consumo agregada. */
data class ConsumptionReading(
    val periodStart: Instant,
    val periodEnd: Instant,
    val energyKwh: Double,
    val estimatedCost: Double?,
    val granularity: Granularity
)

enum class Granularity { HOUR, DAY, MONTH }

/** Meta de consumo definida pelo usuário. */
data class Goal(
    val id: String,
    val targetKwh: Double,
    val period: GoalPeriod,
    val progressKwh: Double
) {
    val progressPercent: Float
        get() = if (targetKwh <= 0) 0f else (progressKwh / targetKwh).toFloat().coerceIn(0f, 1f)
}

enum class GoalPeriod { WEEKLY, MONTHLY }
