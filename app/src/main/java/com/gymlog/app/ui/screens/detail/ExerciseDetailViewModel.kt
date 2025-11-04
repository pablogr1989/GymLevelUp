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
import java.text.SimpleDateFormat
import java.util.*
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
    
    private val _notes = MutableStateFlow("")
    val notes = _notes.asStateFlow()
    
    private val _showSaveSuccess = MutableStateFlow(false)
    val showSaveSuccess = _showSaveSuccess.asStateFlow()
    
    private val _isLoading = MutableStateFlow(false)
    val isLoading = _isLoading.asStateFlow()
    
    private val _showDeleteHistoryDialog = MutableStateFlow(false)
    val showDeleteHistoryDialog = _showDeleteHistoryDialog.asStateFlow()
    
    private val _showDeleteEntryDialog = MutableStateFlow<ExerciseHistory?>(null)
    val showDeleteEntryDialog = _showDeleteEntryDialog.asStateFlow()
    
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
                _notes.value = exercise.notes
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
    
    fun updateNotes(value: String) {
        _notes.value = value
    }
    
    fun saveExerciseStats() {
        viewModelScope.launch {
            val currentExercise = _exercise.value ?: return@launch
            val seriesValue = _series.value.toIntOrNull() ?: return@launch
            val repsValue = _reps.value.toIntOrNull() ?: return@launch
            val weightValue = _weight.value.toFloatOrNull() ?: 0f
            val notesValue = _notes.value
            
            _isLoading.value = true
            
            // Verificar si cambiaron stats o solo notas
            val statsChanged = seriesValue != currentExercise.currentSeries ||
                               repsValue != currentExercise.currentReps ||
                               weightValue != currentExercise.currentWeightKg
            
            val notesChanged = notesValue != currentExercise.notes
            
            if (statsChanged) {
                // Actualizar stats
                repository.updateExerciseStats(exerciseId, seriesValue, repsValue, weightValue)
                
                // Añadir entrada al historial
                repository.insertHistory(
                    ExerciseHistory(
                        id = UUID.randomUUID().toString(),
                        exerciseId = exerciseId,
                        timestamp = System.currentTimeMillis(),
                        series = seriesValue,
                        reps = repsValue,
                        weightKg = weightValue
                    )
                )
                
                // Actualizar changeLogText
                val dateFormat = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
                val timestamp = dateFormat.format(Date())
                val newLogEntry = "$timestamp — $seriesValue series × $repsValue reps — $weightValue kg"
                val updatedChangeLog = if (currentExercise.changeLogText.isEmpty()) {
                    newLogEntry
                } else {
                    "${currentExercise.changeLogText}\n$newLogEntry"
                }
                repository.updateExerciseChangeLog(exerciseId, updatedChangeLog)
            }
            
            if (notesChanged) {
                // Solo actualizar notas (no crea historial)
                repository.updateExerciseNotes(exerciseId, notesValue)
            }
            
            // Reload exercise
            loadExercise()
            
            _showSaveSuccess.value = true
            _isLoading.value = false
        }
    }
    
    fun dismissSaveSuccess() {
        _showSaveSuccess.value = false
    }
    
    fun showDeleteHistoryDialog() {
        _showDeleteHistoryDialog.value = true
    }
    
    fun dismissDeleteHistoryDialog() {
        _showDeleteHistoryDialog.value = false
    }
    
    fun deleteAllHistory() {
        viewModelScope.launch {
            repository.deleteAllHistoryForExercise(exerciseId)
            _showDeleteHistoryDialog.value = false
        }
    }
    
    fun showDeleteEntryDialog(entry: ExerciseHistory) {
        _showDeleteEntryDialog.value = entry
    }
    
    fun dismissDeleteEntryDialog() {
        _showDeleteEntryDialog.value = null
    }
    
    fun deleteHistoryEntry(entry: ExerciseHistory) {
        viewModelScope.launch {
            repository.deleteHistoryById(entry.id)
            _showDeleteEntryDialog.value = null
        }
    }
    
    fun resetToCurrentValues() {
        val currentExercise = _exercise.value ?: return
        _series.value = currentExercise.currentSeries.toString()
        _reps.value = currentExercise.currentReps.toString()
        _weight.value = currentExercise.currentWeightKg.toString()
        _notes.value = currentExercise.notes
    }
}
