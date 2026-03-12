package com.gymlog.app.data.repository

import com.gymlog.app.data.local.dao.ExerciseDao
import com.gymlog.app.data.local.dao.ExerciseHistoryDao
import com.gymlog.app.data.local.dao.SetDao
import com.gymlog.app.data.local.dao.DetailedHistoryDao
import com.gymlog.app.data.local.entity.ExerciseEntity
import com.gymlog.app.data.local.entity.ExerciseHistoryEntity
import com.gymlog.app.data.local.entity.DetailedHistoryEntity
import com.gymlog.app.data.local.entity.MuscleGroup
import com.gymlog.app.data.local.entity.SetEntity
import com.gymlog.app.domain.model.Exercise
import com.gymlog.app.domain.model.ExerciseHistory
import com.gymlog.app.domain.model.DetailedHistory
import com.gymlog.app.domain.model.Set
import com.gymlog.app.domain.repository.ExerciseRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ExerciseRepositoryImpl @Inject constructor(
    private val exerciseDao: ExerciseDao,
    private val historyDao: ExerciseHistoryDao,
    private val setDao: SetDao,
    private val detailedHistoryDao: DetailedHistoryDao
) : ExerciseRepository {

    override fun getAllExercises(): Flow<List<Exercise>> {
        return exerciseDao.getAllExercises().map { entities ->
            entities.map { entity ->
                val sets = setDao.getSetsForExerciseSync(entity.id).map { it.toDomainModel() }
                entity.toDomainModel(sets)
            }
        }
    }

    override fun getExercisesByMuscleGroup(muscleGroup: MuscleGroup): Flow<List<Exercise>> {
        return exerciseDao.getExercisesByMuscleGroup(muscleGroup).map { entities ->
            entities.map { entity ->
                val sets = setDao.getSetsForExerciseSync(entity.id).map { it.toDomainModel() }
                entity.toDomainModel(sets)
            }
        }
    }

    override fun searchExercises(query: String): Flow<List<Exercise>> {
        return exerciseDao.searchExercises(query).map { entities ->
            entities.map { entity ->
                val sets = setDao.getSetsForExerciseSync(entity.id).map { it.toDomainModel() }
                entity.toDomainModel(sets)
            }
        }
    }

    override suspend fun getExerciseById(exerciseId: String): Exercise? {
        val entity = exerciseDao.getExerciseById(exerciseId) ?: return null
        val sets = setDao.getSetsForExerciseSync(exerciseId).map { it.toDomainModel() }
        return entity.toDomainModel(sets)
    }

    override suspend fun insertExercise(exercise: Exercise) {
        exerciseDao.insertExercise(exercise.toEntity())
        exercise.sets.forEach { setDao.insertSet(it.toEntity()) }
    }

    override suspend fun updateExercise(exercise: Exercise) { exerciseDao.updateExercise(exercise.toEntity()) }
    override suspend fun deleteExercise(exercise: Exercise) { exerciseDao.deleteExercise(exercise.toEntity()) }

    override suspend fun updateExerciseStats(exerciseId: String, series: Int, reps: Int, weight: Float) {
        val sets = setDao.getSetsForExerciseSync(exerciseId)
        if (sets.isNotEmpty()) {
            val firstSet = sets.first()
            setDao.updateSet(firstSet.copy(series = series, minReps = reps, maxReps = reps, weightKg = weight))
        } else {
            setDao.insertSet(SetEntity(UUID.randomUUID().toString(), exerciseId, series, reps, reps, weight, null, null))
        }
    }

    override suspend fun updateExerciseNotes(exerciseId: String, notes: String) { exerciseDao.updateExerciseNotes(exerciseId, notes) }
    override suspend fun updateExerciseChangeLog(exerciseId: String, changeLog: String) { exerciseDao.updateExerciseChangeLog(exerciseId, changeLog) }
    override suspend fun updateExerciseInfo(exerciseId: String, name: String, description: String, muscleGroup: MuscleGroup, imageUri: String?) {
        exerciseDao.updateExerciseInfo(exerciseId, name, description, muscleGroup, imageUri)
    }

    override fun getHistoryForExercise(exerciseId: String): Flow<List<ExerciseHistory>> {
        return historyDao.getHistoryForExercise(exerciseId).map { entities -> entities.map { it.toDomainModel() } }
    }

    override suspend fun insertHistory(history: ExerciseHistory) { historyDao.insertHistory(history.toEntity()) }
    override suspend fun deleteHistory(history: ExerciseHistory) { historyDao.deleteHistory(history.toEntity()) }
    override suspend fun deleteHistoryById(historyId: String) { historyDao.deleteHistoryById(historyId) }
    override suspend fun deleteAllHistoryForExercise(exerciseId: String) { historyDao.deleteAllHistoryForExercise(exerciseId) }
    override fun getAllHistory(): Flow<List<ExerciseHistory>> { return historyDao.getAllHistory().map { entities -> entities.map { it.toDomainModel() } } }

    override fun getDetailedHistoryByDaySlot(daySlotId: String): Flow<List<DetailedHistory>> {
        return detailedHistoryDao.getHistoryByDaySlot(daySlotId).map { entities -> entities.map { it.toDomainModel() } }
    }
    override fun getAllDetailedHistory(): Flow<List<DetailedHistory>> {
        return detailedHistoryDao.getAllDetailedHistory().map { entities -> entities.map { it.toDomainModel() } }
    }
    override suspend fun insertDetailedHistory(history: DetailedHistory) { detailedHistoryDao.insertDetailedHistory(history.toEntity()) }

    // NUEVA FUNCIÓN
    override suspend fun updateDetailedHistory(history: DetailedHistory) { detailedHistoryDao.updateDetailedHistory(history.toEntity()) }

    override suspend fun deleteDetailedHistoryById(id: String) { detailedHistoryDao.deleteDetailedHistoryById(id) }

    override suspend fun getSetById(setId: String): Set? { return setDao.getSetById(setId)?.toDomainModel() }
    override suspend fun insertSet(set: Set) { setDao.insertSet(set.toEntity()) }
    override suspend fun updateSet(set: Set) { setDao.updateSet(set.toEntity()) }
    override suspend fun deleteSet(setId: String) { setDao.deleteSetById(setId) }

    private fun ExerciseEntity.toDomainModel(sets: List<Set>) = Exercise(id, name, description, muscleGroup, imageUri, sets, notes, changeLogText, createdAt)
    private fun Exercise.toEntity() = ExerciseEntity(id, name, description, muscleGroup, imageUri, notes, changeLogText, createdAt)
    private fun SetEntity.toDomainModel() = Set(id, exerciseId, series, minReps, maxReps, weightKg, minRir, maxRir)
    private fun Set.toEntity() = SetEntity(id, exerciseId, series, minReps, maxReps, weightKg, minRir, maxRir)
    private fun ExerciseHistoryEntity.toDomainModel() = ExerciseHistory(id, exerciseId, setId, timestamp, series, reps, weightKg)
    private fun ExerciseHistory.toEntity() = ExerciseHistoryEntity(id, exerciseId, setId, timestamp, series, reps, weightKg)

    // ACTUALIZADO CON RIR
    private fun DetailedHistoryEntity.toDomainModel() = DetailedHistory(id, exerciseId, setId, daySlotId, timestamp, seriesNumber, reps, weightKg, notes, rir)
    private fun DetailedHistory.toEntity() = DetailedHistoryEntity(id, exerciseId, setId, daySlotId, timestamp, seriesNumber, reps, weightKg, notes, rir)
}