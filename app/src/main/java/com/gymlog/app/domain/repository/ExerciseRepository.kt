package com.gymlog.app.domain.repository

import com.gymlog.app.data.local.entity.MuscleGroup
import com.gymlog.app.domain.model.Exercise
import com.gymlog.app.domain.model.ExerciseHistory
import kotlinx.coroutines.flow.Flow

interface ExerciseRepository {
    fun getAllExercises(): Flow<List<Exercise>>
    fun getExercisesByMuscleGroup(muscleGroup: MuscleGroup): Flow<List<Exercise>>
    fun searchExercises(query: String): Flow<List<Exercise>>
    suspend fun getExerciseById(exerciseId: String): Exercise?
    suspend fun insertExercise(exercise: Exercise)
    suspend fun updateExercise(exercise: Exercise)
    suspend fun deleteExercise(exercise: Exercise)
    suspend fun updateExerciseStats(exerciseId: String, series: Int, reps: Int, weight: Float)
    
    fun getHistoryForExercise(exerciseId: String): Flow<List<ExerciseHistory>>
    suspend fun insertHistory(history: ExerciseHistory)
    suspend fun deleteHistory(history: ExerciseHistory)
    fun getAllHistory(): Flow<List<ExerciseHistory>>
}
