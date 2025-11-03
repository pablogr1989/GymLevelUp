package com.gymlog.app.ui.screens.detail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gymlog.app.domain.model.Exercise
import com.gymlog.app.domain.model.ExerciseHistory
import com.gymlog.app.domain.repository.ExerciseRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ExerciseDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val repository: ExerciseRepository
) : ViewModel() {
    
    private val exerciseId: String = savedStateHandle.get<String>("exerciseId") ?: ""
    
    private val _exercise = MutableStateFlow<Exercise?>(null)
    val exercise = _exercise.asStateFlow()
    
    private val _series = MutableStateFlow("")
    val series = _series.asStateFlow()
    
    private val _reps = MutableStateFlow("")
    val reps = _reps.asStateFlow()
    
    private val _weight = MutableStateFlow("")
    val weight = _weight.asStateFlow()
    
    private val _showSaveSuccess = MutableStateFlow(false)
    val showSaveSuccess = _showSaveSuccess.asStateFlow()
    
    private val _isLoading = MutableStateFlow(false)
    val isLoading = _isLoading.asStateFlow()
    
    val history: StateFlow<List<ExerciseHistory>> = repository
        .getHistoryForExercise(exerciseId)
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )
    
    init {
        loadExercise()
    }
    
    private fun loadExercise() {
        viewModelScope.launch {
            _isLoading.value = true
            repository.getExerciseById(exerciseId)?.let { exercise ->
                _exercise.value = exercise
                _series.value = exercise.currentSeries.toString()
                _reps.value = exercise.currentReps.toString()
                _weight.value = exercise.currentWeightKg.toString()
            }
            _isLoading.value = false
        }
    }
    
    fun updateSeries(value: String) {
        if (value.isEmpty() || value.all { it.isDigit() }) {
            _series.value = value
        }
    }
    
    fun updateReps(value: String) {
        if (value.isEmpty() || value.all { it.isDigit() }) {
            _reps.value = value
        }
    }
    
    fun updateWeight(value: String) {
        if (value.isEmpty() || value.matches(Regex("^\\d*\\.?\\d*$"))) {
            _weight.value = value
        }
    }
    
    fun saveExerciseStats() {
        viewModelScope.launch {
            val seriesValue = _series.value.toIntOrNull() ?: return@launch
            val repsValue = _reps.value.toIntOrNull() ?: return@launch
            val weightValue = _weight.value.toFloatOrNull() ?: 0f
            
            _isLoading.value = true
            
            // Update exercise stats
            repository.updateExerciseStats(exerciseId, seriesValue, repsValue, weightValue)
            
            // Add history entry
            repository.insertHistory(
                ExerciseHistory(
                    id = java.util.UUID.randomUUID().toString(),
                    exerciseId = exerciseId,
                    timestamp = System.currentTimeMillis(),
                    series = seriesValue,
                    reps = repsValue,
                    weightKg = weightValue
                )
            )
            
            // Reload exercise to get updated values
            loadExercise()
            
            _showSaveSuccess.value = true
            _isLoading.value = false
        }
    }
    
    fun dismissSaveSuccess() {
        _showSaveSuccess.value = false
    }
    
    fun deleteHistoryEntry(entry: ExerciseHistory) {
        viewModelScope.launch {
            repository.deleteHistory(entry)
        }
    }
}
