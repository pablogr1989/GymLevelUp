package com.gymlog.app.ui.screens.backup

import android.app.Application
import android.net.Uri
import android.webkit.MimeTypeMap
import androidx.documentfile.provider.DocumentFile
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gymlog.app.R
import com.gymlog.app.domain.repository.ExerciseRepository
import com.gymlog.app.util.BackupManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream
import javax.inject.Inject

sealed class BackupState {
    object Idle : BackupState()
    object Loading : BackupState()
    data class Success(val message: String) : BackupState()
    data class Error(val message: String) : BackupState()
}

@HiltViewModel
class BackupViewModel @Inject constructor(
    private val backupManager: BackupManager,
    private val application: Application,
    private val exerciseRepository: ExerciseRepository
) : ViewModel() {

    private val _state = MutableStateFlow<BackupState>(BackupState.Idle)
    val state: StateFlow<BackupState> = _state.asStateFlow()

    fun exportData(destinationUri: Uri) {
        viewModelScope.launch {
            _state.value = BackupState.Loading
            try {
                backupManager.exportDataToJson(destinationUri)
                _state.value = BackupState.Success(application.getString(R.string.backup_msg_export_success))
            } catch (e: Exception) {
                _state.value = BackupState.Error(application.getString(R.string.backup_msg_export_error, e.localizedMessage ?: "Desconocido"))
            }
        }
    }

    fun importData(uri: Uri) {
        viewModelScope.launch {
            _state.value = BackupState.Loading
            try {
                backupManager.importDataFromJson(uri)
                _state.value = BackupState.Success(application.getString(R.string.backup_msg_import_success))
            } catch (e: Exception) {
                _state.value = BackupState.Error(application.getString(R.string.backup_msg_import_error, e.localizedMessage ?: "Desconocido"))
            }
        }
    }

    fun exportImages(treeUri: Uri) {
        viewModelScope.launch {
            _state.value = BackupState.Loading
            try {
                val exercises = exerciseRepository.getAllExercises().first()
                val exercisesWithImages = exercises.filter { !it.imageUri.isNullOrEmpty() }

                if (exercisesWithImages.isEmpty()) {
                    _state.value = BackupState.Error("No tienes ningún ejercicio con imagen asignada para exportar.")
                    return@launch
                }

                val pickedDir = DocumentFile.fromTreeUri(application, treeUri)
                if (pickedDir == null || !pickedDir.canWrite()) {
                    _state.value = BackupState.Error("No se tienen permisos para escribir en la carpeta seleccionada.")
                    return@launch
                }

                var gymLogDir = pickedDir.findFile("GymLog_Images")
                if (gymLogDir == null) {
                    gymLogDir = pickedDir.createDirectory("GymLog_Images")
                }

                if (gymLogDir == null) {
                    _state.value = BackupState.Error("No se pudo crear la subcarpeta GymLog_Images.")
                    return@launch
                }

                var successCount = 0

                exercisesWithImages.forEach { exercise ->
                    try {
                        val imagePath = exercise.imageUri!!

                        val inputStream = if (imagePath.startsWith("content://")) {
                            application.contentResolver.openInputStream(Uri.parse(imagePath))
                        } else {
                            val file = File(imagePath)
                            if (file.exists()) file.inputStream() else null
                        }

                        if (inputStream != null) {
                            val cleanName = exercise.name.replace(Regex("[^a-zA-Z0-9_-]"), "_").replace(" ", "_")
                            var fileExtension = "jpg"
                            var mimeType = "image/jpeg"

                            if (imagePath.startsWith("content://")) {
                                val resolvedMime = application.contentResolver.getType(Uri.parse(imagePath))
                                if (resolvedMime != null) {
                                    mimeType = resolvedMime
                                    fileExtension = MimeTypeMap.getSingleton().getExtensionFromMimeType(mimeType) ?: "jpg"
                                }
                            } else {
                                val extension = File(imagePath).extension
                                if (extension.isNotEmpty()) {
                                    fileExtension = extension
                                    mimeType = MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension) ?: "image/jpeg"
                                }
                            }

                            val fileName = "${cleanName}_${exercise.id}.$fileExtension"

                            var destFile = gymLogDir.findFile(fileName)
                            if (destFile == null) {
                                destFile = gymLogDir.createFile(mimeType, fileName)
                            }

                            if (destFile != null) {
                                val outputStream = application.contentResolver.openOutputStream(destFile.uri)
                                if (outputStream != null) {
                                    inputStream.copyTo(outputStream)
                                    outputStream.close()
                                    successCount++
                                }
                            }
                            inputStream.close()
                        }
                    } catch (e: Exception) {
                        // Continuamos si falla una
                    }
                }

                if (successCount > 0) {
                    _state.value = BackupState.Success("Se han exportado $successCount imágenes con éxito.")
                } else {
                    _state.value = BackupState.Error("No se ha podido copiar ninguna imagen.")
                }

            } catch (e: Exception) {
                _state.value = BackupState.Error("Error al exportar: ${e.localizedMessage}")
            }
        }
    }

    // NUEVA FUNCIÓN: IMPORTAR IMÁGENES
    fun importImages(treeUri: Uri) {
        viewModelScope.launch {
            _state.value = BackupState.Loading
            try {
                val pickedDir = DocumentFile.fromTreeUri(application, treeUri)
                if (pickedDir == null || !pickedDir.canRead()) {
                    _state.value = BackupState.Error("No se tienen permisos para leer la carpeta seleccionada.")
                    return@launch
                }

                val exercises = exerciseRepository.getAllExercises().first()
                if (exercises.isEmpty()) {
                    _state.value = BackupState.Error("No hay ejercicios en la base de datos a los que asignar imágenes.")
                    return@launch
                }

                // Creamos/Accedemos a la bóveda interna de la app
                val internalDir = File(application.filesDir, "exercise_images")
                if (!internalDir.exists()) {
                    internalDir.mkdirs()
                }

                var successCount = 0

                // Leemos todos los archivos de la carpeta que ha elegido el usuario
                pickedDir.listFiles().forEach { file ->
                    val fileName = file.name ?: return@forEach

                    // Buscamos a qué ejercicio pertenece analizando el ID en el nombre (Ej: _ex_abductor_maquina.)
                    val matchingExercise = exercises.find { fileName.contains("_${it.id}.") }

                    if (matchingExercise != null) {
                        try {
                            val inputStream = application.contentResolver.openInputStream(file.uri)
                            if (inputStream != null) {
                                val ext = fileName.substringAfterLast('.', "jpg")
                                val internalFile = File(internalDir, "exercise_${matchingExercise.id}.$ext")

                                val outputStream = FileOutputStream(internalFile)
                                inputStream.copyTo(outputStream)

                                inputStream.close()
                                outputStream.close()

                                // Actualizamos la ruta de la imagen en la base de datos
                                val updatedExercise = matchingExercise.copy(imageUri = internalFile.absolutePath)
                                exerciseRepository.updateExercise(updatedExercise)

                                successCount++
                            }
                        } catch (e: Exception) {
                            // Si un archivo falla, seguimos con el siguiente
                        }
                    }
                }

                if (successCount > 0) {
                    _state.value = BackupState.Success("Se han importado y enlazado $successCount imágenes correctamente.")
                } else {
                    _state.value = BackupState.Error("No se han encontrado imágenes compatibles en esta carpeta.")
                }

            } catch (e: Exception) {
                _state.value = BackupState.Error("Error crítico al importar: ${e.localizedMessage}")
            }
        }
    }

    fun dismissMessage() {
        _state.value = BackupState.Idle
    }
}