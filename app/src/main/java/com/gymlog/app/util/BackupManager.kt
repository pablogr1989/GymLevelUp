package com.gymlog.app.util

import android.content.Context
import android.net.Uri
import android.os.Environment
import com.gymlog.app.data.backup.BackupData
import com.gymlog.app.data.local.GymLogDatabase
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File
import java.io.FileWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton
import androidx.room.withTransaction

@Singleton
class BackupManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val db: GymLogDatabase
) {

    private val json = Json { prettyPrint = true; ignoreUnknownKeys = true }

    /**
     * Exporta todos los datos de la base de datos a un archivo JSON en el almacenamiento interno/externo.
     * Retorna la ruta absoluta del archivo creado.
     */
    suspend fun exportDataToJson(destinationUri: Uri): Unit = withContext(Dispatchers.IO) {
        // 1. Recolectar todos los datos
        val allData = BackupData(
            exercises = db.exerciseDao().getAllExercises().first(),
            history = db.exerciseHistoryDao().getAllHistory().first(),
            calendars = db.calendarDao().getAllCalendars().first(),
            months = db.monthDao().getAllMonths().first(),
            weeks = db.weekDao().getAllWeeks().first(),
            daySlots = db.daySlotDao().getAllDaySlots().first()
        )

        val jsonString = json.encodeToString(allData)

        context.contentResolver.openOutputStream(destinationUri)?.use { outputStream ->
            outputStream.bufferedWriter().use { writer ->
                writer.write(jsonString)
            }
        } ?: throw IllegalStateException("No se pudo abrir el stream de escritura para el archivo seleccionado.")
    }

    /**
     * Importa datos desde un archivo JSON (URI de SAF) y los reemplaza en la base de datos.
     * ¡ADVERTENCIA! Esta operación es destructiva y elimina todos los datos existentes.
     * @param uri URI del archivo JSON proporcionado por el SAF.
     */
    suspend fun importDataFromJson(uri: Uri): Unit = withContext(Dispatchers.IO) {

        // Usamos ContentResolver para abrir el archivo desde la URI del SAF
        context.contentResolver.openInputStream(uri)?.use { inputStream ->
            val jsonString = inputStream.bufferedReader().use { it.readText() }
            val backupData = json.decodeFromString<BackupData>(jsonString)

            // 2. Reemplazar datos en una transacción atómica
            db.withTransaction {
                // Paso 1: Limpiar la base de datos (destructivo)
                db.exerciseHistoryDao().deleteAllHistory()
                db.exerciseDao().deleteAllExercises()
                db.daySlotDao().deleteAllDaySlots()
                db.weekDao().deleteAllWeeks()
                db.monthDao().deleteAllMonths()
                db.calendarDao().deleteAllCalendars()

                // Paso 2: Reinsertar datos (orden crucial debido a Foreign Keys)
                backupData.calendars.forEach { db.calendarDao().insertCalendar(it) }
                backupData.months.forEach { db.monthDao().insertMonth(it) }
                backupData.weeks.forEach { db.weekDao().insertWeek(it) }
                backupData.daySlots.forEach { db.daySlotDao().insertDaySlot(it) }

                // Paso 3: Reinsertar Ejercicios e Historial
                backupData.exercises.forEach { db.exerciseDao().insertExercise(it) }
                backupData.history.forEach { db.exerciseHistoryDao().insertHistory(it) }
            }
        } ?: throw IllegalStateException("No se pudo abrir el stream de lectura para el archivo seleccionado. Asegúrate de tener el permiso de lectura.")
    }
}