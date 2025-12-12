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

    @Query("DELETE FROM calendars")
    suspend fun deleteAllCalendars()
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

    @Query("DELETE FROM months")
    suspend fun deleteAllMonths()

    @Query("SELECT * FROM months")
    fun getAllMonths(): Flow<List<MonthEntity>>
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

    @Query("DELETE FROM weeks")
    suspend fun deleteAllWeeks()

    @Query("SELECT * FROM weeks")
    fun getAllWeeks(): Flow<List<WeekEntity>>
}

// ¡IMPORTANTE! He eliminado la interfaz DaySlotDao de aquí.
// Ahora debe vivir exclusivamente en su propio archivo: DaySlotDao.kt