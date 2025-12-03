package com.gymlog.app.ui.screens.backup

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.gymlog.app.R
import com.gymlog.app.ui.theme.*
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

    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        uri?.let { viewModel.importData(it) }
    }

    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri: Uri? ->
        uri?.let { viewModel.exportData(it) }
    }

    Scaffold(
        containerColor = HunterBlack,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        stringResource(R.string.backup_title),
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Black,
                            letterSpacing = 1.sp
                        )
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = stringResource(R.string.common_back), tint = HunterTextPrimary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = HunterBlack,
                    titleContentColor = HunterTextPrimary
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
            // HEADER VISUAL
            Box(
                modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Storage,
                    contentDescription = null,
                    modifier = Modifier.size(100.dp),
                    tint = HunterPrimary.copy(alpha = 0.2f)
                )
                Icon(
                    imageVector = Icons.Default.Save,
                    contentDescription = null,
                    modifier = Modifier.size(48.dp),
                    tint = HunterPrimary
                )
            }

            // TARJETA EXPORTAR
            HunterCard {
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .background(ScreenColors.Backup.ExportIconBg, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.CloudUpload, null, tint = ScreenColors.Backup.ExportIconTint)
                        }
                        Spacer(modifier = Modifier.width(16.dp))
                        Text(stringResource(R.string.backup_export_title), style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold), color = HunterTextPrimary)
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        stringResource(R.string.backup_export_desc),
                        style = MaterialTheme.typography.bodyMedium,
                        color = HunterTextSecondary
                    )
                    Spacer(modifier = Modifier.height(20.dp))

                    HunterButton(
                        text = stringResource(R.string.backup_btn_create),
                        onClick = {
                            val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
                            exportLauncher.launch("GymLog_Backup_$timestamp.json")
                        },
                        color = ScreenColors.Backup.ExportColorButton,
                        enabled = !isProcessing
                    )
                }
            }

            // TARJETA IMPORTAR (Danger Zone)
            HunterCard {
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .background(ScreenColors.Backup.ImportIconBg, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.CloudDownload, null, tint = ScreenColors.Backup.ImportIconTint)
                        }
                        Spacer(modifier = Modifier.width(16.dp))
                        Text(stringResource(R.string.backup_import_title), style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold), color = HunterSecondary)
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        stringResource(R.string.backup_import_desc),
                        style = MaterialTheme.typography.bodyMedium,
                        color = HunterTextSecondary
                    )
                    Spacer(modifier = Modifier.height(20.dp))

                    HunterButton(
                        text = stringResource(R.string.backup_btn_select_file),
                        onClick = { importLauncher.launch(arrayOf("application/json")) },
                        color = ScreenColors.Backup.ImportColorButton,
                        textColor = HunterSecondary,
                        enabled = !isProcessing
                    )
                }
            }
        }
    }

    // Estados de carga y mensajes
    when (val currentState = state) {
        is BackupState.Loading -> {
            AlertDialog(
                onDismissRequest = {},
                containerColor = HunterSurface,
                title = { Text(stringResource(R.string.backup_processing_title), color = HunterTextPrimary, fontWeight = FontWeight.Bold) },
                text = { Text(stringResource(R.string.backup_processing_text), color = HunterTextSecondary) },
                confirmButton = { CircularProgressIndicator(color = HunterPrimary) }
            )
        }
        is BackupState.Success -> {
            HunterConfirmDialog(
                title = stringResource(R.string.backup_dialog_success_title),
                text = currentState.message,
                confirmText = stringResource(R.string.common_accept),
                onConfirm = viewModel::dismissMessage,
                onDismiss = {}
            )
        }
        is BackupState.Error -> {
            HunterConfirmDialog(
                title = stringResource(R.string.backup_dialog_error_title),
                text = currentState.message,
                confirmText = stringResource(R.string.common_close),
                onConfirm = viewModel::dismissMessage,
                onDismiss = {}
            )
        }
        BackupState.Idle -> {}
    }
}