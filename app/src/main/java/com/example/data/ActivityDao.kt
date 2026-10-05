package com.example.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface ActivityDao {
    @Query("SELECT * FROM activity_records ORDER BY timestamp DESC")
    fun getAllActivities(): Flow<List<ActivityRecord>>

    @Query("SELECT * FROM activity_records ORDER BY timestamp DESC LIMIT 5")
    fun getRecentActivities(): Flow<List<ActivityRecord>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(record: ActivityRecord): Long

    @Query("DELETE FROM activity_records WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM activity_records")
    suspend fun clearAll()
}
