package com.gymlog.app.ui.screens.edit

import android.net.Uri
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gymlog.app.data.local.entity.MuscleGroup
import com.gymlog.app.domain.repository.ExerciseRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class EditExerciseViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val repository: ExerciseRepository
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
    
    private val _currentImageUriString = MutableStateFlow<String?>(null)
    
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
                _currentImageUriString.value = exercise.imageUri
                if (exercise.imageUri != null) {
                    try {
                        _imageUri.value = Uri.parse(exercise.imageUri)
                    } catch (e: Exception) {
                        // Ignore invalid URI
                    }
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
        _imageUri.value = uri
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
            
            val imageUriString = _imageUri.value?.toString() ?: _currentImageUriString.value
            
            repository.updateExerciseInfo(
                exerciseId = exerciseId,
                name = _name.value.trim(),
                description = _description.value.trim(),
                muscleGroup = _selectedMuscleGroup.value!!,
                imageUri = imageUriString
            )
            
            _isLoading.value = false
            _navigateBack.value = true
        }
    }
    
    fun resetNavigation() {
        _navigateBack.value = false
    }
}
