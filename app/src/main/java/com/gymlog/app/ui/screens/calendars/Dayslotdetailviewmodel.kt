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

data class ExerciseWithSelectedSets(
    val exercise: Exercise,
    val selectedSets: List<GymSet>,
    val assignment: TrainingAssignment
)

data class DaySlotUiState(
    val daySlot: DaySlot? = null,
    val selectedCategories: Set<DayCategory> = emptySet(),
    val currentAssignments: List<TrainingAssignment> = emptyList(),
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

    private val daySlotId: String = checkNotNull(savedStateHandle["daySlotId"])

    private val _uiState = MutableStateFlow(DaySlotUiState())

    val daySlot = _uiState.map { it.daySlot }.stateIn(viewModelScope, SharingStarted.Lazily, null)
    val selectedCategories = _uiState.map { it.selectedCategories }.stateIn(viewModelScope, SharingStarted.Lazily, emptySet())
    val completed = _uiState.map { it.completed }.stateIn(viewModelScope, SharingStarted.Lazily, false)
    val isLoading = _uiState.map { it.isLoading }.stateIn(viewModelScope, SharingStarted.Lazily, false)
    val isCategoriesExpanded = _uiState.map { it.isCategoriesExpanded }.stateIn(viewModelScope, SharingStarted.Lazily, true)
    val isExercisesExpanded = _uiState.map { it.isExercisesExpanded }.stateIn(viewModelScope, SharingStarted.Lazily, true)

    private val _filteredExercises = MutableStateFlow<List<Exercise>>(emptyList())
    val filteredExercises = _filteredExercises.asStateFlow()

    val selectedExercisesWithSets = _uiState.map { state ->
        val currentDaySlot = state.daySlot ?: return@map emptyList()
        val allExercises = exerciseRepository.getAllExercises().first()
        val exerciseMap = allExercises.associateBy { it.id }

        state.currentAssignments.mapNotNull { assignment ->
            val exercise = exerciseMap[assignment.exerciseId]
            if (exercise != null) {
                // Leemos los IDs de las variantes
                val setIds = assignment.targetSetId?.split(",")?.filter { it.isNotBlank() } ?: emptyList()
                var selectedSets = setIds.mapNotNull { id -> exercise.sets.find { it.id == id } }

                // LÓGICA DE RESCATE (FALLBACK):
                // Si un ejercicio antiguo no tiene variantes asignadas (targetSetId == null),
                // pero el ejercicio tiene variantes creadas, cogemos automáticamente la primera.
                if (selectedSets.isEmpty() && exercise.sets.isNotEmpty()) {
                    selectedSets = listOf(exercise.sets.first())
                }

                ExerciseWithSelectedSets(exercise, selectedSets, assignment)
            } else null
        }
    }.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    private val _navigateBack = MutableStateFlow(false)
    val navigateBack = _navigateBack.asStateFlow()

    private val _navigateToTraining = MutableStateFlow(false)
    val navigateToTraining = _navigateToTraining.asStateFlow()

    init {
        loadDaySlot()
    }

    private fun loadDaySlot() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            calendarRepository.getDayById(daySlotId)?.let { currentDaySlot ->
                _uiState.update { state ->
                    state.copy(
                        daySlot = currentDaySlot,
                        selectedCategories = currentDaySlot.categories.toSet(),
                        currentAssignments = currentDaySlot.exercises,
                        completed = currentDaySlot.completed,
                        isLoading = false
                    )
                }
                loadFilteredExercises()
            }
        }
    }

    private fun loadFilteredExercises() {
        viewModelScope.launch {
            val categories = _uiState.value.selectedCategories
            val muscleGroups = categories.mapNotNull { categoryToMuscleGroup(it) }

            exerciseRepository.getAllExercises().collect { allExercises ->
                val filtered = if (muscleGroups.isEmpty()) {
                    allExercises
                } else {
                    allExercises.filter { muscleGroups.contains(it.muscleGroup) }
                }
                _filteredExercises.value = filtered
            }
        }
    }

    fun toggleCategory(category: DayCategory) {
        _uiState.update { state ->
            val newCategories = if (state.selectedCategories.contains(category)) {
                state.selectedCategories - category
            } else {
                state.selectedCategories + category
            }
            state.copy(selectedCategories = newCategories)
        }
        loadFilteredExercises()
    }

    fun toggleCompleted() {
        _uiState.update { it.copy(completed = !it.completed) }
    }

    fun addExercises(exerciseId: String, setIds: List<String>) {
        _uiState.update { state ->
            val targetSetIdsStr = if (setIds.isEmpty()) null else setIds.joinToString(",")
            val newAssignment = TrainingAssignment(exerciseId, targetSetId = targetSetIdsStr)
            state.copy(currentAssignments = state.currentAssignments + newAssignment)
        }
    }

    fun removeExercise(assignmentToRemove: TrainingAssignment) {
        _uiState.update { state ->
            state.copy(currentAssignments = state.currentAssignments.filter { it !== assignmentToRemove })
        }
    }

    fun moveExercise(fromIndex: Int, toIndex: Int) {
        _uiState.update { state ->
            val newList = state.currentAssignments.toMutableList()
            val item = newList.removeAt(fromIndex)
            newList.add(toIndex, item)
            state.copy(currentAssignments = newList)
        }
    }

    fun saveDaySlot() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val state = _uiState.value
            val currentDaySlot = state.daySlot ?: return@launch

            val updatedDaySlot = currentDaySlot.copy(
                categories = state.selectedCategories.toList(),
                exercises = state.currentAssignments,
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
            DayCategory.CARDIO, DayCategory.REST -> null
        }
    }
}