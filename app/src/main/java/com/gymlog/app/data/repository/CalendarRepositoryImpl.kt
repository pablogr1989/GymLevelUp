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

    override fun getAllCalendars(): Flow<List<Calendar>> { return calendarDao.getAllCalendars().map { entities -> entities.map { it.toDomainModel() } } }
    override suspend fun getCalendarById(calendarId: String): Calendar? { return calendarDao.getCalendarById(calendarId)?.toDomainModel() }
    override suspend fun insertCalendar(calendar: Calendar) { calendarDao.insertCalendar(calendar.toEntity()) }
    override suspend fun updateCalendar(calendar: Calendar) { calendarDao.updateCalendar(calendar.toEntity()) }
    override suspend fun deleteCalendar(calendar: Calendar) { calendarDao.deleteCalendar(calendar.toEntity()) }

    override fun getMonthsForCalendar(calendarId: String): Flow<List<Month>> { return monthDao.getMonthsForCalendar(calendarId).map { entities -> entities.map { it.toDomainModel() } } }
    override suspend fun getMonthById(monthId: String): Month? { return monthDao.getMonthById(monthId)?.toDomainModel() }
    override suspend fun insertMonth(month: Month) { monthDao.insertMonth(month.toEntity()) }
    override suspend fun updateMonth(month: Month) { monthDao.updateMonth(month.toEntity()) }
    override suspend fun deleteMonth(month: Month) { monthDao.deleteMonth(month.toEntity()) }

    override fun getWeeksForMonth(monthId: String): Flow<List<Week>> { return weekDao.getWeeksForMonth(monthId).map { entities -> entities.map { it.toDomainModel() } } }
    override suspend fun getWeekById(weekId: String): Week? { return weekDao.getWeekById(weekId)?.toDomainModel() }
    override suspend fun insertWeek(week: Week) { weekDao.insertWeek(week.toEntity()) }
    override suspend fun updateWeek(week: Week) { weekDao.updateWeek(week.toEntity()) }
    override suspend fun deleteWeek(week: Week) { weekDao.deleteWeek(week.toEntity()) }

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

    override suspend fun deleteDaySlot(daySlot: DaySlot) { daySlotDao.deleteDaySlot(daySlot.toEntity()) }
    override suspend fun updateDayCompleted(dayId: String, completed: Boolean) { daySlotDao.updateDayCompleted(dayId, completed) }
    override suspend fun updateMultipleDaysCompleted(dayIds: List<String>, completed: Boolean) { daySlotDao.updateMultipleDaysCompleted(dayIds, completed) }
    override suspend fun clearAllCompletedForCalendar(calendarId: String) { daySlotDao.clearAllCompletedForCalendar(calendarId) }
    override suspend fun clearExerciseReferences(exerciseId: String) { daySlotDao.clearExerciseReferences(exerciseId) }

    // --- FUNCIONES DE CLONADO Y MOVIMIENTO ---

    override suspend fun swapDaySlots(daySlotId1: String, daySlotId2: String) {
        database.withTransaction {
            val day1 = daySlotDao.getDayById(daySlotId1)
            val day2 = daySlotDao.getDayById(daySlotId2)
            if (day1 != null && day2 != null) {
                val refs1 = daySlotDao.getExercisesForDaySync(daySlotId1)
                val refs2 = daySlotDao.getExercisesForDaySync(daySlotId2)

                daySlotDao.updateDaySlot(day1.copy(categoryList = day2.categoryList, completed = day2.completed))
                daySlotDao.updateDaySlot(day2.copy(categoryList = day1.categoryList, completed = day1.completed))

                daySlotDao.clearExercisesForDay(daySlotId1)
                daySlotDao.clearExercisesForDay(daySlotId2)

                refs2.forEach { daySlotDao.insertDaySlotCrossRef(it.copy(daySlotId = daySlotId1)) }
                refs1.forEach { daySlotDao.insertDaySlotCrossRef(it.copy(daySlotId = daySlotId2)) }
            }
        }
    }

    override suspend fun copyDaySlots(sourceIds: List<String>, targetIds: List<String>) {
        database.withTransaction {
            for (i in sourceIds.indices) {
                val sourceDay = daySlotDao.getDayById(sourceIds[i])
                val targetDay = daySlotDao.getDayById(targetIds[i])
                if (sourceDay != null && targetDay != null) {

                    // AQUÍ ESTÁ LA CORRECCIÓN: Ahora también copia el estado "completed"
                    daySlotDao.updateDaySlot(targetDay.copy(
                        categoryList = sourceDay.categoryList,
                        completed = sourceDay.completed
                    ))

                    val sourceExercises = daySlotDao.getExercisesForDaySync(sourceIds[i])
                    daySlotDao.clearExercisesForDay(targetIds[i])
                    sourceExercises.forEach { ref ->
                        daySlotDao.insertDaySlotCrossRef(ref.copy(daySlotId = targetIds[i]))
                    }
                }
            }
        }
    }

    override suspend fun swapWeeks(weekId1: String, weekId2: String) {
        database.withTransaction {
            val days1 = daySlotDao.getDaysForWeek(weekId1).first().sortedBy { it.dayOfWeek.ordinal }
            val days2 = daySlotDao.getDaysForWeek(weekId2).first().sortedBy { it.dayOfWeek.ordinal }
            val size = minOf(days1.size, days2.size)
            for (i in 0 until size) {
                swapDaySlots(days1[i].id, days2[i].id)
            }
        }
    }

    override suspend fun copyWeeks(sourceWeekIds: List<String>, targetWeekIds: List<String>) {
        database.withTransaction {
            for (i in sourceWeekIds.indices) {
                val days1 = daySlotDao.getDaysForWeek(sourceWeekIds[i]).first().sortedBy { it.dayOfWeek.ordinal }
                val days2 = daySlotDao.getDaysForWeek(targetWeekIds[i]).first().sortedBy { it.dayOfWeek.ordinal }
                val size = minOf(days1.size, days2.size)
                copyDaySlots(days1.take(size).map { it.id }, days2.take(size).map { it.id })
            }
        }
    }

    override suspend fun swapMonths(monthId1: String, monthId2: String) {
        database.withTransaction {
            val weeks1 = weekDao.getWeeksForMonth(monthId1).first().sortedBy { it.weekNumber }
            val weeks2 = weekDao.getWeeksForMonth(monthId2).first().sortedBy { it.weekNumber }
            val size = minOf(weeks1.size, weeks2.size)
            for (i in 0 until size) {
                swapWeeks(weeks1[i].id, weeks2[i].id)
            }
        }
    }

    override suspend fun copyMonths(sourceMonthId: String, targetMonthId: String) {
        database.withTransaction {
            val weeks1 = weekDao.getWeeksForMonth(sourceMonthId).first().sortedBy { it.weekNumber }
            val weeks2 = weekDao.getWeeksForMonth(targetMonthId).first().sortedBy { it.weekNumber }
            val size = minOf(weeks1.size, weeks2.size)
            copyWeeks(weeks1.take(size).map { it.id }, weeks2.take(size).map { it.id })
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
        return daySlotDao.getDaysForCalendar(calendarId).map { _ -> getCalendarWithMonths(calendarId) }
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

    private suspend fun saveDaySlotExercises(daySlotId: String, assignments: List<TrainingAssignment>) {
        daySlotDao.clearExercisesForDay(daySlotId)
        assignments.forEachIndexed { index, assignment ->
            daySlotDao.insertDaySlotCrossRef(
                DaySlotExerciseCrossRef(daySlotId, assignment.exerciseId, assignment.targetSetId, index)
            )
        }
    }

    private fun CalendarEntity.toDomainModel() = Calendar(id, name, createdAt)
    private fun Calendar.toEntity() = CalendarEntity(id, name, createdAt)
    private fun MonthEntity.toDomainModel() = Month(id, calendarId, name, monthNumber)
    private fun Month.toEntity() = MonthEntity(id, calendarId, name, monthNumber)
    private fun WeekEntity.toDomainModel() = Week(id, monthId, weekNumber)
    private fun Week.toEntity() = WeekEntity(id, monthId, weekNumber)
    private fun DaySlotEntity.toDomainModel(crossRefs: List<DaySlotExerciseCrossRef> = emptyList()): DaySlot {
        val assignments = crossRefs.sortedBy { it.orderIndex }.map { TrainingAssignment(it.exerciseId, it.targetSetId) }
        return DaySlot(id, weekId, dayOfWeek, parseCategoryList(categoryList), assignments, completed)
    }
    private fun DaySlot.toEntity() = DaySlotEntity(id, weekId, dayOfWeek, serializeCategoryList(categories), completed)
    private fun parseCategoryList(categoryList: String): List<DayCategory> {
        if (categoryList.isEmpty()) return emptyList()
        return categoryList.split(",").mapNotNull { try { DayCategory.valueOf(it.trim()) } catch (e: Exception) { null } }
    }
    private fun serializeCategoryList(categories: List<DayCategory>): String { return categories.joinToString(",") { it.name } }
}