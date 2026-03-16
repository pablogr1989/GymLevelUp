package com.gymlog.app.ui.screens.history

import android.app.Application
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gymlog.app.domain.repository.CalendarRepository
import com.gymlog.app.domain.repository.ExerciseRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import javax.inject.Inject

data class ExportGroupUi(
    val daySlotId: String,
    val hierarchy: DaySlotHierarchy?,
    val itemCount: Int
)

sealed class ExportState {
    object Idle : ExportState()
    object Loading : ExportState()
    data class Success(val message: String) : ExportState()
    data class Error(val message: String) : ExportState()
}

@HiltViewModel
class ExportHistoryViewModel @Inject constructor(
    private val exerciseRepository: ExerciseRepository,
    private val calendarRepository: CalendarRepository,
    private val application: Application
) : ViewModel() {

    private val allHistory = exerciseRepository.getAllDetailedHistory().stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    private val _daySlotHierarchies = MutableStateFlow<Map<String, DaySlotHierarchy>>(emptyMap())

    private val _isAscending = MutableStateFlow(true)
    val isAscending = _isAscending.asStateFlow()

    private val _selectedGroupIds = MutableStateFlow<Set<String>>(emptySet())
    val selectedGroupIds = _selectedGroupIds.asStateFlow()

    private val _exportState = MutableStateFlow<ExportState>(ExportState.Idle)
    val exportState = _exportState.asStateFlow()

    init {
        viewModelScope.launch {
            allHistory.collect { historyList ->
                val uniqueDaySlotIds = historyList.map { it.daySlotId }.distinct()
                val currentMap = _daySlotHierarchies.value.toMutableMap()

                uniqueDaySlotIds.forEach { id ->
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
                        }
                    }
                }
                _daySlotHierarchies.value = currentMap
            }
        }
    }

    val exportGroups = combine(allHistory, _daySlotHierarchies, _isAscending) { historyList, hierarchies, isAsc ->
        val grouped = historyList.groupBy { it.daySlotId }
        val groupsUi = grouped.map { (daySlotId, items) ->
            ExportGroupUi(daySlotId, hierarchies[daySlotId], items.size)
        }

        if (isAsc) {
            groupsUi.sortedWith(compareBy(
                { it.hierarchy?.monthNumber ?: Int.MAX_VALUE },
                { it.hierarchy?.weekNumber ?: Int.MAX_VALUE },
                { it.hierarchy?.dayOfWeek?.ordinal ?: Int.MAX_VALUE }
            ))
        } else {
            groupsUi.sortedWith(compareByDescending<ExportGroupUi> { it.hierarchy?.monthNumber ?: -1 }
                .thenByDescending { it.hierarchy?.weekNumber ?: -1 }
                .thenByDescending { it.hierarchy?.dayOfWeek?.ordinal ?: -1 })
        }
    }.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    fun toggleSelection(daySlotId: String) {
        _selectedGroupIds.update { current ->
            if (current.contains(daySlotId)) current - daySlotId else current + daySlotId
        }
    }

    fun selectAll() {
        _selectedGroupIds.value = exportGroups.value.map { it.daySlotId }.toSet()
    }

    fun deselectAll() {
        _selectedGroupIds.value = emptySet()
    }

    fun toggleSortOrder() {
        _isAscending.update { !it }
    }

    fun dismissMessage() {
        _exportState.value = ExportState.Idle
    }

    fun exportData(uri: Uri) {
        if (_selectedGroupIds.value.isEmpty()) {
            _exportState.value = ExportState.Error("No hay ningún día seleccionado para exportar.")
            return
        }

        viewModelScope.launch {
            _exportState.value = ExportState.Loading
            try {
                val exercisesList = exerciseRepository.getAllExercises().first()
                val exercises = exercisesList.associateBy { it.id }

                val historiesToExport = allHistory.value.filter { _selectedGroupIds.value.contains(it.daySlotId) }
                val hierarchies = _daySlotHierarchies.value

                val jsonArray = JSONArray()
                val sortedHistories = historiesToExport.sortedBy { it.timestamp }

                sortedHistories.forEach { history ->
                    val exercise = exercises[history.exerciseId]
                    val set = exercise?.sets?.find { it.id == history.setId }
                    val hierarchy = hierarchies[history.daySlotId]

                    val targetReps = set?.let { if (it.minReps == it.maxReps) "${it.minReps}" else "${it.minReps}-${it.maxReps}" } ?: "N/A"
                    val targetRir = set?.let {
                        if (it.minRir != null && it.maxRir != null) {
                            if (it.minRir == it.maxRir) "${it.minRir}" else "${it.minRir}-${it.maxRir}"
                        } else "N/A"
                    } ?: "N/A"

                    val jsonObject = JSONObject().apply {
                        put("timestamp", history.timestamp)
                        put("mes", hierarchy?.let { if (it.monthName.isNotBlank()) it.monthName else "Mes ${it.monthNumber}" } ?: "Sin Mes")
                        put("semana", hierarchy?.weekNumber ?: 0)
                        put("dia", hierarchy?.dayOfWeek?.name ?: "Desconocido")
                        put("ejercicio", exercise?.name ?: "Ejercicio Eliminado")
                        put("numero_serie", history.seriesNumber)
                        put("reps_objetivas", targetReps)
                        put("rir_objetivo", targetRir)
                        put("reps_conseguidas", history.reps)

                        // --- AQUÍ AÑADIMOS EL NUEVO CAMPO RIR CONSEGUIDO ---
                        put("rir_conseguido", history.rir ?: "")

                        put("peso_logrado", history.weightKg)
                        put("observaciones", history.notes)
                    }
                    jsonArray.put(jsonObject)
                }

                val jsonString = jsonArray.toString(4)

                application.contentResolver.openOutputStream(uri)?.use { outputStream ->
                    outputStream.write(jsonString.toByteArray())
                }

                _exportState.value = ExportState.Success("Historial exportado correctamente.")
            } catch (e: Exception) {
                _exportState.value = ExportState.Error(e.localizedMessage ?: "Error al generar el JSON.")
            }
        }
    }
}