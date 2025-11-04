package com.gymlog.app.ui.screens.calendars

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gymlog.app.domain.model.Calendar
import com.gymlog.app.domain.repository.CalendarRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class CalendarsViewModel @Inject constructor(
    private val repository: CalendarRepository
) : ViewModel() {
    
    val calendars: StateFlow<List<Calendar>> = repository.getAllCalendars()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )
    
    private val _showDeleteDialog = MutableStateFlow<Calendar?>(null)
    val showDeleteDialog = _showDeleteDialog.asStateFlow()
    
    fun showDeleteDialog(calendar: Calendar) {
        _showDeleteDialog.value = calendar
    }
    
    fun dismissDeleteDialog() {
        _showDeleteDialog.value = null
    }
    
    fun deleteCalendar(calendar: Calendar) {
        viewModelScope.launch {
            repository.deleteCalendar(calendar)
            dismissDeleteDialog()
        }
    }
}
