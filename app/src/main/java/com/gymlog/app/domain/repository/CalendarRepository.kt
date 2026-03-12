package com.gymlog.app.domain.repository

import com.gymlog.app.domain.model.*
import kotlinx.coroutines.flow.Flow

interface CalendarRepository {
    fun getAllCalendars(): Flow<List<Calendar>>
    suspend fun getCalendarById(calendarId: String): Calendar?
    suspend fun insertCalendar(calendar: Calendar)
    suspend fun updateCalendar(calendar: Calendar)
    suspend fun deleteCalendar(calendar: Calendar)

    fun getMonthsForCalendar(calendarId: String): Flow<List<Month>>
    suspend fun getMonthById(monthId: String): Month?
    suspend fun insertMonth(month: Month)
    suspend fun updateMonth(month: Month)
    suspend fun deleteMonth(month: Month)

    fun getWeeksForMonth(monthId: String): Flow<List<Week>>
    suspend fun getWeekById(weekId: String): Week?
    suspend fun insertWeek(week: Week)
    suspend fun updateWeek(week: Week)
    suspend fun deleteWeek(week: Week)

    fun getDaysForWeek(weekId: String): Flow<List<DaySlot>>
    suspend fun getDayById(dayId: String): DaySlot?
    suspend fun insertDaySlot(daySlot: DaySlot)
    suspend fun updateDaySlot(daySlot: DaySlot)
    suspend fun deleteDaySlot(daySlot: DaySlot)
    suspend fun updateDayCompleted(dayId: String, completed: Boolean)
    suspend fun updateMultipleDaysCompleted(dayIds: List<String>, completed: Boolean)
    suspend fun clearAllCompletedForCalendar(calendarId: String)
    suspend fun clearExerciseReferences(exerciseId: String)

    // --- NUEVAS HERRAMIENTAS AVANZADAS ---
    suspend fun swapDaySlots(daySlotId1: String, daySlotId2: String)
    suspend fun copyDaySlots(sourceIds: List<String>, targetIds: List<String>)

    suspend fun swapWeeks(weekId1: String, weekId2: String)
    suspend fun copyWeeks(sourceWeekIds: List<String>, targetWeekIds: List<String>)

    suspend fun swapMonths(monthId1: String, monthId2: String)
    suspend fun copyMonths(sourceMonthId: String, targetMonthId: String)

    // Composite operations
    suspend fun getCalendarWithMonths(calendarId: String): CalendarWithMonths?
    suspend fun getMonthWithWeeks(monthId: String): MonthWithWeeks?
    fun getCalendarWithMonthsFlow(calendarId: String): Flow<CalendarWithMonths?>
    suspend fun getWeekWithDays(weekId: String): WeekWithDays?
}