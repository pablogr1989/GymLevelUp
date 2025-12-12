package com.gymlog.app.data.repository

import androidx.room.withTransaction
import com.gymlog.app.data.local.GymLogDatabase
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
    private val daySlotDao: DaySlotDao,
    private val database: GymLogDatabase
) : CalendarRepository {

    // --- Calendar Operations ---

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

    // --- Month Operations ---

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

    // --- Week Operations ---

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

    // --- DaySlot Operations (CLEANED UP) ---

    override fun getDaysForWeek(weekId: String): Flow<List<DaySlot>> {
        return daySlotDao.getDaysForWeek(weekId).map { entities ->
            entities.map { entity ->
                val crossRefs = daySlotDao.getExercisesForDaySync(entity.id)
                entity.toDomainModel(crossRefs)
            }
        }
    }

    override suspend fun getDayById(dayId: String): DaySlot? {
        val entity = daySlotDao.getDayById(dayId) ?: return null
        val crossRefs = daySlotDao.getExercisesForDaySync(dayId)
        return entity.toDomainModel(crossRefs)
    }

    override suspend fun insertDaySlot(daySlot: DaySlot) {
        database.withTransaction {
            daySlotDao.insertDaySlot(daySlot.toEntity())
            saveDaySlotExercises(daySlot.id, daySlot.exercises)
        }
    }

    override suspend fun updateDaySlot(daySlot: DaySlot) {
        database.withTransaction {
            daySlotDao.updateDaySlot(daySlot.toEntity())
            saveDaySlotExercises(daySlot.id, daySlot.exercises)
        }
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

    override suspend fun swapDaySlots(daySlotId1: String, daySlotId2: String) {
        database.withTransaction {
            val day1 = daySlotDao.getDayById(daySlotId1)
            val day2 = daySlotDao.getDayById(daySlotId2)

            if (day1 != null && day2 != null) {
                // 1. Obtener referencias
                val refs1 = daySlotDao.getExercisesForDaySync(daySlotId1)
                val refs2 = daySlotDao.getExercisesForDaySync(daySlotId2)

                // 2. Intercambiar datos básicos
                val updatedDay1 = day1.copy(
                    categoryList = day2.categoryList,
                    completed = day2.completed
                )
                val updatedDay2 = day2.copy(
                    categoryList = day1.categoryList,
                    completed = day1.completed
                )

                daySlotDao.updateDaySlot(updatedDay1)
                daySlotDao.updateDaySlot(updatedDay2)

                // 3. Intercambiar referencias
                daySlotDao.clearExercisesForDay(daySlotId1)
                daySlotDao.clearExercisesForDay(daySlotId2)

                refs2.forEach { ref ->
                    daySlotDao.insertDaySlotCrossRef(ref.copy(daySlotId = daySlotId1))
                }
                refs1.forEach { ref ->
                    daySlotDao.insertDaySlotCrossRef(ref.copy(daySlotId = daySlotId2))
                }
            }
        }
    }

    // --- Composite Operations ---

    override suspend fun getCalendarWithMonths(calendarId: String): CalendarWithMonths? {
        val calendar = calendarDao.getCalendarById(calendarId)?.toDomainModel() ?: return null
        val months = monthDao.getMonthsForCalendar(calendarId).first()

        val allCrossRefs = daySlotDao.getAllCrossRefs().groupBy { it.daySlotId }

        val monthsWithWeeks = months.map { monthEntity ->
            val month = monthEntity.toDomainModel()
            val weeks = weekDao.getWeeksForMonth(month.id).first()

            val weeksWithDays = weeks.map { weekEntity ->
                val week = weekEntity.toDomainModel()
                val days = daySlotDao.getDaysForWeek(week.id).first().map { entity ->
                    val refs = allCrossRefs[entity.id] ?: emptyList()
                    entity.toDomainModel(refs)
                }
                WeekWithDays(week, days)
            }
            MonthWithWeeks(month, weeksWithDays)
        }

        return CalendarWithMonths(calendar, monthsWithWeeks)
    }

    override fun getCalendarWithMonthsFlow(calendarId: String): Flow<CalendarWithMonths?> {
        return daySlotDao.getDaysForCalendar(calendarId).map { _ ->
            getCalendarWithMonths(calendarId)
        }
    }

    override suspend fun getMonthWithWeeks(monthId: String): MonthWithWeeks? {
        val month = monthDao.getMonthById(monthId)?.toDomainModel() ?: return null
        val weeks = weekDao.getWeeksForMonth(monthId).first()

        val weeksWithDays = weeks.map { weekEntity ->
            val week = weekEntity.toDomainModel()
            val days = daySlotDao.getDaysForWeek(week.id).first().map { entity ->
                val refs = daySlotDao.getExercisesForDaySync(entity.id)
                entity.toDomainModel(refs)
            }
            WeekWithDays(week, days)
        }
        return MonthWithWeeks(month, weeksWithDays)
    }

    override suspend fun getWeekWithDays(weekId: String): WeekWithDays? {
        val week = weekDao.getWeekById(weekId)?.toDomainModel() ?: return null
        val days = daySlotDao.getDaysForWeek(weekId).first().map { entity ->
            val refs = daySlotDao.getExercisesForDaySync(entity.id)
            entity.toDomainModel(refs)
        }
        return WeekWithDays(week, days)
    }

    // --- Helpers ---

    private suspend fun saveDaySlotExercises(daySlotId: String, assignments: List<TrainingAssignment>) {
        daySlotDao.clearExercisesForDay(daySlotId)
        assignments.forEachIndexed { index, assignment ->
            daySlotDao.insertDaySlotCrossRef(
                DaySlotExerciseCrossRef(
                    daySlotId = daySlotId,
                    exerciseId = assignment.exerciseId,
                    targetSetId = assignment.targetSetId,
                    orderIndex = index
                )
            )
        }
    }

    // --- Mappers ---

    private fun CalendarEntity.toDomainModel() = Calendar(id, name, createdAt)
    private fun Calendar.toEntity() = CalendarEntity(id, name, createdAt)

    private fun MonthEntity.toDomainModel() = Month(id, calendarId, name, monthNumber)
    private fun Month.toEntity() = MonthEntity(id, calendarId, name, monthNumber)

    private fun WeekEntity.toDomainModel() = Week(id, monthId, weekNumber)
    private fun Week.toEntity() = WeekEntity(id, monthId, weekNumber)

    private fun DaySlotEntity.toDomainModel(crossRefs: List<DaySlotExerciseCrossRef> = emptyList()): DaySlot {
        // Mapeo limpio: Entidad -> Modelo de Dominio
        val assignments = crossRefs
            .sortedBy { it.orderIndex }
            .map { TrainingAssignment(it.exerciseId, it.targetSetId) }

        return DaySlot(
            id = id,
            weekId = weekId,
            dayOfWeek = dayOfWeek,
            categories = parseCategoryList(categoryList),
            exercises = assignments, // Ahora pasamos objetos puros
            completed = completed
        )
    }

    private fun DaySlot.toEntity() = DaySlotEntity(
        id = id,
        weekId = weekId,
        dayOfWeek = dayOfWeek,
        categoryList = serializeCategoryList(categories),
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
}