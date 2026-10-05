package com.example.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverter
import androidx.room.TypeConverters

class Converters {
    @TypeConverter
    fun fromOperationType(value: OperationType): String = value.name

    @TypeConverter
    fun toOperationType(value: String): OperationType = try {
        OperationType.valueOf(value)
    } catch (e: Exception) {
        OperationType.ENCRYPT
    }

    @TypeConverter
    fun fromOperationStatus(value: OperationStatus): String = value.name

    @TypeConverter
    fun toOperationStatus(value: String): OperationStatus = try {
        OperationStatus.valueOf(value)
    } catch (e: Exception) {
        OperationStatus.SUCCESS
    }
}

@Database(entities = [ActivityRecord::class], version = 1, exportSchema = false)
@TypeConverters(Converters::class)
abstract class VaultDatabase : RoomDatabase() {
    abstract fun activityDao(): ActivityDao

    companion object {
        @Volatile
        private var INSTANCE: VaultDatabase? = null

        fun getInstance(context: Context): VaultDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    VaultDatabase::class.java,
                    "vaultx_local.db"
                ).fallbackToDestructiveMigration().build()
                INSTANCE = instance
                instance
            }
        }
    }
}
