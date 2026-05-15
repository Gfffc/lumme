package com.univesp.lumme.di

import com.univesp.lumme.data.repository.AuthRepositoryImpl
import com.univesp.lumme.data.repository.ConsumptionRepositoryImpl
import com.univesp.lumme.data.repository.DeviceRepositoryImpl
import com.univesp.lumme.data.repository.GoalRepositoryImpl
import com.univesp.lumme.domain.repository.AuthRepository
import com.univesp.lumme.domain.repository.ConsumptionRepository
import com.univesp.lumme.domain.repository.DeviceRepository
import com.univesp.lumme.domain.repository.GoalRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds @Singleton
    abstract fun bindAuthRepo(impl: AuthRepositoryImpl): AuthRepository

    @Binds @Singleton
    abstract fun bindDeviceRepo(impl: DeviceRepositoryImpl): DeviceRepository

    @Binds @Singleton
    abstract fun bindConsumptionRepo(impl: ConsumptionRepositoryImpl): ConsumptionRepository

    @Binds @Singleton
    abstract fun bindGoalRepo(impl: GoalRepositoryImpl): GoalRepository
}
