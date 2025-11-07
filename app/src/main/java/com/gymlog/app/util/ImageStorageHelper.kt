package com.gymlog.app.util

import android.content.Context
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ImageStorageHelper @Inject constructor(
    private val context: Context
) {

    /**
     * Copia una imagen desde una URI temporal a almacenamiento interno permanente
     * @return URI persistente de la imagen copiada, o null si falla
     */
    suspend fun saveImageToInternalStorage(sourceUri: Uri): String? = withContext(Dispatchers.IO) {
        try {
            // Crear directorio de imágenes si no existe
            val imagesDir = File(context.filesDir, "exercise_images")
            if (!imagesDir.exists()) {
                imagesDir.mkdirs()
            }

            // Generar nombre único para la imagen
            val fileName = "exercise_${UUID.randomUUID()}.jpg"
            val destinationFile = File(imagesDir, fileName)

            // Copiar contenido de la URI al archivo interno
            context.contentResolver.openInputStream(sourceUri)?.use { inputStream ->
                FileOutputStream(destinationFile).use { outputStream ->
                    inputStream.copyTo(outputStream)
                }
            }

            // Retornar URI persistente (file:// path)
            destinationFile.absolutePath

        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     * Elimina una imagen del almacenamiento interno
     */
    suspend fun deleteImage(imagePath: String?) = withContext(Dispatchers.IO) {
        try {
            if (imagePath != null && imagePath.startsWith("/")) {
                val file = File(imagePath)
                if (file.exists()) {
                    file.delete()
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * Convierte path absoluto a URI para Coil
     */
    fun pathToUri(path: String?): Uri? {
        return path?.let { Uri.parse("file://$it") }
    }
}