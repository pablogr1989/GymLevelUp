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
import kotlin.text.ifEmpty

@HiltViewModel
class ExerciseDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val repository: ExerciseRepository
) : ViewModel() {

    private val exerciseId: String = savedStateHandle.get<String>("exerciseId") ?: ""

    private val _exercise = MutableStateFlow<Exercise?>(null)
    val exercise = _exercise.asStateFlow()

    private val _notes = MutableStateFlow("")
    val notes = _notes.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading = _isLoading.asStateFlow()

    private val _showDeleteHistoryDialog = MutableStateFlow(false)
    val showDeleteHistoryDialog = _showDeleteHistoryDialog.asStateFlow()

    private val _showDeleteEntryDialog = MutableStateFlow<ExerciseHistory?>(null)
    val showDeleteEntryDialog = _showDeleteEntryDialog.asStateFlow()

    private val _showDeleteSetDialog = MutableStateFlow<String?>(null) // ID del set a borrar
    val showDeleteSetDialog = _showDeleteSetDialog.asStateFlow()

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

    // Se hace público para recargar tras editar sets
    fun loadExercise() {
        viewModelScope.launch {
            _isLoading.value = true
            repository.getExerciseById(exerciseId)?.let { exercise ->
                _exercise.value = exercise
                _notes.value = exercise.notes
            }
            _isLoading.value = false
        }
    }

    fun updateNotes(value: String) {
        _notes.value = value
    }

    // Solo guarda las notas, ya no hay "valores actuales" que guardar aquí
    fun saveNotes() {
        viewModelScope.launch {
            val currentExercise = _exercise.value ?: return@launch
            val notesValue = _notes.value.trim()

            if (notesValue != currentExercise.notes) {
                _isLoading.value = true
                repository.updateExerciseNotes(exerciseId, notesValue)
                loadExercise()
                _isLoading.value = false
            }
        }
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

    fun confirmDeleteSet(setId: String) {
        _showDeleteSetDialog.value = setId
    }

    fun dismissDeleteSetDialog() {
        _showDeleteSetDialog.value = null
    }

    fun deleteSet() {
        val setId = _showDeleteSetDialog.value ?: return
        viewModelScope.launch {
            repository.deleteSet(setId)
            _showDeleteSetDialog.value = null
            loadExercise() // Recargar la lista
        }
    }
}