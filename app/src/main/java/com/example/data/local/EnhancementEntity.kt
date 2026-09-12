package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.domain.model.MediaType
import com.example.domain.model.ProcessStatus

@Entity(tableName = "enhancements")
data class EnhancementEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val mediaType: MediaType,
    val originalUri: String,
    val enhancedUri: String? = null,
    val thumbnailUri: String? = null,
    val originalWidth: Int = 0,
    val originalHeight: Int = 0,
    val enhancedWidth: Int = 0,
    val enhancedHeight: Int = 0,
    val timestamp: Long = System.currentTimeMillis(),
    val status: ProcessStatus = ProcessStatus.IDLE,
    val presetUsed: String = "Pro Auto",
    val processingTimeMs: Long = 0L,
    val fileSizeFormatted: String = "0 MB"
)
