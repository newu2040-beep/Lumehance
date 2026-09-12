package com.example.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface EnhancementDao {
    @Query("SELECT * FROM enhancements ORDER BY timestamp DESC")
    fun getAllEnhancements(): Flow<List<EnhancementEntity>>

    @Query("SELECT * FROM enhancements WHERE id = :id LIMIT 1")
    fun getEnhancementById(id: Long): Flow<EnhancementEntity?>

    @Query("SELECT * FROM enhancements WHERE status IN ('QUEUED', 'PROCESSING') ORDER BY timestamp ASC")
    fun getQueueItems(): Flow<List<EnhancementEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEnhancement(item: EnhancementEntity): Long

    @Update
    suspend fun updateEnhancement(item: EnhancementEntity)

    @Delete
    suspend fun deleteEnhancement(item: EnhancementEntity)

    @Query("DELETE FROM enhancements WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM enhancements")
    suspend fun clearAll()
}
