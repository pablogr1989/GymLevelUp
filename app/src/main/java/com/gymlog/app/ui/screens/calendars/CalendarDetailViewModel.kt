package com.gymlog.app.ui.screens.calendars

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gymlog.app.domain.model.*
import com.gymlog.app.domain.repository.CalendarRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map // <--- IMPORTACIÓN FALTANTE AÑADIDA
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

// UiState Unificado para Calendario
data class CalendarUiState(
    val calendarWithMonths: CalendarWithMonths? = null,
    val currentMonthIndex: Int = 0,

    // Modo Selección (Checkboxes para marcar completado)
    val isSelectionMode: Boolean = false,
    val selectedDayIds: Set<String> = emptySet(),

    // Modo Intercambio (Swap)
    val swapSourceDayId: String? = null, // ID del día "origen" para mover

    val showClearAllDialog: Boolean = false
)

@HiltViewModel
class CalendarDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val repository: CalendarRepository
) : ViewModel() {

    private val calendarId: String = savedStateHandle.get<String>("calendarId") ?: ""

    private val _uiState = MutableStateFlow(CalendarUiState())
    val uiState = _uiState.asStateFlow()

    // --- Legacy StateFlows (Compatibilidad UI) ---
    val calendarWithMonths = repository.getCalendarWithMonthsFlow(calendarId)
        .stateIn(viewModelScope, SharingStarted.Lazily, null)

    val currentMonthIndex = _uiState.map { it.currentMonthIndex }
        .stateIn(viewModelScope, SharingStarted.Lazily, 0)

    val isSelectionMode = _uiState.map { it.isSelectionMode }
        .stateIn(viewModelScope, SharingStarted.Lazily, false)

    val selectedDayIds = _uiState.map { it.selectedDayIds }
        .stateIn(viewModelScope, SharingStarted.Lazily, emptySet())

    val showClearAllDialog = _uiState.map { it.showClearAllDialog }
        .stateIn(viewModelScope, SharingStarted.Lazily, false)

    // Nuevo StateFlow para UI: Día origen de movimiento
    val swapSourceDayId = _uiState.map { it.swapSourceDayId }
        .stateIn(viewModelScope, SharingStarted.Lazily, null)
    // ---------------------------------------------


    fun changeMonth(delta: Int) {
        val current = _uiState.value.currentMonthIndex
        val maxIndex = (calendarWithMonths.value?.months?.size ?: 1) - 1
        val newIndex = (current + delta).coerceIn(0, maxIndex)
        _uiState.update { it.copy(currentMonthIndex = newIndex) }
    }

    // Lógica principal de interacción con días
    fun onDayClick(dayId: String, onNavigate: (String) -> Unit) {
        val state = _uiState.value

        when {
            // Caso 1: Modo Selección (Marcar varios)
            state.isSelectionMode -> {
                toggleDaySelection(dayId)
            }
            // Caso 2: Modo Intercambio (Mover día)
            state.swapSourceDayId != null -> {
                // Realizar intercambio
                if (state.swapSourceDayId != dayId) {
                    viewModelScope.launch {
                        repository.swapDaySlots(state.swapSourceDayId, dayId)
                        // Salir del modo swap
                        _uiState.update { it.copy(swapSourceDayId = null) }
                    }
                } else {
                    // Si clicamos el mismo, cancelar
                    _uiState.update { it.copy(swapSourceDayId = null) }
                }
            }
            // Caso 3: Navegación normal
            else -> {
                onNavigate(dayId)
            }
        }
    }

    fun onDayLongPress(dayId: String) {
        val state = _uiState.value

        // Si ya estamos en modo selección, el long press añade a la selección
        if (state.isSelectionMode) {
            toggleDaySelection(dayId)
            return
        }

        // Si no, activamos modo SWAP (Mover día)
        // Si ya había uno seleccionado para mover, lo cambiamos
        if (state.swapSourceDayId == dayId) {
            _uiState.update { it.copy(swapSourceDayId = null) } // Cancelar
        } else {
            _uiState.update { it.copy(swapSourceDayId = dayId) } // Activar origen
        }
    }

    fun toggleDaySelection(dayId: String) {
        _uiState.update { state ->
            val current = state.selectedDayIds
            val newSet = if (current.contains(dayId)) current - dayId else current + dayId
            state.copy(
                selectedDayIds = newSet,
                isSelectionMode = newSet.isNotEmpty()
            )
        }
    }

    fun clearSelection() {
        _uiState.update { it.copy(selectedDayIds = emptySet(), isSelectionMode = false) }
    }

    fun cancelSwap() {
        _uiState.update { it.copy(swapSourceDayId = null) }
    }

    fun markSelectedAsCompleted() {
        viewModelScope.launch {
            repository.updateMultipleDaysCompleted(_uiState.value.selectedDayIds.toList(), true)
            clearSelection()
        }
    }

    fun clearSelectedCompleted() {
        viewModelScope.launch {
            repository.updateMultipleDaysCompleted(_uiState.value.selectedDayIds.toList(), false)
            clearSelection()
        }
    }

    fun showClearAllDialog() {
        _uiState.update { it.copy(showClearAllDialog = true) }
    }

    fun dismissClearAllDialog() {
        _uiState.update { it.copy(showClearAllDialog = false) }
    }

    fun clearAllCompleted() {
        viewModelScope.launch {
            repository.clearAllCompletedForCalendar(calendarId)
            _uiState.update { it.copy(showClearAllDialog = false) }
        }
    }
}