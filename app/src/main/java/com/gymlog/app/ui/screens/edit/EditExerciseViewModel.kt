package com.gymlog.app.ui.screens.edit

import android.net.Uri
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gymlog.app.data.local.entity.MuscleGroup
import com.gymlog.app.domain.repository.ExerciseRepository
import com.gymlog.app.util.ImageStorageHelper
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class EditExerciseViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val repository: ExerciseRepository,
    private val imageStorageHelper: ImageStorageHelper
) : ViewModel() {

    private val exerciseId: String = savedStateHandle.get<String>("exerciseId") ?: ""

    private val _name = MutableStateFlow("")
    val name = _name.asStateFlow()

    private val _description = MutableStateFlow("")
    val description = _description.asStateFlow()

    private val _selectedMuscleGroup = MutableStateFlow<MuscleGroup?>(null)
    val selectedMuscleGroup = _selectedMuscleGroup.asStateFlow()

    private val _imageUri = MutableStateFlow<Uri?>(null)
    val imageUri = _imageUri.asStateFlow()

    // Path persistente de la imagen actual
    private val _currentImagePath = MutableStateFlow<String?>(null)

    // URI temporal seleccionada (no persistente)
    private var pendingImageUri: Uri? = null

    private val _showMuscleGroupError = MutableStateFlow(false)
    val showMuscleGroupError = _showMuscleGroupError.asStateFlow()

    private val _showNameError = MutableStateFlow(false)
    val showNameError = _showNameError.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading = _isLoading.asStateFlow()

    private val _navigateBack = MutableStateFlow(false)
    val navigateBack = _navigateBack.asStateFlow()

    init {
        loadExercise()
    }

    private fun loadExercise() {
        viewModelScope.launch {
            repository.getExerciseById(exerciseId)?.let { exercise ->
                _name.value = exercise.name
                _description.value = exercise.description
                _selectedMuscleGroup.value = exercise.muscleGroup
                _currentImagePath.value = exercise.imageUri

                // Convertir path a URI para mostrar en UI
                if (exercise.imageUri != null) {
                    _imageUri.value = imageStorageHelper.pathToUri(exercise.imageUri)
                }
            }
        }
    }

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
        pendingImageUri = uri
        _imageUri.value = uri // Mostrar preview inmediato
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

            // Si hay una imagen nueva pendiente, copiarla a almacenamiento interno
            val finalImagePath = if (pendingImageUri != null) {
                // Eliminar imagen anterior si existe
                imageStorageHelper.deleteImage(_currentImagePath.value)

                // Guardar nueva imagen
                imageStorageHelper.saveImageToInternalStorage(pendingImageUri!!)
            } else {
                // Mantener imagen actual
                _currentImagePath.value
            }

            repository.updateExerciseInfo(
                exerciseId = exerciseId,
                name = _name.value.trim(),
                description = _description.value.trim(),
                muscleGroup = _selectedMuscleGroup.value!!,
                imageUri = finalImagePath // Guardar path absoluto, no URI
            )

            _isLoading.value = false
            _navigateBack.value = true
        }
    }

    fun resetNavigation() {
        _navigateBack.value = false
    }
}