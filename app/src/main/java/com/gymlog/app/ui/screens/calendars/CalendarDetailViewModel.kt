package com.gymlog.app.ui.screens.calendars

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gymlog.app.domain.model.*
import com.gymlog.app.domain.repository.CalendarRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class CalendarDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val repository: CalendarRepository
) : ViewModel() {
    
    private val calendarId: String = savedStateHandle.get<String>("calendarId") ?: ""
    
    private val _calendarWithMonths = MutableStateFlow<CalendarWithMonths?>(null)
    val calendarWithMonths = _calendarWithMonths.asStateFlow()
    
    private val _currentMonthIndex = MutableStateFlow(0)
    val currentMonthIndex = _currentMonthIndex.asStateFlow()
    
    private val _selectedDayIds = MutableStateFlow<Set<String>>(emptySet())
    val selectedDayIds = _selectedDayIds.asStateFlow()
    
    private val _isSelectionMode = MutableStateFlow(false)
    val isSelectionMode = _isSelectionMode.asStateFlow()
    
    private val _showClearAllDialog = MutableStateFlow(false)
    val showClearAllDialog = _showClearAllDialog.asStateFlow()
    
    init {
        loadCalendar()
    }
    
    private fun loadCalendar() {
        viewModelScope.launch {
            val data = repository.getCalendarWithMonths(calendarId)
            _calendarWithMonths.value = data
        }
    }
    
    fun changeMonth(delta: Int) {
        val current = _currentMonthIndex.value
        val maxIndex = (_calendarWithMonths.value?.months?.size ?: 1) - 1
        val newIndex = (current + delta).coerceIn(0, maxIndex)
        _currentMonthIndex.value = newIndex
    }
    
    fun toggleDaySelection(dayId: String) {
        val current = _selectedDayIds.value
        _selectedDayIds.value = if (current.contains(dayId)) {
            current - dayId
        } else {
            current + dayId
        }
        
        _isSelectionMode.value = _selectedDayIds.value.isNotEmpty()
        android.util.Log.d("CalendarDetail", "Selection mode: ${_isSelectionMode.value}, Selected: ${_selectedDayIds.value.size}")
    }
    
    fun clearSelection() {
        _selectedDayIds.value = emptySet()
        _isSelectionMode.value = false
    }
    
    fun markSelectedAsCompleted() {
        viewModelScope.launch {
            repository.updateMultipleDaysCompleted(_selectedDayIds.value.toList(), true)
            clearSelection()
            loadCalendar()
        }
    }
    
    fun clearSelectedCompleted() {
        viewModelScope.launch {
            repository.updateMultipleDaysCompleted(_selectedDayIds.value.toList(), false)
            clearSelection()
            loadCalendar()
        }
    }
    
    fun showClearAllDialog() {
        _showClearAllDialog.value = true
    }
    
    fun dismissClearAllDialog() {
        _showClearAllDialog.value = false
    }
    
    fun clearAllCompleted() {
        viewModelScope.launch {
            repository.clearAllCompletedForCalendar(calendarId)
            _showClearAllDialog.value = false
            loadCalendar()
        }
    }
    
    fun toggleDayCompleted(dayId: String, currentState: Boolean) {
        viewModelScope.launch {
            repository.updateDayCompleted(dayId, !currentState)
            loadCalendar()
        }
    }
}
