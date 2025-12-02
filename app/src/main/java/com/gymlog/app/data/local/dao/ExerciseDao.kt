package com.gymlog.app.data.local.dao

import androidx.room.*
import com.gymlog.app.data.local.entity.ExerciseEntity
import com.gymlog.app.data.local.entity.MuscleGroup
import kotlinx.coroutines.flow.Flow

@Dao
interface ExerciseDao {
    @Transaction
    @Query("SELECT * FROM exercises ORDER BY name ASC")
    fun getAllExercises(): Flow<List<ExerciseEntity>>

    @Query("SELECT COUNT(*) FROM exercises")
    suspend fun getExerciseCount(): Int

    @Query("SELECT * FROM exercises WHERE muscleGroup = :muscleGroup ORDER BY name ASC")
    fun getExercisesByMuscleGroup(muscleGroup: MuscleGroup): Flow<List<ExerciseEntity>>

    @Query("SELECT * FROM exercises WHERE id = :exerciseId")
    suspend fun getExerciseById(exerciseId: String): ExerciseEntity?

    @Query("SELECT * FROM exercises WHERE name LIKE '%' || :query || '%' ORDER BY name ASC")
    fun searchExercises(query: String): Flow<List<ExerciseEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExercise(exercise: ExerciseEntity)

    @Update
    suspend fun updateExercise(exercise: ExerciseEntity)

    @Delete
    suspend fun deleteExercise(exercise: ExerciseEntity)

    @Query("DELETE FROM exercises WHERE id = :exerciseId")
    suspend fun deleteExerciseById(exerciseId: String)

    @Query("DELETE FROM exercises")
    suspend fun deleteAllExercises()

    @Query("UPDATE exercises SET notes = :notes WHERE id = :exerciseId")
    suspend fun updateExerciseNotes(exerciseId: String, notes: String)

    @Query("UPDATE exercises SET changeLogText = :changeLog WHERE id = :exerciseId")
    suspend fun updateExerciseChangeLog(exerciseId: String, changeLog: String)

    @Query("UPDATE exercises SET name = :name, description = :description, muscleGroup = :muscleGroup, imageUri = :imageUri WHERE id = :exerciseId")
    suspend fun updateExerciseInfo(exerciseId: String, name: String, description: String, muscleGroup: MuscleGroup, imageUri: String?)
}