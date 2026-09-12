package com.example.data.repository

import com.example.data.local.EnhancementDao
import com.example.data.local.EnhancementEntity
import kotlinx.coroutines.flow.Flow

class EnhancementRepository(private val dao: EnhancementDao) {

    val allEnhancements: Flow<List<EnhancementEntity>> = dao.getAllEnhancements()
    val queueItems: Flow<List<EnhancementEntity>> = dao.getQueueItems()

    fun getEnhancementById(id: Long): Flow<EnhancementEntity?> = dao.getEnhancementById(id)

    suspend fun insert(item: EnhancementEntity): Long = dao.insertEnhancement(item)

    suspend fun update(item: EnhancementEntity) = dao.updateEnhancement(item)

    suspend fun delete(item: EnhancementEntity) = dao.deleteEnhancement(item)

    suspend fun deleteById(id: Long) = dao.deleteById(id)

    suspend fun clearAll() = dao.clearAll()
}
