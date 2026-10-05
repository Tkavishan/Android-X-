package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class OperationType {
    ENCRYPT,
    DECRYPT
}

enum class OperationStatus {
    SUCCESS,
    FAILED
}

@Entity(tableName = "activity_records")
data class ActivityRecord(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val type: OperationType,
    val sourceFileName: String,
    val outputFileName: String,
    val outputFilePath: String?,
    val fileSizeBytes: Long,
    val timestamp: Long = System.currentTimeMillis(),
    val status: OperationStatus,
    val message: String? = null
)
