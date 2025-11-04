package com.gymlog.app.ui.screens.main

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gymlog.app.data.local.entity.MuscleGroup
import com.gymlog.app.domain.model.Exercise
import com.gymlog.app.domain.repository.ExerciseRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    private val repository: ExerciseRepository
) : ViewModel() {
    
    private val _searchQuery = MutableStateFlow("")
    val searchQuery = _searchQuery.asStateFlow()
    
    private val _selectedMuscleGroup = MutableStateFlow<MuscleGroup?>(null)
    val selectedMuscleGroup = _selectedMuscleGroup.asStateFlow()
    
    private val _showDeleteDialog = MutableStateFlow<Exercise?>(null)
    val showDeleteDialog = _showDeleteDialog.asStateFlow()
    
    // Cachear ejercicios con distinctUntilChanged para evitar emisiones duplicadas
    private val allExercisesFlow = repository.getAllExercises()
        .distinctUntilChanged()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Lazily, // Lazy en lugar de WhileSubscribed
            initialValue = emptyList()
        )
    
    // Optimizar filtrado con early returns y sequence para lazy evaluation
    val exercises: StateFlow<Map<MuscleGroup, List<Exercise>>> = 
        combine(
            allExercisesFlow,
            searchQuery,  // StateFlow ya tiene distinctUntilChanged incorporado
            selectedMuscleGroup  // StateFlow ya tiene distinctUntilChanged incorporado
        ) { exercises, query, group ->
            // Early return para caso común sin filtros
            when {
                exercises.isEmpty() -> emptyMap()
                query.isEmpty() && group == null -> {
                    // Caso más común: sin filtros
                    exercises.groupBy { it.muscleGroup }
                }
                else -> {
                    // Usar sequence para lazy evaluation
                    exercises.asSequence()
                        .filter { exercise ->
                            query.isEmpty() || exercise.name.contains(query, ignoreCase = true)
                        }
                        .filter { exercise ->
                            group == null || exercise.muscleGroup == group
                        }
                        .groupBy { it.muscleGroup }
                }
            }
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.Lazily,
            initialValue = emptyMap()
        )
    
    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }
    
    fun selectMuscleGroup(group: MuscleGroup?) {
        _selectedMuscleGroup.value = group
    }
    
    fun showDeleteDialog(exercise: Exercise) {
        _showDeleteDialog.value = exercise
    }
    
    fun dismissDeleteDialog() {
        _showDeleteDialog.value = null
    }
    
    fun deleteExercise(exercise: Exercise) {
        viewModelScope.launch {
            repository.deleteExercise(exercise)
            dismissDeleteDialog()
        }
    }
}
