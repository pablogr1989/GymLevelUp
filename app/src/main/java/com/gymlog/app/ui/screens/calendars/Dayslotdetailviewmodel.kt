package com.gymlog.app.ui.screens.calendars

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gymlog.app.data.local.entity.DayCategory
import com.gymlog.app.data.local.entity.MuscleGroup
import com.gymlog.app.domain.model.DaySlot
import com.gymlog.app.domain.model.Exercise
import com.gymlog.app.domain.model.TrainingAssignment
import com.gymlog.app.domain.repository.CalendarRepository
import com.gymlog.app.domain.repository.ExerciseRepository
import com.gymlog.app.domain.model.Set as GymSet
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

// Modelo UI auxiliar
data class ExerciseWithSelectedSet(
    val exercise: Exercise,
    val selectedSet: GymSet?,
    val assignment: TrainingAssignment // Objeto puro para referencias
)

data class DaySlotUiState(
    val daySlot: DaySlot? = null,
    val selectedCategories: Set<DayCategory> = emptySet(),
    val currentAssignments: List<TrainingAssignment> = emptyList(), // LISTA TIPADA
    val completed: Boolean = false,
    val isLoading: Boolean = false,
    val isCategoriesExpanded: Boolean = true,
    val isExercisesExpanded: Boolean = true
)

