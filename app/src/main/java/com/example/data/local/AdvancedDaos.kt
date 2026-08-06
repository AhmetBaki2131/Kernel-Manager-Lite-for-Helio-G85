package com.example.data.local

import androidx.room.*
import com.example.data.model.AiHistoryEntity
import com.example.data.model.BackupEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AiHistoryDao {
    @Query("SELECT * FROM ai_history ORDER BY timestamp DESC")
    fun getAllHistory(): Flow<List<AiHistoryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHistory(item: AiHistoryEntity): Long

    @Delete
    suspend fun deleteHistory(item: AiHistoryEntity)

    @Query("DELETE FROM ai_history")
    suspend fun clearHistory()
}

@Dao
interface BackupDao {
    @Query("SELECT * FROM kernel_backups ORDER BY timestamp DESC")
    fun getAllBackups(): Flow<List<BackupEntity>>

    @Query("SELECT * FROM kernel_backups WHERE id = :id")
    suspend fun getBackupById(id: Long): BackupEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBackup(backup: BackupEntity): Long

    @Delete
    suspend fun deleteBackup(backup: BackupEntity)
}
