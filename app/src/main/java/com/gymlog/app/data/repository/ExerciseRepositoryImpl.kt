package com.gymlog.app.data.repository

import com.gymlog.app.data.local.dao.ExerciseDao
import com.gymlog.app.data.local.dao.ExerciseHistoryDao
import com.gymlog.app.data.local.entity.ExerciseEntity
import com.gymlog.app.data.local.entity.ExerciseHistoryEntity
import com.gymlog.app.data.local.entity.MuscleGroup
import com.gymlog.app.domain.model.Exercise
import com.gymlog.app.domain.model.ExerciseHistory
import com.gymlog.app.domain.repository.ExerciseRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ExerciseRepositoryImpl @Inject constructor(
    private val exerciseDao: ExerciseDao,
    private val historyDao: ExerciseHistoryDao
) : ExerciseRepository {
    
    override fun getAllExercises(): Flow<List<Exercise>> {
        return exerciseDao.getAllExercises().map { entities ->
            entities.map { it.toDomainModel() }
        }
    }
    
    override fun getExercisesByMuscleGroup(muscleGroup: MuscleGroup): Flow<List<Exercise>> {
        return exerciseDao.getExercisesByMuscleGroup(muscleGroup).map { entities ->
            entities.map { it.toDomainModel() }
        }
    }
    
    override fun searchExercises(query: String): Flow<List<Exercise>> {
        return exerciseDao.searchExercises(query).map { entities ->
            entities.map { it.toDomainModel() }
        }
    }
    
    override suspend fun getExerciseById(exerciseId: String): Exercise? {
        return exerciseDao.getExerciseById(exerciseId)?.toDomainModel()
    }
    
    override suspend fun insertExercise(exercise: Exercise) {
        exerciseDao.insertExercise(exercise.toEntity())
    }
    
    override suspend fun updateExercise(exercise: Exercise) {
        exerciseDao.updateExercise(exercise.toEntity())
    }
    
    override suspend fun deleteExercise(exercise: Exercise) {
        exerciseDao.deleteExercise(exercise.toEntity())
    }
    
    override suspend fun updateExerciseStats(exerciseId: String, series: Int, reps: Int, weight: Float) {
        exerciseDao.updateExerciseStats(exerciseId, series, reps, weight)
    }
    
    override fun getHistoryForExercise(exerciseId: String): Flow<List<ExerciseHistory>> {
        return historyDao.getHistoryForExercise(exerciseId).map { entities ->
            entities.map { it.toDomainModel() }
        }
    }
    
    override suspend fun insertHistory(history: ExerciseHistory) {
        historyDao.insertHistory(history.toEntity())
    }
    
    override suspend fun deleteHistory(history: ExerciseHistory) {
        historyDao.deleteHistory(history.toEntity())
    }
    
    override fun getAllHistory(): Flow<List<ExerciseHistory>> {
        return historyDao.getAllHistory().map { entities ->
            entities.map { it.toDomainModel() }
        }
    }
    
    // Mapper functions
    private fun ExerciseEntity.toDomainModel(): Exercise {
        return Exercise(
            id = id,
            name = name,
            description = description,
            muscleGroup = muscleGroup,
            imageUri = imageUri,
            currentSeries = currentSeries,
            currentReps = currentReps,
            currentWeightKg = currentWeightKg,
            createdAt = createdAt
        )
    }
    
    private fun Exercise.toEntity(): ExerciseEntity {
        return ExerciseEntity(
            id = id,
            name = name,
            description = description,
            muscleGroup = muscleGroup,
            imageUri = imageUri,
            currentSeries = currentSeries,
            currentReps = currentReps,
            currentWeightKg = currentWeightKg,
            createdAt = createdAt
        )
    }
    
    private fun ExerciseHistoryEntity.toDomainModel(): ExerciseHistory {
        return ExerciseHistory(
            id = id,
            exerciseId = exerciseId,
            timestamp = timestamp,
            series = series,
            reps = reps,
            weightKg = weightKg
        )
    }
    
    private fun ExerciseHistory.toEntity(): ExerciseHistoryEntity {
        return ExerciseHistoryEntity(
            id = id,
            exerciseId = exerciseId,
            timestamp = timestamp,
            series = series,
            reps = reps,
            weightKg = weightKg
        )
    }
}
