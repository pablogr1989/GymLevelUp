package com.gymlog.app.ui.screens.create

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gymlog.app.data.local.entity.MuscleGroup
import com.gymlog.app.domain.model.Exercise
import com.gymlog.app.domain.model.ExerciseHistory
import com.gymlog.app.domain.model.Set
import com.gymlog.app.domain.repository.ExerciseRepository
import com.gymlog.app.util.ImageStorageHelper
import com.gymlog.app.util.InputValidator.validateFloat
import com.gymlog.app.util.InputValidator.validateInt
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class CreateExerciseViewModel @Inject constructor(
    private val repository: ExerciseRepository,
    private val imageStorageHelper: ImageStorageHelper
) : ViewModel() {

    private val _name = MutableStateFlow("")
    val name = _name.asStateFlow()

    private val _description = MutableStateFlow("")
    val description = _description.asStateFlow()

    private val _selectedMuscleGroup = MutableStateFlow<MuscleGroup?>(null)
    val selectedMuscleGroup = _selectedMuscleGroup.asStateFlow()

    private val _imageUri = MutableStateFlow<Uri?>(null)
    val imageUri = _imageUri.asStateFlow()

    private val _series = MutableStateFlow("")
    val series = _series.asStateFlow()

    private val _minReps = MutableStateFlow("")
    val minReps = _minReps.asStateFlow()

    private val _maxReps = MutableStateFlow("")
    val maxReps = _maxReps.asStateFlow()

    private val _weight = MutableStateFlow("")
    val weight = _weight.asStateFlow()

    // NUEVO: RIR
    private val _minRir = MutableStateFlow("")
    val minRir = _minRir.asStateFlow()

    private val _maxRir = MutableStateFlow("")
    val maxRir = _maxRir.asStateFlow()

    private val _showMuscleGroupError = MutableStateFlow(false)
    val showMuscleGroupError = _showMuscleGroupError.asStateFlow()

    private val _showNameError = MutableStateFlow(false)
    val showNameError = _showNameError.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading = _isLoading.asStateFlow()

    private val _navigateBack = MutableStateFlow(false)
    val navigateBack = _navigateBack.asStateFlow()

    fun updateName(value: String) {
        _name.value = value
        _showNameError.value = false
    }

    fun updateDescription(value: String) {
        _description.value = value
    }

    fun selectMuscleGroup(group: MuscleGroup) {
        _selectedMuscleGroup.value = group
        _showMuscleGroupError.value = false
    }

    fun updateImageUri(uri: Uri?) {
        _imageUri.value = uri
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

    // NUEVO: Actualizar RIR
    fun updateMinRir(value: String) {
        if (value.validateInt() || value.isEmpty()) _minRir.value = value
    }

    fun updateMaxRir(value: String) {
        if (value.validateInt() || value.isEmpty()) _maxRir.value = value
    }

    fun saveExercise() {
        if (_name.value.trim().isEmpty()) {
            _showNameError.value = true
            return
        }

        if (_selectedMuscleGroup.value == null) {
            _showMuscleGroupError.value = true
            return
        }

        viewModelScope.launch {
            _isLoading.value = true

            val savedImagePath = _imageUri.value?.let { uri ->
                imageStorageHelper.saveImageToInternalStorage(uri)
            }

            val exerciseId = UUID.randomUUID().toString()
            val seriesValue = _series.value.toIntOrNull() ?: 0
            val minRepsValue = _minReps.value.toIntOrNull() ?: 0
            val maxRepsValue = _maxReps.value.toIntOrNull() ?: 0
            val weightValue = _weight.value.toFloatOrNull() ?: 0f
            val minRirValue = _minRir.value.toIntOrNull()
            val maxRirValue = _maxRir.value.toIntOrNull()

            val initialSets = if (seriesValue > 0 || minRepsValue > 0 || maxRepsValue > 0 || weightValue > 0f) {
                listOf(
                    Set(
                        id = UUID.randomUUID().toString(),
                        exerciseId = exerciseId,
                        series = seriesValue,
                        minReps = minRepsValue,
                        maxReps = maxRepsValue,
                        weightKg = weightValue,
                        minRir = minRirValue,
                        maxRir = maxRirValue
                    )
                )
            } else {
                emptyList()
            }

            val exercise = Exercise(
                id = exerciseId,
                name = _name.value.trim(),
                description = _description.value.trim(),
                muscleGroup = _selectedMuscleGroup.value!!,
                imageUri = savedImagePath,
                sets = initialSets,
                createdAt = System.currentTimeMillis()
            )

            repository.insertExercise(exercise)

            if (initialSets.isNotEmpty()) {
                val firstSet = initialSets.first()
                repository.insertHistory(
                    ExerciseHistory(
                        id = UUID.randomUUID().toString(),
                        exerciseId = exerciseId,
                        setId = firstSet.id,
                        timestamp = System.currentTimeMillis(),
                        series = firstSet.series,
                        reps = firstSet.minReps,
                        weightKg = firstSet.weightKg
                    )
                )
            }

            _isLoading.value = false
            _navigateBack.value = true
        }
    }

    fun resetNavigation() {
        _navigateBack.value = false
    }
}