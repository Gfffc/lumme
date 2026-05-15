package com.univesp.lumme.di

import android.content.Context
import androidx.room.Room
import com.univesp.lumme.data.local.DeviceDao
import com.univesp.lumme.data.local.LummeDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides @Singleton
    fun provideDatabase(@ApplicationContext context: Context): LummeDatabase =
        Room.databaseBuilder(context, LummeDatabase::class.java, "lumme.db")
            .fallbackToDestructiveMigration()
            .build()

    @Provides
    fun provideDeviceDao(db: LummeDatabase): DeviceDao = db.deviceDao()
}
