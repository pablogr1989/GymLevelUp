package com.gymlog.app.ui.screens.edit

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gymlog.app.domain.model.Set
import com.gymlog.app.domain.repository.ExerciseRepository
import com.gymlog.app.util.InputValidator.validateFloat
import com.gymlog.app.util.InputValidator.validateInt
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class EditSetViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val repository: ExerciseRepository
) : ViewModel() {

    private val exerciseId: String = checkNotNull(savedStateHandle["exerciseId"])
    private val setId: String? = savedStateHandle["setId"]

    private val _series = MutableStateFlow("")
    val series = _series.asStateFlow()

    private val _reps = MutableStateFlow("")
    val reps = _reps.asStateFlow()

    private val _weight = MutableStateFlow("")
    val weight = _weight.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading = _isLoading.asStateFlow()

    private val _navigateBack = MutableStateFlow(false)
    val navigateBack = _navigateBack.asStateFlow()

    private val _showExitConfirmation = MutableStateFlow(false)
    val showExitConfirmation = _showExitConfirmation.asStateFlow()

    private var initialSeries = ""
    private var initialReps = ""
    private var initialWeight = ""

    init {
        if (setId != null) {
            loadSet(setId)
        }
    }

    private fun loadSet(id: String) {
        viewModelScope.launch {
            _isLoading.value = true
            repository.getSetById(id)?.let { set ->
                _series.value = set.series.toString()
                _reps.value = set.reps.toString()
                _weight.value = set.weightKg.toString()

                // Guardar estado inicial para detectar cambios
                initialSeries = _series.value
                initialReps = _reps.value
                initialWeight = _weight.value
            }
            _isLoading.value = false
        }
    }

    fun updateSeries(value: String) {
        if (value.validateInt()) _series.value = value
    }

    fun updateReps(value: String) {
        if (value.validateInt()) _reps.value = value
    }

    fun updateWeight(value: String) {
        if (value.validateFloat()) _weight.value = value
    }

    fun saveSet() {
        val seriesVal = _series.value.toIntOrNull() ?: 0
        val repsVal = _reps.value.toIntOrNull() ?: 0
        val weightVal = _weight.value.toFloatOrNull() ?: 0f

        if (seriesVal == 0 && repsVal == 0) return // Validación mínima

        viewModelScope.launch {
            _isLoading.value = true
            val set = Set(
                id = setId ?: UUID.randomUUID().toString(),
                exerciseId = exerciseId,
                series = seriesVal,
                reps = repsVal,
                weightKg = weightVal
            )

            if (setId != null) {
                repository.updateSet(set)
            } else {
                repository.insertSet(set)
            }

            _isLoading.value = false
            _navigateBack.value = true
        }
    }

    fun onBackPressed() {
        if (hasChanges()) {
            _showExitConfirmation.value = true
        } else {
            _navigateBack.value = true
        }
    }

    fun confirmExit() {
        _showExitConfirmation.value = false
        _navigateBack.value = true
    }

    fun dismissExitConfirmation() {
        _showExitConfirmation.value = false
    }

    fun resetNavigation() {
        _navigateBack.value = false
    }

    private fun hasChanges(): Boolean {
        return _series.value != initialSeries ||
                _reps.value != initialReps ||
                _weight.value != initialWeight
    }
}