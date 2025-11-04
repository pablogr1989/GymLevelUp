package com.gymlog.app.data.local.dao

import androidx.room.*
import com.gymlog.app.data.local.entity.*
import kotlinx.coroutines.flow.Flow

@Dao
interface CalendarDao {
    @Query("SELECT * FROM calendars ORDER BY createdAt DESC")
    fun getAllCalendars(): Flow<List<CalendarEntity>>
    
    @Query("SELECT * FROM calendars WHERE id = :calendarId")
    suspend fun getCalendarById(calendarId: String): CalendarEntity?
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCalendar(calendar: CalendarEntity): Long
    
    @Update
    suspend fun updateCalendar(calendar: CalendarEntity)
    
    @Delete
    suspend fun deleteCalendar(calendar: CalendarEntity)
    
    @Query("DELETE FROM calendars WHERE id = :calendarId")
    suspend fun deleteCalendarById(calendarId: String)
}

@Dao
interface MonthDao {
    @Query("SELECT * FROM months WHERE calendarId = :calendarId ORDER BY monthNumber ASC")
    fun getMonthsForCalendar(calendarId: String): Flow<List<MonthEntity>>
    
    @Query("SELECT * FROM months WHERE id = :monthId")
    suspend fun getMonthById(monthId: String): MonthEntity?
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMonth(month: MonthEntity): Long
    
    @Update
    suspend fun updateMonth(month: MonthEntity)
    
    @Delete
    suspend fun deleteMonth(month: MonthEntity)
    
    @Query("DELETE FROM months WHERE calendarId = :calendarId")
    suspend fun deleteMonthsForCalendar(calendarId: String)
}

@Dao
interface WeekDao {
    @Query("SELECT * FROM weeks WHERE monthId = :monthId ORDER BY weekNumber ASC")
    fun getWeeksForMonth(monthId: String): Flow<List<WeekEntity>>
    
    @Query("SELECT * FROM weeks WHERE id = :weekId")
    suspend fun getWeekById(weekId: String): WeekEntity?
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWeek(week: WeekEntity): Long
    
    @Update
    suspend fun updateWeek(week: WeekEntity)
    
    @Delete
    suspend fun deleteWeek(week: WeekEntity)
}

@Dao
interface DaySlotDao {
    @Query("SELECT * FROM day_slots WHERE weekId = :weekId ORDER BY dayOfWeek ASC")
    fun getDaysForWeek(weekId: String): Flow<List<DaySlotEntity>>
    
    @Query("SELECT * FROM day_slots WHERE id = :dayId")
    suspend fun getDayById(dayId: String): DaySlotEntity?
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDaySlot(daySlot: DaySlotEntity): Long
    
    @Update
    suspend fun updateDaySlot(daySlot: DaySlotEntity)
    
    @Delete
    suspend fun deleteDaySlot(daySlot: DaySlotEntity)
    
    @Query("UPDATE day_slots SET completed = :completed WHERE id = :dayId")
    suspend fun updateDayCompleted(dayId: String, completed: Boolean)
    
    @Query("UPDATE day_slots SET completed = :completed WHERE id IN (:dayIds)")
    suspend fun updateMultipleDaysCompleted(dayIds: List<String>, completed: Boolean)
    
    @Query("UPDATE day_slots SET completed = 0 WHERE weekId IN (SELECT id FROM weeks WHERE monthId IN (SELECT id FROM months WHERE calendarId = :calendarId))")
    suspend fun clearAllCompletedForCalendar(calendarId: String)
    
    @Query("UPDATE day_slots SET selectedExerciseId = NULL WHERE selectedExerciseId = :exerciseId")
    suspend fun clearExerciseReferences(exerciseId: String)
}
