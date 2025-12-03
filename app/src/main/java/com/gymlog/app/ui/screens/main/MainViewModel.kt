package com.gymlog.app.ui.screens.main

import android.app.Application
import android.util.Log
import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gymlog.app.R
import com.gymlog.app.data.local.GymLogDatabase
import com.gymlog.app.data.local.entity.ExerciseEntity
import com.gymlog.app.data.local.entity.ExerciseHistoryEntity
import com.gymlog.app.data.local.entity.MuscleGroup
import com.gymlog.app.data.local.entity.SetEntity
import com.gymlog.app.domain.model.Exercise
import com.gymlog.app.domain.repository.ExerciseRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    private val repository: ExerciseRepository,
    private val database: GymLogDatabase,
    private val application: Application
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    val searchQuery = _searchQuery.asStateFlow()

    private val _selectedMuscleGroup = MutableStateFlow<MuscleGroup?>(null)
    val selectedMuscleGroup = _selectedMuscleGroup.asStateFlow()

    private val _showDeleteDialog = MutableStateFlow<Exercise?>(null)
    val showDeleteDialog = _showDeleteDialog.asStateFlow()

    private val allExercisesFlow = repository.getAllExercises()
        .distinctUntilChanged()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Lazily,
            initialValue = emptyList()
        )

    val exercises: StateFlow<Map<MuscleGroup, List<Exercise>>> =
        combine(
            allExercisesFlow,
            searchQuery,
            selectedMuscleGroup
        ) { exercises, query, group ->
            when {
                exercises.isEmpty() -> emptyMap()
                query.isEmpty() && group == null -> {
                    exercises.groupBy { it.muscleGroup }
                }
                else -> {
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