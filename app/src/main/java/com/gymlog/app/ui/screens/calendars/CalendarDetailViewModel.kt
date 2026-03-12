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
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class ToolAction(val maxSource: Int, val isMove: Boolean) {
    NONE(0, false),
    MOVE_DAY(1, true),
    COPY_DAYS(Int.MAX_VALUE, false),
    MOVE_WEEK(1, true),
    COPY_WEEKS(Int.MAX_VALUE, false),
    MOVE_MONTH(1, true),
    COPY_MONTH(1, false)
}

enum class ActionPhase { SELECT_SOURCE, SELECT_TARGET }

data class CalendarUiState(
    val calendarWithMonths: CalendarWithMonths? = null,
    val currentMonthIndex: Int = 0,

    // Modo Selección Clásico (Completados)
    val isSelectionMode: Boolean = false,
    val selectedDayIds: Set<String> = emptySet(),
    val showClearAllDialog: Boolean = false,

    // Nuevas Herramientas Avanzadas
    val toolAction: ToolAction = ToolAction.NONE,
    val actionPhase: ActionPhase = ActionPhase.SELECT_SOURCE,
    val sourceSelections: List<String> = emptyList(),
    val targetSelections: List<String> = emptyList()
)

@HiltViewModel
class CalendarDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val repository: CalendarRepository
) : ViewModel() {

    private val calendarId: String = savedStateHandle.get<String>("calendarId") ?: ""

    private val _uiState = MutableStateFlow(CalendarUiState())
    val uiState = _uiState.asStateFlow()

    val calendarWithMonths = repository.getCalendarWithMonthsFlow(calendarId)
        .stateIn(viewModelScope, SharingStarted.Lazily, null)

    init {
        viewModelScope.launch {
            calendarWithMonths.collect { data ->
                _uiState.update { it.copy(calendarWithMonths = data) }
            }
        }
    }

    fun changeMonth(delta: Int) {
        val current = _uiState.value.currentMonthIndex
        val maxIndex = (calendarWithMonths.value?.months?.size ?: 1) - 1
        val newIndex = (current + delta).coerceIn(0, maxIndex)
        _uiState.update { it.copy(currentMonthIndex = newIndex) }
    }

    // --- Lógica Principal de Clicks en la UI ---

    fun onDayClick(dayId: String, weekId: String, monthId: String, onNavigate: (String) -> Unit) {
        val state = _uiState.value

        when {
            state.toolAction != ToolAction.NONE -> {
                val selectionId = when (state.toolAction) {
                    ToolAction.MOVE_DAY, ToolAction.COPY_DAYS -> dayId
                    ToolAction.MOVE_WEEK, ToolAction.COPY_WEEKS -> weekId
                    ToolAction.MOVE_MONTH, ToolAction.COPY_MONTH -> monthId
                    else -> dayId
                }
                handleToolSelection(selectionId)
            }
            state.isSelectionMode -> {
                toggleDaySelection(dayId)
            }
            else -> {
                onNavigate(dayId)
            }
        }
    }

    fun onDayLongPress(dayId: String) {
        val state = _uiState.value
        if (state.toolAction == ToolAction.NONE) {
            toggleDaySelection(dayId) // Entra en modo selección para marcar completados
        }
    }

    // --- Herramientas Avanzadas ---

    fun startTool(action: ToolAction) {
        _uiState.update { it.copy(
            toolAction = action,
            actionPhase = ActionPhase.SELECT_SOURCE,
            sourceSelections = emptyList(),
            targetSelections = emptyList(),
            isSelectionMode = false,
            selectedDayIds = emptySet()
        )}
    }

    fun cancelTool() {
        _uiState.update { it.copy(
            toolAction = ToolAction.NONE,
            sourceSelections = emptyList(),
            targetSelections = emptyList()
        )}
    }

    private fun handleToolSelection(id: String) {
        val state = _uiState.value
        val action = state.toolAction

        if (state.actionPhase == ActionPhase.SELECT_SOURCE) {
            val current = state.sourceSelections.toMutableList()
            if (current.contains(id)) current.remove(id)
            else if (current.size < action.maxSource) current.add(id)

            if (current.size == action.maxSource) {
                // Si es un límite de 1 (como Mover/Copiar Mes o Mover Semana), avanzamos automáticamente
                _uiState.update { it.copy(sourceSelections = current, actionPhase = ActionPhase.SELECT_TARGET) }
            } else {
                _uiState.update { it.copy(sourceSelections = current) }
            }
        } else {
            val current = state.targetSelections.toMutableList()
            if (current.contains(id)) current.remove(id)
            else if (current.size < state.sourceSelections.size) current.add(id)

            if (current.size == state.sourceSelections.size && action.isMove) {
                // Si es mover y completamos destino, ejecutamos directamente (swap automático)
                _uiState.update { it.copy(targetSelections = current) }
                executeTool()
            } else {
                _uiState.update { it.copy(targetSelections = current) }
            }
        }
    }

    fun confirmSourceSelection() {
        if (_uiState.value.sourceSelections.isNotEmpty()) {
            _uiState.update { it.copy(actionPhase = ActionPhase.SELECT_TARGET) }
        }
    }

    fun executeTool() {
        val state = _uiState.value
        val sources = sortSelections(state.sourceSelections, state.toolAction)
        val targets = sortSelections(state.targetSelections, state.toolAction)

        if (sources.size != targets.size || sources.isEmpty()) return

        viewModelScope.launch {
            when (state.toolAction) {
                ToolAction.MOVE_DAY -> repository.swapDaySlots(sources[0], targets[0])
                ToolAction.COPY_DAYS -> repository.copyDaySlots(sources, targets)
                ToolAction.MOVE_WEEK -> repository.swapWeeks(sources[0], targets[0])
                ToolAction.COPY_WEEKS -> repository.copyWeeks(sources, targets)
                ToolAction.MOVE_MONTH -> repository.swapMonths(sources[0], targets[0])
                ToolAction.COPY_MONTH -> repository.copyMonths(sources[0], targets[0])
                ToolAction.NONE -> {}
            }
            cancelTool()
        }
    }

    // Ordena cronológicamente los IDs según su aparición en el calendario
    private fun sortSelections(selections: List<String>, action: ToolAction): List<String> {
        val calendarData = calendarWithMonths.value ?: return selections
        return selections.sortedBy { id ->
            when (action) {
                ToolAction.MOVE_DAY, ToolAction.COPY_DAYS -> {
                    calendarData.months.flatMap { it.weeks }.flatMap { it.days }.indexOfFirst { it.id == id }
                }
                ToolAction.MOVE_WEEK, ToolAction.COPY_WEEKS -> {
                    calendarData.months.flatMap { it.weeks }.map { it.week }.indexOfFirst { it.id == id }
                }
                ToolAction.MOVE_MONTH, ToolAction.COPY_MONTH -> {
                    calendarData.months.map { it.month }.indexOfFirst { it.id == id }
                }
                ToolAction.NONE -> 0
            }
        }
    }

    // --- Herramientas de Completado Clásicas ---

    fun toggleDaySelection(dayId: String) {
        _uiState.update { state ->
            val current = state.selectedDayIds
            val newSet = if (current.contains(dayId)) current - dayId else current + dayId
            state.copy(selectedDayIds = newSet, isSelectionMode = newSet.isNotEmpty())
        }
    }

    fun clearSelection() { _uiState.update { it.copy(selectedDayIds = emptySet(), isSelectionMode = false) } }
    fun showClearAllDialog() { _uiState.update { it.copy(showClearAllDialog = true) } }
    fun dismissClearAllDialog() { _uiState.update { it.copy(showClearAllDialog = false) } }

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

    fun clearAllCompleted() {
        viewModelScope.launch {
            repository.clearAllCompletedForCalendar(calendarId)
            _uiState.update { it.copy(showClearAllDialog = false) }
        }
    }
}