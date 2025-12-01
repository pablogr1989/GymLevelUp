package com.gymlog.app.ui.screens.backup

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BackupScreen(
    onNavigateBack: () -> Unit,
    viewModel: BackupViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    val isProcessing = state is BackupState.Loading

    // Contrato SAF: Abrir Documento para IMPORTAR
    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        uri?.let { viewModel.importData(it) }
    }

    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json") // <-- Tipo MIME
    ) { uri: Uri? ->
        uri?.let { viewModel.exportData(it) } // Llama al ViewModel con la URI
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Copia de Seguridad") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Volver")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // Sección de Exportación (Backup)
            BackupCard(
                title = "Exportar Datos (Backup) 📤",
                // **ACTUALIZAR DESCRIPCIÓN**
                description = "Guarda todo el estado de la aplicación en un archivo JSON en la ubicación que elijas (ej. tu carpeta Downloads o Drive).",
                buttonText = "Seleccionar Ubicación", // **ACTUALIZAR TEXTO BOTÓN**
                icon = Icons.Default.CloudUpload,
                onClick = {
                    // Lanza el diálogo SAF para seleccionar la ubicación y el nombre
                    val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(
                        Date()
                    )
                    val defaultFileName = "GymLog_Backup_$timestamp.json"
                    exportLauncher.launch(defaultFileName) // <-- Lanza SAF con nombre sugerido
                },
                isProcessing = isProcessing
            )

            Divider()

            // Sección de Importación (Restore)
            RestoreCard(
                title = "Importar Datos (Restaurar) 📥",
                description = "Carga un archivo JSON de backup. ¡Advertencia! Esto eliminará y reemplazará *todos* los datos existentes.",
                buttonText = "Seleccionar Archivo JSON",
                icon = Icons.Default.CloudDownload,
                onClick = {
                    // Lanza el selector de archivos (SAF)
                    importLauncher.launch(arrayOf("application/json"))
                },
                isProcessing = isProcessing
            )
        }
    }

    // Mostrar estado/resultado
    when (val currentState = state) {
        is BackupState.Loading -> {
            AlertDialog(
                onDismissRequest = { /* No dismissable while loading */ },
                title = { Text("Procesando...") },
                text = { Text("Por favor, espera mientras se procesa la copia de seguridad/restauración. No cierres la aplicación.") },
                confirmButton = {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp))
                }
            )
        }
        is BackupState.Success -> {
            AlertDialog(
                onDismissRequest = viewModel::dismissMessage,
                title = { Text("Operación Exitosa ✅") },
                text = { Text(currentState.message) },
                confirmButton = {
                    TextButton(onClick = viewModel::dismissMessage) {
                        Text("Aceptar")
                    }
                }
            )
        }
        is BackupState.Error -> {
            AlertDialog(
                onDismissRequest = viewModel::dismissMessage,
                title = { Text("Error ❌") },
                text = { Text(currentState.message) },
                confirmButton = {
                    TextButton(onClick = viewModel::dismissMessage) {
                        Text("Cerrar")
                    }
                }
            )
        }
        BackupState.Idle -> { /* Do nothing */ }
    }
}

@Composable
private fun BackupCard(
    title: String,
    description: String,
    buttonText: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit,
    isProcessing: Boolean
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(description, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(modifier = Modifier.height(16.dp))
            Button(onClick = onClick, modifier = Modifier.fillMaxWidth(), enabled = !isProcessing) {
                if (isProcessing) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp), color = MaterialTheme.colorScheme.onPrimary, strokeWidth = 2.dp)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Exportando...")
                } else {
                    Text(buttonText)
                }
            }
        }
    }
}

@Composable
private fun RestoreCard(
    title: String,
    description: String,
    buttonText: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit,
    isProcessing: Boolean
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(description, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = onClick,
                enabled = !isProcessing,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
            ) {
                if (isProcessing) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp), color = MaterialTheme.colorScheme.onPrimary, strokeWidth = 2.dp)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Importando...")
                } else {
                    Text(buttonText)
                }
            }
        }
    }
}