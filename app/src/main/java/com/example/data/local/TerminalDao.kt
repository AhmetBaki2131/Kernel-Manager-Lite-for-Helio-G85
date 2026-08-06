package com.example.data.local

import androidx.room.*
import com.example.data.model.TerminalCommandEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TerminalDao {
    @Query("SELECT * FROM terminal_commands ORDER BY timestamp DESC")
    fun getAllCommands(): Flow<List<TerminalCommandEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCommand(cmd: TerminalCommandEntity): Long

    @Query("DELETE FROM terminal_commands")
    suspend fun clearHistory()
}
