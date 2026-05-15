package com.univesp.lumme.data.remote

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

// ---------- AUTH ----------

@JsonClass(generateAdapter = true)
data class OAuthStartResponse(
    @Json(name = "authorizeUrl") val authorizeUrl: String,
    @Json(name = "state") val state: String
)

@JsonClass(generateAdapter = true)
data class OAuthExchangeRequest(
    @Json(name = "code") val code: String,
    @Json(name = "state") val state: String
)

@JsonClass(generateAdapter = true)
data class AuthResponse(
    @Json(name = "jwt") val jwt: String,
    @Json(name = "expiresAt") val expiresAt: String
)

// ---------- DEVICES ----------

@JsonClass(generateAdapter = true)
data class DeviceDto(
    @Json(name = "id") val id: String,
    @Json(name = "label") val label: String,
    @Json(name = "roomName") val roomName: String?,
    @Json(name = "type") val type: String,
    @Json(name = "status") val status: DeviceStatusDto,
    @Json(name = "capabilities") val capabilities: List<String>,
    @Json(name = "lastUpdate") val lastUpdate: String?
)

@JsonClass(generateAdapter = true)
data class DeviceStatusDto(
    @Json(name = "isOn") val isOn: Boolean,
    @Json(name = "powerWatts") val powerWatts: Double?,
    @Json(name = "online") val online: Boolean
)

@JsonClass(generateAdapter = true)
data class CommandRequest(
    @Json(name = "capability") val capability: String,   // ex: "switch"
    @Json(name = "command") val command: String,         // ex: "on" | "off"
    @Json(name = "arguments") val arguments: List<Any> = emptyList()
)

@JsonClass(generateAdapter = true)
data class CreateDeviceRequest(
    @Json(name = "label") val label: String,
    @Json(name = "type") val type: String,          // SWITCH | LIGHT_BULB | THERMOSTAT | OUTLET | SENSOR
    @Json(name = "roomName") val roomName: String?
)

// ---------- CONSUMPTION ----------

@JsonClass(generateAdapter = true)
data class ConsumptionDto(
    @Json(name = "periodStart") val periodStart: String,
    @Json(name = "periodEnd") val periodEnd: String,
    @Json(name = "energyKwh") val energyKwh: Double,
    @Json(name = "estimatedCost") val estimatedCost: Double?,
    @Json(name = "granularity") val granularity: String
)

// ---------- GOALS ----------

@JsonClass(generateAdapter = true)
data class GoalDto(
    @Json(name = "id") val id: String,
    @Json(name = "targetKwh") val targetKwh: Double,
    @Json(name = "period") val period: String,
    @Json(name = "progressKwh") val progressKwh: Double
)

@JsonClass(generateAdapter = true)
data class CreateGoalRequest(
    @Json(name = "targetKwh") val targetKwh: Double,
    @Json(name = "period") val period: String = "MONTHLY"
)
