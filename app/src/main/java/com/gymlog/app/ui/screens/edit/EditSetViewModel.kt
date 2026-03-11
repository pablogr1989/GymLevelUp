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

    private val _minReps = MutableStateFlow("")
    val minReps = _minReps.asStateFlow()

    private val _maxReps = MutableStateFlow("")
    val maxReps = _maxReps.asStateFlow()

    private val _weight = MutableStateFlow("")
    val weight = _weight.asStateFlow()

    private val _minRir = MutableStateFlow("")
    val minRir = _minRir.asStateFlow()

    private val _maxRir = MutableStateFlow("")
    val maxRir = _maxRir.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading = _isLoading.asStateFlow()

    private val _navigateBack = MutableStateFlow(false)
    val navigateBack = _navigateBack.asStateFlow()

    private val _showExitConfirmation = MutableStateFlow(false)
    val showExitConfirmation = _showExitConfirmation.asStateFlow()

    private var initialSeries = ""
    private var initialMinReps = ""
    private var initialMaxReps = ""
    private var initialWeight = ""
    private var initialMinRir = ""
    private var initialMaxRir = ""

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
                _minReps.value = set.minReps.toString()
                _maxReps.value = set.maxReps.toString()
                _weight.value = set.weightKg.toString()
                _minRir.value = set.minRir?.toString() ?: ""
                _maxRir.value = set.maxRir?.toString() ?: ""

                initialSeries = _series.value
                initialMinReps = _minReps.value
                initialMaxReps = _maxReps.value
                initialWeight = _weight.value
                initialMinRir = _minRir.value
                initialMaxRir = _maxRir.value
            }
            _isLoading.value = false
        }
    }

    fun updateSeries(value: String) {
        if (value.validateInt()) _series.value = value
    }

    fun updateMinReps(value: String) {
        if (value.validateInt()) _minReps.value = value
    }

    fun updateMaxReps(value: String) {
        if (value.validateInt()) _maxReps.value = value
    }

    fun updateWeight(value: String) {
        if (value.validateFloat()) _weight.value = value
    }

    fun updateMinRir(value: String) {
        if (value.validateInt() || value.isEmpty()) _minRir.value = value
    }

    fun updateMaxRir(value: String) {
        if (value.validateInt() || value.isEmpty()) _maxRir.value = value
    }

    fun saveSet() {
        val seriesVal = _series.value.toIntOrNull() ?: 0
        val minRepsVal = _minReps.value.toIntOrNull() ?: 0
        val maxRepsVal = _maxReps.value.toIntOrNull() ?: 0
        val weightVal = _weight.value.toFloatOrNull() ?: 0f
        val minRirVal = _minRir.value.toIntOrNull()
        val maxRirVal = _maxRir.value.toIntOrNull()

        if (seriesVal == 0 && minRepsVal == 0 && maxRepsVal == 0) return

        viewModelScope.launch {
            _isLoading.value = true
            val set = Set(
                id = setId ?: UUID.randomUUID().toString(),
                exerciseId = exerciseId,
                series = seriesVal,
                minReps = minRepsVal,
                maxReps = maxRepsVal,
                weightKg = weightVal,
                minRir = minRirVal,
                maxRir = maxRirVal
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
                _minReps.value != initialMinReps ||
                _maxReps.value != initialMaxReps ||
                _weight.value != initialWeight ||
                _minRir.value != initialMinRir ||
                _maxRir.value != initialMaxRir
    }
}