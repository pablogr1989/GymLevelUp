@file:OptIn(kotlinx.serialization.InternalSerializationApi::class)

package com.gymlog.app.util

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.room.withTransaction
import com.gymlog.app.data.backup.BackupData
import com.gymlog.app.data.local.GymLogDatabase
import com.gymlog.app.data.local.entity.*
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first // <--- ESTA ES LA IMPORTACIÓN QUE FALTABA
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton
import java.util.UUID

@Singleton
class BackupManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val db: GymLogDatabase
) {

    private val json = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    suspend fun exportDataToJson(destinationUri: Uri): Unit = withContext(Dispatchers.IO) {
        val allData = BackupData(
            exercises = db.exerciseDao().getAllExercises().first(), // first() requiere el import de arriba
            sets = db.setDao().getAllSets(),
            history = db.exerciseHistoryDao().getAllHistory().first(),
            calendars = db.calendarDao().getAllCalendars().first(),
            months = db.monthDao().getAllMonths().first(),
            weeks = db.weekDao().getAllWeeks().first(),
            daySlots = db.daySlotDao().getAllDaySlots().first(),
            daySlotExercises = db.daySlotDao().getAllCrossRefs() // Esto devuelve List, no Flow, así que no lleva first()
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

            val rawData = try {
                val data = json.decodeFromString<BackupData>(jsonString)
                if (data.daySlotExercises.isEmpty() && data.daySlots.isNotEmpty()) {
                    tryMigrateFromV2(jsonString) ?: data
                } else {
                    data
                }
            } catch (e: Exception) {
                Log.w("BackupManager", "Fallo lectura V3, intentando migración legacy: ${e.message}")
                tryMigrateFromV2(jsonString) ?: throw e
            }

            // FASE DE SANITIZACIÓN: Limpiamos referencias rotas antes de tocar la DB
            val cleanData = sanitizeData(rawData)

            db.withTransaction {
                // 1. Limpieza
                db.daySlotDao().deleteAllCrossRefs()
                db.exerciseHistoryDao().deleteAllHistory()
                db.setDao().deleteAllSets()
                db.exerciseDao().deleteAllExercises()
                db.daySlotDao().deleteAllDaySlots()
                db.weekDao().deleteAllWeeks()
                db.monthDao().deleteAllMonths()
                db.calendarDao().deleteAllCalendars()

                // 2. Inserción Ordenada
                cleanData.calendars.forEach { db.calendarDao().insertCalendar(it) }
                cleanData.months.forEach { db.monthDao().insertMonth(it) }
                cleanData.weeks.forEach { db.weekDao().insertWeek(it) }

                cleanData.daySlots.forEach { db.daySlotDao().insertDaySlot(it) }
                cleanData.exercises.forEach { db.exerciseDao().insertExercise(it) }

                // Estos son los puntos críticos de FK Constraint
                cleanData.sets.forEach { db.setDao().insertSet(it) }
                cleanData.daySlotExercises.forEach { db.daySlotDao().insertDaySlotCrossRef(it) }
                cleanData.history.forEach { db.exerciseHistoryDao().insertHistory(it) }
            }
            Log.i("BackupManager", "Importación completada y sanitizada.")
        } ?: throw IllegalStateException("No se pudo leer el archivo.")
    }

    /**
     * Filtra los datos para eliminar registros que apunten a IDs inexistentes.
     * Esto evita el error SQLITE_CONSTRAINT_FOREIGNKEY (code 787).
     */
    private fun sanitizeData(data: BackupData): BackupData {
        // 1. Índices de IDs válidos (Padres)
        val validCalendarIds = data.calendars.map { it.id }.toSet()
        val validMonthIds = data.months.filter { it.calendarId in validCalendarIds }.map { it.id }.toSet()
        val validWeekIds = data.weeks.filter { it.monthId in validMonthIds }.map { it.id }.toSet()
        val validDaySlotIds = data.daySlots.filter { it.weekId in validWeekIds }.map { it.id }.toSet()
        val validExerciseIds = data.exercises.map { it.id }.toSet()

        // Sets válidos (son padres del historial, pero hijos de ejercicios)
        // Primero filtramos sets que apuntan a ejercicios que no existen
        val validSets = data.sets.filter { it.exerciseId in validExerciseIds }
        val validSetIds = validSets.map { it.id }.toSet()

        // 2. Filtrado de Hijos (Huérfanos)

        // Meses huérfanos
        val cleanMonths = data.months.filter { it.calendarId in validCalendarIds }

        // Semanas huérfanas
        val cleanWeeks = data.weeks.filter { it.monthId in validMonthIds }

        // Días huérfanos
        val cleanDaySlots = data.daySlots.filter { it.weekId in validWeekIds }

        // Relaciones Día-Ejercicio rotas
        val cleanCrossRefs = data.daySlotExercises.filter {
            it.daySlotId in validDaySlotIds && it.exerciseId in validExerciseIds
        }

        // Historial roto
        val cleanHistory = data.history.filter {
            it.exerciseId in validExerciseIds
        }

        Log.d("BackupSanitizer", """
            Datos limpiados:
            - CrossRefs eliminados: ${data.daySlotExercises.size - cleanCrossRefs.size}
            - Historial eliminado: ${data.history.size - cleanHistory.size}
            - Sets eliminados: ${data.sets.size - validSets.size}
        """.trimIndent())

        return data.copy(
            months = cleanMonths,
            weeks = cleanWeeks,
            daySlots = cleanDaySlots,
            sets = validSets,
            daySlotExercises = cleanCrossRefs,
            history = cleanHistory
        )
    }

    private fun tryMigrateFromV2(jsonString: String): BackupData? {
        return try {
            val legacyData = json.decodeFromString<LegacyBackupDataV2>(jsonString)

            val newCrossRefs = mutableListOf<DaySlotExerciseCrossRef>()
            val cleanDaySlots = legacyData.daySlots.map { oldSlot ->
                if (oldSlot.selectedExerciseIds.isNotEmpty()) {
                    val entries = oldSlot.selectedExerciseIds.split(",")
                    entries.forEachIndexed { index, entry ->
                        if (entry.isNotBlank()) {
                            val parts = entry.split("|")
                            val exId = parts[0]
                            val setId = parts.getOrNull(1)?.takeIf { it != "null" && it.isNotEmpty() }

                            newCrossRefs.add(
                                DaySlotExerciseCrossRef(
                                    daySlotId = oldSlot.id,
                                    exerciseId = exId,
                                    targetSetId = setId,
                                    orderIndex = index
                                )
                            )
                        }
                    }
                }
                DaySlotEntity(
                    id = oldSlot.id,
                    weekId = oldSlot.weekId,
                    dayOfWeek = oldSlot.dayOfWeek,
                    categoryList = oldSlot.categoryList,
                    completed = oldSlot.completed
                )
            }

            BackupData(
                calendars = legacyData.calendars,
                months = legacyData.months,
                weeks = legacyData.weeks,
                daySlots = cleanDaySlots,
                exercises = legacyData.exercises,
                sets = legacyData.sets,
                history = legacyData.history,
                daySlotExercises = newCrossRefs
            )
        } catch (e: Exception) {
            Log.e("BackupManager", "Error fatal migrando V2: ${e.message}")
            null
        }
    }

    @Serializable
    private data class LegacyBackupDataV2(
        val exercises: List<ExerciseEntity> = emptyList(),
        val sets: List<SetEntity> = emptyList(),
        val history: List<ExerciseHistoryEntity> = emptyList(),
        val calendars: List<CalendarEntity> = emptyList(),
        val months: List<MonthEntity> = emptyList(),
        val weeks: List<WeekEntity> = emptyList(),
        val daySlots: List<LegacyDaySlotEntityV2> = emptyList()
    )

    @Serializable
    private data class LegacyDaySlotEntityV2(
        val id: String,
        val weekId: String,
        val dayOfWeek: DayOfWeek,
        val categoryList: String = "",
        val selectedExerciseIds: String = "",
        val completed: Boolean = false
    )
}