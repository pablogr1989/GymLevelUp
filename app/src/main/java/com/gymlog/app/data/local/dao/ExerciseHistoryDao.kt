package com.gymlog.app.data.local.dao

import androidx.room.*
import com.gymlog.app.data.local.entity.ExerciseHistoryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ExerciseHistoryDao {
    @Query("SELECT * FROM exercise_history WHERE exerciseId = :exerciseId ORDER BY timestamp DESC")
    fun getHistoryForExercise(exerciseId: String): Flow<List<ExerciseHistoryEntity>>
    
    @Query("SELECT * FROM exercise_history WHERE exerciseId = :exerciseId ORDER BY timestamp DESC LIMIT :limit")
    suspend fun getRecentHistoryForExercise(exerciseId: String, limit: Int = 10): List<ExerciseHistoryEntity>
    
    @Insert
    suspend fun insertHistory(history: ExerciseHistoryEntity)
    
    @Delete
    suspend fun deleteHistory(history: ExerciseHistoryEntity)
    
    @Query("DELETE FROM exercise_history WHERE exerciseId = :exerciseId")
    suspend fun deleteAllHistoryForExercise(exerciseId: String)
    
    @Query("SELECT * FROM exercise_history ORDER BY timestamp DESC")
    fun getAllHistory(): Flow<List<ExerciseHistoryEntity>>
}
