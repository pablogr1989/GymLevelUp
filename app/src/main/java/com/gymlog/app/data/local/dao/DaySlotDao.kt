package com.gymlog.app.data.local.dao

import androidx.room.*
import com.gymlog.app.data.local.entity.DaySlotEntity
import com.gymlog.app.data.local.entity.DaySlotExerciseCrossRef
import kotlinx.coroutines.flow.Flow

@Dao
interface DaySlotDao {
    @Query("""
    SELECT * FROM day_slots WHERE weekId = :weekId 
    ORDER BY 
    CASE dayOfWeek
        WHEN 'MONDAY' THEN 1
        WHEN 'TUESDAY' THEN 2  
        WHEN 'WEDNESDAY' THEN 3
        WHEN 'THURSDAY' THEN 4
        WHEN 'FRIDAY' THEN 5
        WHEN 'SATURDAY' THEN 6
        WHEN 'SUNDAY' THEN 7
    END ASC
""")
    fun getDaysForWeek(weekId: String): Flow<List<DaySlotEntity>>

    @Query("SELECT * FROM day_slots WHERE id = :dayId")
    suspend fun getDayById(dayId: String): DaySlotEntity?

    // --- NUEVO: Gestión de Relaciones ---

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDaySlotCrossRef(crossRef: DaySlotExerciseCrossRef)

    @Query("DELETE FROM day_slot_exercise_cross_ref WHERE daySlotId = :daySlotId")
    suspend fun clearExercisesForDay(daySlotId: String)

    @Query("SELECT * FROM day_slot_exercise_cross_ref WHERE daySlotId = :daySlotId ORDER BY orderIndex ASC")
    fun getExercisesForDay(daySlotId: String): Flow<List<DaySlotExerciseCrossRef>>

    @Query("SELECT * FROM day_slot_exercise_cross_ref WHERE daySlotId = :daySlotId ORDER BY orderIndex ASC")
    suspend fun getExercisesForDaySync(daySlotId: String): List<DaySlotExerciseCrossRef>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDaySlot(daySlot: DaySlotEntity): Long

    @Update
    suspend fun updateDaySlot(daySlot: DaySlotEntity)

    @Delete
    suspend fun deleteDaySlot(daySlot: DaySlotEntity)

    // Consultas específicas de lógica de negocio
    @Query("UPDATE day_slots SET completed = :completed WHERE id = :dayId")
    suspend fun updateDayCompleted(dayId: String, completed: Boolean)

    @Query("UPDATE day_slots SET completed = :completed WHERE id IN (:dayIds)")
    suspend fun updateMultipleDaysCompleted(dayIds: List<String>, completed: Boolean)

    @Query("UPDATE day_slots SET completed = 0 WHERE weekId IN (SELECT id FROM weeks WHERE monthId IN (SELECT id FROM months WHERE calendarId = :calendarId))")
    suspend fun clearAllCompletedForCalendar(calendarId: String)

    // Actualizado: Ya no usamos LIKE con Strings, borramos directamente de la tabla relacional
    @Query("DELETE FROM day_slot_exercise_cross_ref WHERE exerciseId = :exerciseId")
    suspend fun clearExerciseReferences(exerciseId: String)

    @Query("DELETE FROM day_slots")
    suspend fun deleteAllDaySlots()

    @Query("DELETE FROM day_slot_exercise_cross_ref")
    suspend fun deleteAllCrossRefs()

    // Consulta compleja para obtener todos los días de un calendario (sin cambios funcionales, solo estructura)
    @Query("""
        SELECT day_slots.* FROM day_slots 
        JOIN weeks ON day_slots.weekId = weeks.id 
        JOIN months ON weeks.monthId = months.id 
        WHERE months.calendarId = :calendarId 
        ORDER BY months.monthNumber, weeks.weekNumber, day_slots.dayOfWeek
    """)
    fun getDaysForCalendar(calendarId: String): Flow<List<DaySlotEntity>>

    @Query("SELECT * FROM day_slots")
    fun getAllDaySlots(): Flow<List<DaySlotEntity>>

    @Query("SELECT * FROM day_slot_exercise_cross_ref")
    suspend fun getAllCrossRefs(): List<DaySlotExerciseCrossRef>
}