@file:OptIn(kotlinx.serialization.InternalSerializationApi::class)

package com.gymlog.app.util

import android.content.Context
import android.net.Uri
import android.util.Log
import com.gymlog.app.data.backup.BackupData
import com.gymlog.app.data.local.GymLogDatabase
import com.gymlog.app.data.local.entity.ExerciseEntity
import com.gymlog.app.data.local.entity.MuscleGroup
import com.gymlog.app.data.local.entity.SetEntity
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton
import androidx.room.withTransaction
import java.util.UUID

@Singleton
class BackupManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val db: GymLogDatabase
) {

    private val json = Json { prettyPrint = true; ignoreUnknownKeys = true }

    suspend fun exportDataToJson(destinationUri: Uri): Unit = withContext(Dispatchers.IO) {
        val allData = BackupData(
            exercises = db.exerciseDao().getAllExercises().first(),
            sets = db.setDao().getAllSets(),
            history = db.exerciseHistoryDao().getAllHistory().first(),
            calendars = db.calendarDao().getAllCalendars().first(),
            months = db.monthDao().getAllMonths().first(),
            weeks = db.weekDao().getAllWeeks().first(),
            daySlots = db.daySlotDao().getAllDaySlots().first()
        )

        val jsonString = json.encodeToString(allData)

        context.contentResolver.openOutputStream(destinationUri)?.use { outputStream ->
            val utf8Bytes = jsonString.toByteArray(Charsets.UTF_8)
            outputStream.write(utf8Bytes)
        } ?: throw IllegalStateException("Error al abrir stream de escritura.")
    }

    suspend fun importDataFromJson(uri: Uri): Unit = withContext(Dispatchers.IO) {
        context.contentResolver.openInputStream(uri)?.use { inputStream ->
            val jsonString = inputStream.bufferedReader().use { it.readText() }

            // Intentar migrar formato antiguo o leer formato nuevo
            val backupData = try {
                // Intentamos leer formato nuevo.
                // Al haber hecho obligatorio el campo 'sets', esto FALLARÁ con los backups antiguos.
                val data = json.decodeFromString<BackupData>(jsonString)
                Log.d("BackupManager", "Formato V2 detectado correctamente.")
                data
            } catch (e: Exception) {
                Log.i("BackupManager", "Formato V2 falló (${e.message}). Intentando migración LEGACY...")
                // Si falla, asumimos que es un backup antiguo y migramos
                val legacyData = json.decodeFromString<LegacyBackupData>(jsonString)
                migrateLegacyToNew(legacyData)
            }

            db.withTransaction {
                // Limpieza total
                db.exerciseHistoryDao().deleteAllHistory()
                db.setDao().deleteAllSets()
                db.exerciseDao().deleteAllExercises()
                db.daySlotDao().deleteAllDaySlots()
                db.weekDao().deleteAllWeeks()
                db.monthDao().deleteAllMonths()
                db.calendarDao().deleteAllCalendars()

                // Inserción ordenada
                backupData.calendars.forEach { db.calendarDao().insertCalendar(it) }
                backupData.months.forEach { db.monthDao().insertMonth(it) }
                backupData.weeks.forEach { db.weekDao().insertWeek(it) }
                backupData.daySlots.forEach { db.daySlotDao().insertDaySlot(it) }

                backupData.exercises.forEach { db.exerciseDao().insertExercise(it) }
                backupData.sets.forEach { db.setDao().insertSet(it) }
                backupData.history.forEach { db.exerciseHistoryDao().insertHistory(it) }
            }
            Log.i("BackupManager", "Importación finalizada con éxito.")
        } ?: throw IllegalStateException("No se pudo leer el archivo.")
    }

    private fun migrateLegacyToNew(legacy: LegacyBackupData): BackupData {
        val newSets = mutableListOf<SetEntity>()
        val newExercises = legacy.exercises.map { old ->
            // Lógica de migración: Crear Set si había datos
            if (old.currentSeries > 0 || old.currentReps > 0 || old.currentWeightKg > 0f) {
                val setId = UUID.randomUUID().toString()
                newSets.add(
                    SetEntity(
                        id = setId,
                        exerciseId = old.id,
                        series = old.currentSeries,
                        reps = old.currentReps,
                        weightKg = old.currentWeightKg
                    )
                )
            }
            // Mapear ejercicio limpio
            ExerciseEntity(
                id = old.id,
                name = old.name,
                description = old.description,
                muscleGroup = old.muscleGroup,
                imageUri = old.imageUri,
                notes = old.notes,
                changeLogText = old.changeLogText,
                createdAt = old.createdAt
            )
        }

        Log.i("BackupManager", "Migración completada: ${newExercises.size} ejercicios y ${newSets.size} sets generados.")

        return BackupData(
            exercises = newExercises,
            sets = newSets,
            history = legacy.history,
            calendars = legacy.calendars,
            months = legacy.months,
            weeks = legacy.weeks,
            daySlots = legacy.daySlots
        )
    }

    @Serializable
    private data class LegacyBackupData(
        val exercises: List<LegacyExerciseEntity>,
        val history: List<com.gymlog.app.data.local.entity.ExerciseHistoryEntity>,
        val calendars: List<com.gymlog.app.data.local.entity.CalendarEntity>,
        val months: List<com.gymlog.app.data.local.entity.MonthEntity>,
        val weeks: List<com.gymlog.app.data.local.entity.WeekEntity>,
        val daySlots: List<com.gymlog.app.data.local.entity.DaySlotEntity>
    )

    @Serializable
    private data class LegacyExerciseEntity(
        val id: String,
        val name: String,
        val description: String = "",
        val muscleGroup: MuscleGroup,
        val imageUri: String? = null,
        val currentSeries: Int = 0,
        val currentReps: Int = 0,
        val currentWeightKg: Float = 0f,
        val notes: String = "",
        val changeLogText: String = "",
        val createdAt: Long
    )
}