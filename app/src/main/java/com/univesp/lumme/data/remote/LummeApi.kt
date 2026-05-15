package com.univesp.lumme.data.remote

import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

interface LummeApi {

    @POST("auth/smartthings/start")
    suspend fun startOAuth(): OAuthStartResponse

    @POST("auth/smartthings/exchange")
    suspend fun exchangeCode(@Body body: OAuthExchangeRequest): AuthResponse

    @POST("auth/refresh")
    suspend fun refreshToken(): AuthResponse

    @GET("devices")
    suspend fun listDevices(): List<DeviceDto>

    @GET("devices/{id}")
    suspend fun getDevice(@Path("id") id: String): DeviceDto

    @POST("devices")
    suspend fun createDevice(@Body body: CreateDeviceRequest): DeviceDto

    @DELETE("devices/{id}")
    suspend fun deleteDevice(@Path("id") id: String)

    @POST("devices/{id}/command")
    suspend fun sendCommand(
        @Path("id") id: String,
        @Body command: CommandRequest
    )

    @GET("consumption")
    suspend fun getConsumption(
        @Query("from") from: String,
        @Query("to") to: String,
        @Query("granularity") granularity: String
    ): List<ConsumptionDto>

    @GET("consumption/device/{id}")
    suspend fun getDeviceConsumption(
        @Path("id") deviceId: String,
        @Query("from") from: String,
        @Query("to") to: String
    ): List<ConsumptionDto>

    @GET("goals/active")
    suspend fun activeGoal(): GoalDto?

    @POST("goals")
    suspend fun createGoal(@Body body: CreateGoalRequest): GoalDto
}
