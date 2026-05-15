package com.univesp.lumme.data.local

import androidx.room.ColumnInfo
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.RoomDatabase
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "devices")
data class DeviceEntity(
    @PrimaryKey val id: String,
    val label: String,
    @ColumnInfo(name = "room_name") val roomName: String?,
    val type: String,
    @ColumnInfo(name = "is_on") val isOn: Boolean,
    @ColumnInfo(name = "power_watts") val powerWatts: Double?,
    val online: Boolean,
    val capabilities: List<String>,
    @ColumnInfo(name = "last_update") val lastUpdate: Long?   // epoch millis
)

class StringListConverter {
    @TypeConverter
    fun fromList(value: List<String>): String = value.joinToString("|")

    @TypeConverter
    fun toList(value: String): List<String> =
        if (value.isBlank()) emptyList() else value.split("|")
}

@Dao
interface DeviceDao {
    @Query("SELECT * FROM devices ORDER BY room_name, label")
    fun observeAll(): Flow<List<DeviceEntity>>

    @Query("SELECT * FROM devices WHERE id = :id")
    suspend fun findById(id: String): DeviceEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(devices: List<DeviceEntity>)

    @Query("UPDATE devices SET is_on = :isOn WHERE id = :id")
    suspend fun updateState(id: String, isOn: Boolean)

    @Query("DELETE FROM devices")
    suspend fun clear()
}

@Database(
    entities = [DeviceEntity::class],
    version = 1,
    exportSchema = false
)
@TypeConverters(StringListConverter::class)
abstract class LummeDatabase : RoomDatabase() {
    abstract fun deviceDao(): DeviceDao
}
