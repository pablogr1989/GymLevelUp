package com.gymlog.app.ui.screens.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gymlog.app.data.local.entity.DayCategory
import com.gymlog.app.data.local.entity.DayOfWeek
import com.gymlog.app.domain.model.DetailedHistory
import com.gymlog.app.domain.repository.CalendarRepository
import com.gymlog.app.domain.repository.ExerciseRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class HistoryItemUi(
    val history: DetailedHistory,
    val exerciseName: String,
    val targetMinReps: Int,
    val targetMaxReps: Int,
    val targetMinRir: Int?,
    val targetMaxRir: Int?
)

data class DaySlotHierarchy(
    val daySlotId: String,
    val calendarName: String,
    val monthName: String,
    val monthNumber: Int,
    val weekNumber: Int,
    val dayOfWeek: DayOfWeek,
    val categories: List<DayCategory>
)

@HiltViewModel
class WorkoutHistoryViewModel @Inject constructor(
    private val exerciseRepository: ExerciseRepository,
    private val calendarRepository: CalendarRepository
) : ViewModel() {

    private val allExercises = exerciseRepository.getAllExercises().stateIn(viewModelScope, SharingStarted.Lazily, emptyList())
    private val allHistory = exerciseRepository.getAllDetailedHistory().stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    private val _daySlotHierarchies = MutableStateFlow<Map<String, DaySlotHierarchy>>(emptyMap())
    val daySlotHierarchies = _daySlotHierarchies.asStateFlow()

    // NUEVO: Agrupamos el historial primero por DaySlotId y luego por ExerciseId
    val groupedHistory = combine(allExercises, allHistory) { exercises, historyList ->
        val exerciseMap = exercises.associateBy { it.id }

        val uiItems = historyList.map { history ->
            val exercise = exerciseMap[history.exerciseId]
            val set = exercise?.sets?.find { it.id == history.setId }

            HistoryItemUi(
                history = history,
                exerciseName = exercise?.name ?: "Ejercicio Eliminado",
                targetMinReps = set?.minReps ?: 0,
                targetMaxReps = set?.maxReps ?: 0,
                targetMinRir = set?.minRir,
                targetMaxRir = set?.maxRir
            )
        }

        // Doble agrupación: { DaySlotId -> { ExerciseId -> List<HistoryItemUi> } }
        uiItems.groupBy { it.history.daySlotId }
            .mapValues { (_, items) -> items.groupBy { it.history.exerciseId } }
    }.stateIn(viewModelScope, SharingStarted.Lazily, emptyMap())

    private val _selectedIds = MutableStateFlow<Set<String>>(emptySet())
    val selectedIds = _selectedIds.asStateFlow()

    private val _editingItem = MutableStateFlow<HistoryItemUi?>(null)
    val editingItem = _editingItem.asStateFlow()

    // Control del primer nivel de desplegables (Días)
    private val _expandedGroups = MutableStateFlow<Set<String>>(emptySet())
    val expandedGroups = _expandedGroups.asStateFlow()

    // NUEVO: Control del segundo nivel de desplegables (Ejercicios)
    private val _expandedExercises = MutableStateFlow<Set<String>>(emptySet())
    val expandedExercises = _expandedExercises.asStateFlow()

    init {
        viewModelScope.launch {
            allHistory.collect { historyList ->
                val uniqueDaySlotIds = historyList.map { it.daySlotId }.distinct()
                val currentMap = _daySlotHierarchies.value.toMutableMap()
                var changed = false

                for (id in uniqueDaySlotIds) {
                    if (!currentMap.containsKey(id)) {
                        val day = calendarRepository.getDayById(id)
                        if (day != null) {
                            val week = calendarRepository.getWeekById(day.weekId)
                            val month = week?.let { calendarRepository.getMonthById(it.monthId) }
                            val calendar = month?.let { calendarRepository.getCalendarById(it.calendarId) }

                            currentMap[id] = DaySlotHierarchy(
                                daySlotId = id,
                                calendarName = calendar?.name ?: "Rutina",
                                monthName = month?.name ?: "",
                                monthNumber = month?.monthNumber ?: 0,
                                weekNumber = week?.weekNumber ?: 0,
                                dayOfWeek = day.dayOfWeek,
                                categories = day.categories
                            )
                            changed = true
                        }
                    }
                }
                if (changed) {
                    _daySlotHierarchies.value = currentMap
                }
            }
        }
    }

    fun toggleGroup(daySlotId: String) {
        _expandedGroups.update { current ->
            if (current.contains(daySlotId)) current - daySlotId else current + daySlotId
        }
    }

    // NUEVO: Función para abrir/cerrar un ejercicio específico dentro de un día
    fun toggleExerciseGroup(daySlotId: String, exerciseId: String) {
        val key = "${daySlotId}_${exerciseId}"
        _expandedExercises.update { current ->
            if (current.contains(key)) current - key else current + key
        }
    }

    fun toggleSelection(id: String) {
        _selectedIds.update { current ->
            if (current.contains(id)) current - id else current + id
        }
    }

    fun clearSelection() {
        _selectedIds.value = emptySet()
    }

    fun deleteSelected() {
        viewModelScope.launch {
            _selectedIds.value.forEach { id ->
                exerciseRepository.deleteDetailedHistoryById(id)
            }
            clearSelection()
        }
    }

    fun deleteSingleItem(id: String) {
        viewModelScope.launch {
            exerciseRepository.deleteDetailedHistoryById(id)
            _editingItem.value = null
        }
    }

    fun startEditing(item: HistoryItemUi) {
        _editingItem.value = item
    }

    fun cancelEditing() {
        _editingItem.value = null
    }

    fun saveEditedItem(weight: Float, reps: Int, rir: Int?, notes: String) {
        val currentItem = _editingItem.value ?: return
        viewModelScope.launch {
            val updatedHistory = currentItem.history.copy(
                weightKg = weight,
                reps = reps,
                rir = rir,
                notes = notes
            )
            exerciseRepository.updateDetailedHistory(updatedHistory)
            _editingItem.value = null
        }
    }
}