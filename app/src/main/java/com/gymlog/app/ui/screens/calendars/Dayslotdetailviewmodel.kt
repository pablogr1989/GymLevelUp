package com.gymlog.app.ui.screens.calendars

import android.util.Log
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gymlog.app.data.local.entity.DayCategory
import com.gymlog.app.data.local.entity.MuscleGroup
import com.gymlog.app.domain.model.DaySlot
import com.gymlog.app.domain.model.Exercise
import com.gymlog.app.domain.repository.CalendarRepository
import com.gymlog.app.domain.repository.ExerciseRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class DaySlotDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val calendarRepository: CalendarRepository,
    private val exerciseRepository: ExerciseRepository
) : ViewModel() {

    private val daySlotId: String = savedStateHandle.get<String>("daySlotId") ?: ""

    private val _daySlot = MutableStateFlow<DaySlot?>(null)
    val daySlot = _daySlot.asStateFlow()

    private val _selectedCategories = MutableStateFlow<Set<DayCategory>>(emptySet())
    val selectedCategories = _selectedCategories.asStateFlow()

    private val _selectedExerciseIds = MutableStateFlow<List<String>>(emptyList())
    val selectedExerciseIds = _selectedExerciseIds.asStateFlow()

    // Obtener todos los ejercicios
    private val allExercises: StateFlow<List<Exercise>> = exerciseRepository
        .getAllExercises()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // Obtener ejercicios seleccionados con detalles
    val selectedExercises: StateFlow<List<Exercise>> = combine(
        allExercises,
        selectedExerciseIds
    ) { exercises, ids ->
        // Crear un mapa para búsqueda rápida
        val exerciseMap = exercises.associateBy { it.id }

        // Mantener el orden de selectedExerciseIds
        ids.mapNotNull { id -> exerciseMap[id] }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    private val _completed = MutableStateFlow(false)
    val completed = _completed.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading = _isLoading.asStateFlow()

    private val _navigateBack = MutableStateFlow(false)
    val navigateBack = _navigateBack.asStateFlow()

    private val _navigateToTraining = MutableStateFlow(false)
    val navigateToTraining = _navigateToTraining.asStateFlow()

    // Estado de expansión de secciones (se mantiene durante la sesión)
    private val _isCategoriesExpanded = MutableStateFlow(true)
    val isCategoriesExpanded = _isCategoriesExpanded.asStateFlow()

    private val _isExercisesExpanded = MutableStateFlow(true)
    val isExercisesExpanded = _isExercisesExpanded.asStateFlow()

    // Filtrar ejercicios por categorÃ­as seleccionadas
    val filteredExercises: StateFlow<List<Exercise>> = combine(
        allExercises,
        selectedCategories
    ) { exercises, categories ->
        if (categories.isEmpty()) {
            exercises
        } else {
            // Si tiene FULL_BODY, mostrar todos los ejercicios
            if (categories.contains(DayCategory.FULL_BODY)) {
                exercises
            }
            // Si solo tiene CARDIO o REST, no mostrar ejercicios
            else if (categories.all { it == DayCategory.CARDIO || it == DayCategory.REST }) {
                emptyList()
            }
            // Filtrar por categorÃ­as normales
            else {
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
            _isLoading.value = true
            calendarRepository.getDayById(daySlotId)?.let { daySlot ->
                _daySlot.value = daySlot
                _selectedCategories.value = daySlot.categories.toSet()
                _selectedExerciseIds.value = daySlot.selectedExerciseIds
                _completed.value = daySlot.completed
            }
            _isLoading.value = false
        }
    }

    fun toggleCategory(category: DayCategory) {
        val current = _selectedCategories.value
        _selectedCategories.value = if (current.contains(category)) {
            current - category
        } else {
            current + category
        }

        // Limpiar ejercicios seleccionados si ya no son compatibles
        val currentExercises = _selectedExerciseIds.value
        if (currentExercises.isNotEmpty()) {
            val newCategories = _selectedCategories.value
            // Si solo hay CARDIO o REST, limpiar todos los ejercicios
            if (newCategories.all { it == DayCategory.CARDIO || it == DayCategory.REST }) {
                _selectedExerciseIds.value = emptyList()
            } else {
                // Si hay otras categorÃ­as, verificar compatibilidad de ejercicios
                val compatibleExercises = if (newCategories.contains(DayCategory.FULL_BODY)) {
                    allExercises.value
                } else {
                    allExercises.value.filter { exercise ->
                        val muscleGroups = newCategories.mapNotNull { cat ->
                            categoryToMuscleGroup(cat)
                        }
                        exercise.muscleGroup in muscleGroups
                    }
                }
                // Mantener solo ejercicios compatibles
                _selectedExerciseIds.value = currentExercises.filter { exerciseId ->
                    compatibleExercises.any { it.id == exerciseId }
                }
            }
        }
    }

    fun addExercise(exerciseId: String) {
        val current = _selectedExerciseIds.value
        if (!current.contains(exerciseId)) {
            _selectedExerciseIds.value = current + exerciseId
        }
    }

    fun removeExercise(exerciseId: String) {
        _selectedExerciseIds.value = _selectedExerciseIds.value - exerciseId
    }

    fun selectExercise(exerciseId: String?) {
        // Mantener por compatibilidad, ahora usa addExercise
        if (exerciseId != null) {
            addExercise(exerciseId)
        }
    }

    fun toggleCompleted() {
        _completed.value = !_completed.value
    }

    fun saveDaySlot() {
        viewModelScope.launch {
            val currentDaySlot = _daySlot.value ?: return@launch

            _isLoading.value = true

            val updatedDaySlot = currentDaySlot.copy(
                categories = _selectedCategories.value.toList(),
                selectedExerciseIds = _selectedExerciseIds.value,
                completed = _completed.value
            )

            calendarRepository.updateDaySlot(updatedDaySlot)

            _isLoading.value = false
            _navigateBack.value = true
        }
    }

    fun resetNavigation() {
        _navigateBack.value = false
        _navigateToTraining.value = false
    }

    // Funciones para manejar expansión de secciones
    fun toggleCategoriesExpansion() {
        _isCategoriesExpanded.value = !_isCategoriesExpanded.value
    }

    fun toggleExercisesExpansion() {
        _isExercisesExpanded.value = !_isExercisesExpanded.value
    }

    // Función para reordenar ejercicios
    fun moveExercise(fromIndex: Int, toIndex: Int) {
        Log.d("Drag", "MoveExercise Start: From: $fromIndex To: $toIndex")
        val currentList = _selectedExerciseIds.value.toMutableList()
        currentList.forEachIndexed { item, index ->
            Log.d("Drag", "MoveExercise antes: Item: $item Index: $index")
        }
        if (fromIndex in currentList.indices && toIndex in currentList.indices) {
            val item = currentList.removeAt(fromIndex)
            currentList.add(toIndex, item)
            _selectedExerciseIds.value = currentList
        }
        currentList.forEachIndexed { item, index ->
            Log.d("Drag", "MoveExercise despues: Item: $item Index: $index")
        }
    }

    // Función para navegar al modo entrenamiento
    fun startTraining() {
        _navigateToTraining.value = true
    }

    // Mapeo de DayCategory a MuscleGroup
    private fun categoryToMuscleGroup(category: DayCategory): MuscleGroup? {
        return when (category) {
            DayCategory.CHEST -> MuscleGroup.CHEST
            DayCategory.LEGS -> MuscleGroup.LEGS
            DayCategory.GLUTES -> MuscleGroup.GLUTES
            DayCategory.BACK -> MuscleGroup.BACK
            DayCategory.BICEPS -> MuscleGroup.BICEPS
            DayCategory.TRICEPS -> MuscleGroup.TRICEPS
            DayCategory.SHOULDERS -> MuscleGroup.SHOULDERS
            DayCategory.FULL_BODY -> null // No hay equivalente exacto
            DayCategory.CARDIO -> null // No hay ejercicios de cardio en la lista
            DayCategory.REST -> null // DÃ­a de descanso
        }
    }
}