@HiltViewModel
class DaySlotDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val calendarRepository: CalendarRepository,
    private val exerciseRepository: ExerciseRepository
) : ViewModel() {

    private val daySlotId: String = savedStateHandle.get<String>("daySlotId") ?: ""

    private val _uiState = MutableStateFlow(DaySlotUiState())
    val uiState = _uiState.asStateFlow()

    // --- StateFlows (Compatibilidad UI) ---
    val daySlot = _uiState.map { it.daySlot }.stateIn(viewModelScope, SharingStarted.Lazily, null)
    val selectedCategories = _uiState.map { it.selectedCategories }.stateIn(viewModelScope, SharingStarted.Lazily, emptySet())
    val completed = _uiState.map { it.completed }.stateIn(viewModelScope, SharingStarted.Lazily, false)
    val isLoading = _uiState.map { it.isLoading }.stateIn(viewModelScope, SharingStarted.Lazily, false)
    val isCategoriesExpanded = _uiState.map { it.isCategoriesExpanded }.stateIn(viewModelScope, SharingStarted.Lazily, true)
    val isExercisesExpanded = _uiState.map { it.isExercisesExpanded }.stateIn(viewModelScope, SharingStarted.Lazily, true)
    // --------------------------------------

    private val _navigateBack = MutableStateFlow(false)
    val navigateBack = _navigateBack.asStateFlow()

    private val _navigateToTraining = MutableStateFlow(false)
    val navigateToTraining = _navigateToTraining.asStateFlow()

    private val allExercises: StateFlow<List<Exercise>> = exerciseRepository
        .getAllExercises()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val selectedExercisesWithSets: StateFlow<List<ExerciseWithSelectedSet>> = combine(
        allExercises,
        _uiState.map { it.currentAssignments }
    ) { exercises, assignments ->
        val exerciseMap = exercises.associateBy { it.id }

        assignments.mapNotNull { assignment ->
            val exercise = exerciseMap[assignment.exerciseId]
            if (exercise != null) {
                val set = if (assignment.targetSetId != null) {
                    exercise.sets.find { it.id == assignment.targetSetId } ?: exercise.sets.firstOrNull()
                } else {
                    exercise.sets.firstOrNull()
                }
                ExerciseWithSelectedSet(exercise, set, assignment)
            } else {
                null
            }
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    // Lista filtrada de ejercicios para el selector (bottom sheet)
    val filteredExercises: StateFlow<List<Exercise>> = combine(
        allExercises,
        _uiState.map { it.selectedCategories }
    ) { exercises, categories ->
        if (categories.isEmpty()) {
            exercises
        } else {
            if (categories.contains(DayCategory.FULL_BODY)) {
                exercises
            } else if (categories.all { it == DayCategory.CARDIO || it == DayCategory.REST }) {
                emptyList()
            } else {
                exercises.filter { exercise ->
                    val muscleGroups = categories.mapNotNull { category ->
                        categoryToMuscleGroup(category)
                    }
                    exercise.muscleGroup in muscleGroups
                }
            }
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    init {
        loadDaySlot()
    }

    private fun loadDaySlot() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            calendarRepository.getDayById(daySlotId)?.let { daySlot ->
                _uiState.update { it.copy(
                    daySlot = daySlot,
                    selectedCategories = daySlot.categories.toSet(),
                    currentAssignments = daySlot.exercises, // Carga directa, sin splits
                    completed = daySlot.completed
                )}
            }
            _uiState.update { it.copy(isLoading = false) }
        }
    }

    fun toggleCategory(category: DayCategory) {
        _uiState.update { state ->
            val current = state.selectedCategories
            val newCategories = if (current.contains(category)) current - category else current + category

            // Limpieza inteligente de ejercicios incompatibles
            var newAssignments = state.currentAssignments

            if (newAssignments.isNotEmpty()) {
                if (newCategories.all { it == DayCategory.CARDIO || it == DayCategory.REST }) {
                    newAssignments = emptyList()
                } else {
                    val compatibleExercises = if (newCategories.contains(DayCategory.FULL_BODY)) {
                        allExercises.value
                    } else {
                        allExercises.value.filter { exercise ->
                            val muscleGroups = newCategories.mapNotNull { cat -> categoryToMuscleGroup(cat) }
                            exercise.muscleGroup in muscleGroups
                        }
                    }
                    newAssignments = newAssignments.filter { assignment ->
                        compatibleExercises.any { it.id == assignment.exerciseId }
                    }
                }
            }

            state.copy(
                selectedCategories = newCategories,
                currentAssignments = newAssignments
            )
        }
    }

    fun addExercise(exerciseId: String, setId: String) {
        val newAssignment = TrainingAssignment(exerciseId, setId)
        _uiState.update { state ->
            // Evitar duplicados exactos si se desea, o permitir (aquí permitimos pero verificamos si ya existe exacto)
            val current = state.currentAssignments
            if (!current.contains(newAssignment)) {
                state.copy(currentAssignments = current + newAssignment)
            } else state
        }
    }

    fun removeExercise(assignment: TrainingAssignment) {
        _uiState.update { state ->
            state.copy(currentAssignments = state.currentAssignments - assignment)
        }
    }

    fun moveExercise(fromIndex: Int, toIndex: Int) {
        val currentList = _uiState.value.currentAssignments.toMutableList()
        if (fromIndex in currentList.indices && toIndex in currentList.indices) {
            val item = currentList.removeAt(fromIndex)
            currentList.add(toIndex, item)
            _uiState.update { it.copy(currentAssignments = currentList) }
        }
    }

    fun toggleCompleted() {
        _uiState.update { it.copy(completed = !it.completed) }
    }

    fun saveDaySlot() {
        viewModelScope.launch {
            val state = _uiState.value
            val currentDaySlot = state.daySlot ?: return@launch

            _uiState.update { it.copy(isLoading = true) }

            val updatedDaySlot = currentDaySlot.copy(
                categories = state.selectedCategories.toList(),
                exercises = state.currentAssignments, // Guardado directo
                completed = state.completed
            )

            calendarRepository.updateDaySlot(updatedDaySlot)

            _uiState.update { it.copy(isLoading = false) }
            _navigateBack.value = true
        }
    }

    fun resetNavigation() {
        _navigateBack.value = false
        _navigateToTraining.value = false
    }

    fun toggleCategoriesExpansion() {
        _uiState.update { it.copy(isCategoriesExpanded = !it.isCategoriesExpanded) }
    }

    fun toggleExercisesExpansion() {
        _uiState.update { it.copy(isExercisesExpanded = !it.isExercisesExpanded) }
    }

    fun startTraining() {
        _navigateToTraining.value = true
    }

    private fun categoryToMuscleGroup(category: DayCategory): MuscleGroup? {
        return when (category) {
            DayCategory.CHEST -> MuscleGroup.CHEST
            DayCategory.LEGS -> MuscleGroup.LEGS
            DayCategory.GLUTES -> MuscleGroup.GLUTES
            DayCategory.BACK -> MuscleGroup.BACK
            DayCategory.BICEPS -> MuscleGroup.BICEPS
            DayCategory.TRICEPS -> MuscleGroup.TRICEPS
            DayCategory.SHOULDERS -> MuscleGroup.SHOULDERS
            DayCategory.FULL_BODY -> null
            DayCategory.CARDIO -> null
            DayCategory.REST -> null
        }
    }
}