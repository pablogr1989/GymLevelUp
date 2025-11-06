package com.gymlog.app.ui.screens.calendars

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gymlog.app.domain.model.Calendar
import com.gymlog.app.domain.model.Month
import com.gymlog.app.domain.model.Week
import com.gymlog.app.domain.model.DaySlot
import com.gymlog.app.data.local.entity.DayOfWeek
import com.gymlog.app.domain.repository.CalendarRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class CreateCalendarViewModel @Inject constructor(
    private val repository: CalendarRepository
) : ViewModel() {

    private val _name = MutableStateFlow("")
    val name = _name.asStateFlow()

    private val _monthCount = MutableStateFlow(3)
    val monthCount = _monthCount.asStateFlow()

    private val _showNameError = MutableStateFlow(false)
    val showNameError = _showNameError.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading = _isLoading.asStateFlow()

    private val _navigateBack = MutableStateFlow(false)
    val navigateBack = _navigateBack.asStateFlow()

    fun updateName(value: String) {
        _name.value = value
        _showNameError.value = false
    }

    fun incrementMonths() {
        if (_monthCount.value < 12) {
            _monthCount.value += 1
        }
    }

    fun decrementMonths() {
        if (_monthCount.value > 1) {
            _monthCount.value -= 1
        }
    }

    fun createCalendar() {
        if (_name.value.trim().isEmpty()) {
            _showNameError.value = true
            return
        }

        viewModelScope.launch {
            _isLoading.value = true

            val calendarId = UUID.randomUUID().toString()

            // Crear calendario
            val calendar = Calendar(
                id = calendarId,
                name = _name.value.trim(),
                createdAt = System.currentTimeMillis()
            )
            repository.insertCalendar(calendar)

            // Crear meses, semanas y días
            repeat(_monthCount.value) { monthIndex ->
                val monthId = UUID.randomUUID().toString()
                val month = Month(
                    id = monthId,
                    calendarId = calendarId,
                    name = "Mes ${monthIndex + 1}",
                    monthNumber = monthIndex + 1
                )
                repository.insertMonth(month)

                // Crear 4 semanas por mes
                repeat(4) { weekIndex ->
                    val weekId = UUID.randomUUID().toString()
                    val week = Week(
                        id = weekId,
                        monthId = monthId,
                        weekNumber = weekIndex + 1
                    )
                    repository.insertWeek(week)

                    // Crear 7 días por semana
                    DayOfWeek.entries.forEach { dayOfWeek ->
                        val daySlot = DaySlot(
                            id = UUID.randomUUID().toString(),
                            weekId = weekId,
                            dayOfWeek = dayOfWeek,
                            categories = emptyList(),
                            selectedExerciseIds = emptyList(),
                            completed = false
                        )
                        repository.insertDaySlot(daySlot)
                    }
                }
            }

            _isLoading.value = false
            _navigateBack.value = true
        }
    }

    fun resetNavigation() {
        _navigateBack.value = false
    }
}