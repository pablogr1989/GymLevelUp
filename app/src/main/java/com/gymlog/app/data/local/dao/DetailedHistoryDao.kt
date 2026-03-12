package com.gymlog.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.gymlog.app.data.local.entity.DetailedHistoryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface DetailedHistoryDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDetailedHistory(history: DetailedHistoryEntity)

    @Update
    suspend fun updateDetailedHistory(history: DetailedHistoryEntity) // NUEVA FUNCIÓN

    @Query("SELECT * FROM detailed_history WHERE daySlotId = :daySlotId ORDER BY timestamp ASC")
    fun getHistoryByDaySlot(daySlotId: String): Flow<List<DetailedHistoryEntity>>

    @Query("SELECT * FROM detailed_history ORDER BY timestamp DESC")
    fun getAllDetailedHistory(): Flow<List<DetailedHistoryEntity>>

    @Query("DELETE FROM detailed_history WHERE id = :id")
    suspend fun deleteDetailedHistoryById(id: String)
}