package com.gymlog.app.ui.screens.backup

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gymlog.app.util.BackupManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed class BackupState {
    object Idle : BackupState()
    object Loading : BackupState()
    data class Success(val message: String) : BackupState()
    data class Error(val message: String) : BackupState()
}

@HiltViewModel
class BackupViewModel @Inject constructor(
    private val backupManager: BackupManager
) : ViewModel() {

    private val _state = MutableStateFlow<BackupState>(BackupState.Idle)
    val state: StateFlow<BackupState> = _state.asStateFlow()

    /**
     * Inicia el proceso de exportación de datos.
     */
    fun exportData(destinationUri: Uri) {
        viewModelScope.launch {
            _state.value = BackupState.Loading
            try {
                backupManager.exportDataToJson(destinationUri)
                _state.value = BackupState.Success("Datos exportados con éxito a la ubicación seleccionada.")
            } catch (e: Exception) {
                _state.value = BackupState.Error("Error al exportar: ${e.localizedMessage ?: "Desconocido"}")
            }
        }
    }

    /**
     * Inicia el proceso de importación usando una URI del SAF.
     */
    fun importData(uri: Uri) {
        viewModelScope.launch {
            _state.value = BackupState.Loading
            try {
                // Llama al manager para importar desde el URI
                backupManager.importDataFromJson(uri)
                _state.value = BackupState.Success("Datos importados con éxito y base de datos restaurada.")
            } catch (e: Exception) {
                _state.value = BackupState.Error("Error al importar. Asegúrate de que el archivo es un JSON válido. Mensaje: ${e.localizedMessage ?: "Desconocido"}")
            }
        }
    }

    fun dismissMessage() {
        _state.value = BackupState.Idle
    }
}