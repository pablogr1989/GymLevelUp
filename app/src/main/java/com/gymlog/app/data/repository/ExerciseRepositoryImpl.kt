package com.gymlog.app.data.repository

import com.gymlog.app.data.local.dao.ExerciseDao
import com.gymlog.app.data.local.dao.ExerciseHistoryDao
import com.gymlog.app.data.local.dao.SetDao
import com.gymlog.app.data.local.entity.ExerciseEntity
import com.gymlog.app.data.local.entity.ExerciseHistoryEntity
import com.gymlog.app.data.local.entity.MuscleGroup
import com.gymlog.app.data.local.entity.SetEntity
import com.gymlog.app.domain.model.Exercise
import com.gymlog.app.domain.model.ExerciseHistory
import com.gymlog.app.domain.model.Set
import com.gymlog.app.domain.repository.ExerciseRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ExerciseRepositoryImpl @Inject constructor(
    private val exerciseDao: ExerciseDao,
    private val historyDao: ExerciseHistoryDao,
    private val setDao: SetDao
) : ExerciseRepository {

    // Combina ejercicios con sus sets para devolver el modelo completo
    override fun getAllExercises(): Flow<List<Exercise>> {
        // Nota: Esta no es la forma más eficiente (N+1 queries potencialmente),
        // pero Room no soporta relaciones en Flows fácilmente sin POJOs intermedios complejos.
        // Dado el tamaño de la DB, esto es aceptable por ahora o podemos optimizarlo cargando todos los sets.
        // Una mejor estrategia: cargar todos los ejercicios y map.

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
        // Insertar sets también
        exercise.sets.forEach { set ->
            setDao.insertSet(set.toEntity())
        }
    }

    override suspend fun updateExercise(exercise: Exercise) {
        exerciseDao.updateExercise(exercise.toEntity())
    }

    override suspend fun deleteExercise(exercise: Exercise) {
        exerciseDao.deleteExercise(exercise.toEntity())
    }

    // Adaptador Legacy: Busca el primer set y lo actualiza, o crea uno nuevo
    override suspend fun updateExerciseStats(exerciseId: String, series: Int, reps: Int, weight: Float) {
        val sets = setDao.getSetsForExerciseSync(exerciseId)
        if (sets.isNotEmpty()) {
            val firstSet = sets.first()
            setDao.updateSet(firstSet.copy(series = series, reps = reps, weightKg = weight))
        } else {
            setDao.insertSet(
                SetEntity(
                    id = UUID.randomUUID().toString(),
                    exerciseId = exerciseId,
                    series = series,
                    reps = reps,
                    weightKg = weight
                )
            )
        }
    }

    override suspend fun updateExerciseNotes(exerciseId: String, notes: String) {
        exerciseDao.updateExerciseNotes(exerciseId, notes)
    }

    override suspend fun updateExerciseChangeLog(exerciseId: String, changeLog: String) {
        exerciseDao.updateExerciseChangeLog(exerciseId, changeLog)
    }

    override suspend fun updateExerciseInfo(exerciseId: String, name: String, description: String, muscleGroup: MuscleGroup, imageUri: String?) {
        exerciseDao.updateExerciseInfo(exerciseId, name, description, muscleGroup, imageUri)
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

    override suspend fun deleteHistoryById(historyId: String) {
        historyDao.deleteHistoryById(historyId)
    }

    override suspend fun deleteAllHistoryForExercise(exerciseId: String) {
        historyDao.deleteAllHistoryForExercise(exerciseId)
    }

    override fun getAllHistory(): Flow<List<ExerciseHistory>> {
        return historyDao.getAllHistory().map { entities ->
            entities.map { it.toDomainModel() }
        }
    }

    // Sets operations
    override suspend fun insertSet(set: Set) {
        setDao.insertSet(set.toEntity())
    }

    override suspend fun updateSet(set: Set) {
        setDao.updateSet(set.toEntity())
    }

    override suspend fun deleteSet(setId: String) {
        // Implementación futura directa en DAO si es necesario
    }

    // Mappers
    private fun ExerciseEntity.toDomainModel(sets: List<Set>): Exercise {
        return Exercise(
            id = id,
            name = name,
            description = description,
            muscleGroup = muscleGroup,
            imageUri = imageUri,
            sets = sets,
            notes = notes,
            changeLogText = changeLogText,
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
            notes = notes,
            changeLogText = changeLogText,
            createdAt = createdAt
        )
    }

    private fun SetEntity.toDomainModel() = Set(id, exerciseId, series, reps, weightKg)
    private fun Set.toEntity() = SetEntity(id, exerciseId, series, reps, weightKg)

    private fun ExerciseHistoryEntity.toDomainModel() = ExerciseHistory(id, exerciseId, setId, timestamp, series, reps, weightKg)
    private fun ExerciseHistory.toEntity() = ExerciseHistoryEntity(id, exerciseId, setId, timestamp, series, reps, weightKg)
}