package com.gymlog.app.data.repository

import com.gymlog.app.data.local.dao.*
import com.gymlog.app.data.local.entity.*
import com.gymlog.app.domain.model.*
import com.gymlog.app.domain.repository.CalendarRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CalendarRepositoryImpl @Inject constructor(
    private val calendarDao: CalendarDao,
    private val monthDao: MonthDao,
    private val weekDao: WeekDao,
    private val daySlotDao: DaySlotDao
) : CalendarRepository {

    // Calendar operations
    override fun getAllCalendars(): Flow<List<Calendar>> {
        return calendarDao.getAllCalendars().map { entities ->
            entities.map { it.toDomainModel() }
        }
    }

    override suspend fun getCalendarById(calendarId: String): Calendar? {
        return calendarDao.getCalendarById(calendarId)?.toDomainModel()
    }

    override suspend fun insertCalendar(calendar: Calendar) {
        calendarDao.insertCalendar(calendar.toEntity())
    }

    override suspend fun updateCalendar(calendar: Calendar) {
        calendarDao.updateCalendar(calendar.toEntity())
    }

    override suspend fun deleteCalendar(calendar: Calendar) {
        calendarDao.deleteCalendar(calendar.toEntity())
    }

    // Month operations
    override fun getMonthsForCalendar(calendarId: String): Flow<List<Month>> {
        return monthDao.getMonthsForCalendar(calendarId).map { entities ->
            entities.map { it.toDomainModel() }
        }
    }

    override suspend fun getMonthById(monthId: String): Month? {
        return monthDao.getMonthById(monthId)?.toDomainModel()
    }

    override suspend fun insertMonth(month: Month) {
        monthDao.insertMonth(month.toEntity())
    }

    override suspend fun updateMonth(month: Month) {
        monthDao.updateMonth(month.toEntity())
    }

    override suspend fun deleteMonth(month: Month) {
        monthDao.deleteMonth(month.toEntity())
    }

    // Week operations
    override fun getWeeksForMonth(monthId: String): Flow<List<Week>> {
        return weekDao.getWeeksForMonth(monthId).map { entities ->
            entities.map { it.toDomainModel() }
        }
    }

    override suspend fun getWeekById(weekId: String): Week? {
        return weekDao.getWeekById(weekId)?.toDomainModel()
    }

    override suspend fun insertWeek(week: Week) {
        weekDao.insertWeek(week.toEntity())
    }

    override suspend fun updateWeek(week: Week) {
        weekDao.updateWeek(week.toEntity())
    }

    override suspend fun deleteWeek(week: Week) {
        weekDao.deleteWeek(week.toEntity())
    }

    // DaySlot operations
    override fun getDaysForWeek(weekId: String): Flow<List<DaySlot>> {
        return daySlotDao.getDaysForWeek(weekId).map { entities ->
            entities.map { it.toDomainModel() }
        }
    }

    override suspend fun getDayById(dayId: String): DaySlot? {
        return daySlotDao.getDayById(dayId)?.toDomainModel()
    }

    override suspend fun insertDaySlot(daySlot: DaySlot) {
        daySlotDao.insertDaySlot(daySlot.toEntity())
    }

    override suspend fun updateDaySlot(daySlot: DaySlot) {
        daySlotDao.updateDaySlot(daySlot.toEntity())
    }

    override suspend fun deleteDaySlot(daySlot: DaySlot) {
        daySlotDao.deleteDaySlot(daySlot.toEntity())
    }

    override suspend fun updateDayCompleted(dayId: String, completed: Boolean) {
        daySlotDao.updateDayCompleted(dayId, completed)
    }

    override suspend fun updateMultipleDaysCompleted(dayIds: List<String>, completed: Boolean) {
        daySlotDao.updateMultipleDaysCompleted(dayIds, completed)
    }

    override suspend fun clearAllCompletedForCalendar(calendarId: String) {
        daySlotDao.clearAllCompletedForCalendar(calendarId)
    }

    override suspend fun clearExerciseReferences(exerciseId: String) {
        daySlotDao.clearExerciseReferences(exerciseId)
    }

    // Composite operations
    override suspend fun getCalendarWithMonths(calendarId: String): CalendarWithMonths? {
        val calendar = calendarDao.getCalendarById(calendarId)?.toDomainModel() ?: return null
        val months = monthDao.getMonthsForCalendar(calendarId).first()

        val monthsWithWeeks = months.map { monthEntity ->
            val month = monthEntity.toDomainModel()
            val weeks = weekDao.getWeeksForMonth(month.id).first()

            val weeksWithDays = weeks.map { weekEntity ->
                val week = weekEntity.toDomainModel()
                val days = daySlotDao.getDaysForWeek(week.id).first().map { it.toDomainModel() }
                WeekWithDays(week, days)
            }

            MonthWithWeeks(month, weeksWithDays)
        }

        return CalendarWithMonths(calendar, monthsWithWeeks)
    }

    override fun getCalendarWithMonthsFlow(calendarId: String): Flow<CalendarWithMonths?> {
        return daySlotDao.getDaysForCalendar(calendarId).map { _ ->
            // Cuando cambian los DaySlots, reconstruimos todo el CalendarWithMonths
            val calendar = calendarDao.getCalendarById(calendarId)?.toDomainModel() ?: return@map null
            val months = monthDao.getMonthsForCalendar(calendarId).first()

            val monthsWithWeeks = months.map { monthEntity ->
                val month = monthEntity.toDomainModel()
                val weeks = weekDao.getWeeksForMonth(month.id).first()

                val weeksWithDays = weeks.map { weekEntity ->
                    val week = weekEntity.toDomainModel()
                    val days = daySlotDao.getDaysForWeek(week.id).first().map { it.toDomainModel() }
                    WeekWithDays(week, days)
                }

                MonthWithWeeks(month, weeksWithDays)
            }

            CalendarWithMonths(calendar, monthsWithWeeks)
        }
    }

    override suspend fun getMonthWithWeeks(monthId: String): MonthWithWeeks? {
        val month = monthDao.getMonthById(monthId)?.toDomainModel() ?: return null
        val weeks = weekDao.getWeeksForMonth(monthId).first()

        val weeksWithDays = weeks.map { weekEntity ->
            val week = weekEntity.toDomainModel()
            val days = daySlotDao.getDaysForWeek(week.id).first().map { it.toDomainModel() }
            WeekWithDays(week, days)
        }

        return MonthWithWeeks(month, weeksWithDays)
    }

    override suspend fun getWeekWithDays(weekId: String): WeekWithDays? {
        val week = weekDao.getWeekById(weekId)?.toDomainModel() ?: return null
        val days = daySlotDao.getDaysForWeek(weekId).first().map { it.toDomainModel() }
        return WeekWithDays(week, days)
    }

    // Mapper functions
    private fun CalendarEntity.toDomainModel() = Calendar(id, name, createdAt)
    private fun Calendar.toEntity() = CalendarEntity(id, name, createdAt)

    private fun MonthEntity.toDomainModel() = Month(id, calendarId, name, monthNumber)
    private fun Month.toEntity() = MonthEntity(id, calendarId, name, monthNumber)

    private fun WeekEntity.toDomainModel() = Week(id, monthId, weekNumber)
    private fun Week.toEntity() = WeekEntity(id, monthId, weekNumber)

    private fun DaySlotEntity.toDomainModel() = DaySlot(
        id = id,
        weekId = weekId,
        dayOfWeek = dayOfWeek,
        categories = parseCategoryList(categoryList),
        selectedExerciseIds = parseExerciseIdList(selectedExerciseIds),
        completed = completed
    )

    private fun DaySlot.toEntity() = DaySlotEntity(
        id = id,
        weekId = weekId,
        dayOfWeek = dayOfWeek,
        categoryList = serializeCategoryList(categories),
        selectedExerciseIds = serializeExerciseIdList(selectedExerciseIds),
        completed = completed
    )

    private fun parseCategoryList(categoryList: String): List<DayCategory> {
        if (categoryList.isEmpty()) return emptyList()
        return categoryList.split(",").mapNotNull {
            try { DayCategory.valueOf(it.trim()) } catch (e: Exception) { null }
        }
    }

    private fun serializeCategoryList(categories: List<DayCategory>): String {
        return categories.joinToString(",") { it.name }
    }

    private fun parseExerciseIdList(exerciseIds: String): List<String> {
        if (exerciseIds.isEmpty()) return emptyList()
        return exerciseIds.split(",").map { it.trim() }.filter { it.isNotEmpty() }
    }

    private fun serializeExerciseIdList(exerciseIds: List<String>): String {
        return exerciseIds.joinToString(",")
    }
}