package com.gymlog.app.ui.screens.history

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.gymlog.app.R
import com.gymlog.app.ui.theme.*
import com.gymlog.app.ui.util.UiMappers
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExportHistoryScreen(
    onNavigateBack: () -> Unit,
    viewModel: ExportHistoryViewModel = hiltViewModel()
) {
    val exportGroups by viewModel.exportGroups.collectAsState()
    val selectedGroupIds by viewModel.selectedGroupIds.collectAsState()
    val isAscending by viewModel.isAscending.collectAsState()
    val exportState by viewModel.exportState.collectAsState()

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
                        "Exportar Historial",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Black, letterSpacing = 1.sp)
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, null, tint = HunterTextPrimary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = HunterBlack, titleContentColor = HunterTextPrimary)
            )
        },
        bottomBar = {
            Surface(color = HunterSurface, modifier = Modifier.fillMaxWidth()) {
                Box(modifier = Modifier.padding(16.dp)) {
                    HunterButton(
                        text = "Exportar JSON (${selectedGroupIds.size} días)",
                        onClick = {
                            val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
                            exportLauncher.launch("GymLog_Historial_$timestamp.json")
                        },
                        enabled = selectedGroupIds.isNotEmpty() && exportState !is ExportState.Loading,
                        color = ScreenColors.Backup.ExportColorButton
                    )
                }
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // BARRA DE HERRAMIENTAS
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(HunterSurface)
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextButton(onClick = viewModel::selectAll) {
                        Text("Marcar Todos", color = HunterPrimary, fontWeight = FontWeight.Bold)
                    }
                    TextButton(onClick = viewModel::deselectAll) {
                        Text("Desmarcar", color = HunterSecondary)
                    }
                }

                IconButton(
                    onClick = viewModel::toggleSortOrder,
                    modifier = Modifier.clip(RoundedCornerShape(8.dp)).background(HunterBlack)
                ) {
                    Icon(
                        imageVector = if (isAscending) Icons.Default.ArrowDownward else Icons.Default.ArrowUpward,
                        contentDescription = "Cambiar Orden",
                        tint = HunterPrimary
                    )
                }
            }

            if (exportGroups.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No hay historial disponible.", color = HunterTextSecondary)
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(exportGroups, key = { it.daySlotId }) { group ->
                        val isSelected = selectedGroupIds.contains(group.daySlotId)

                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { viewModel.toggleSelection(group.daySlotId) },
                            colors = CardDefaults.cardColors(
                                containerColor = if (isSelected) HunterPrimary.copy(alpha = 0.15f) else HunterSurface
                            ),
                            border = BorderStroke(1.dp, if (isSelected) HunterPrimary else HunterPrimary.copy(alpha = 0.2f)),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    if (group.hierarchy != null) {
                                        val monthText = if (group.hierarchy.monthName.isNotBlank()) "${group.hierarchy.monthName} (Mes ${group.hierarchy.monthNumber})" else "Mes ${group.hierarchy.monthNumber}"
                                        Text(
                                            text = "${group.hierarchy.calendarName} / $monthText / Semana ${group.hierarchy.weekNumber}",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = HunterTextSecondary
                                        )

                                        val categoriesText = group.hierarchy.categories.map { stringResource(UiMappers.getDisplayNameRes(it)) }.joinToString(", ")
                                        Text(
                                            text = "${stringResource(UiMappers.getDisplayNameRes(group.hierarchy.dayOfWeek)).uppercase()} - $categoriesText",
                                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                            color = HunterTextPrimary
                                        )
                                    } else {
                                        Text("Día de Entrenamiento", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold), color = HunterTextPrimary)
                                    }

                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text("${group.itemCount} series registradas", style = MaterialTheme.typography.labelSmall, color = HunterPrimary)
                                }

                                Checkbox(
                                    checked = isSelected,
                                    onCheckedChange = null,
                                    colors = CheckboxDefaults.colors(checkedColor = HunterPrimary, uncheckedColor = HunterTextSecondary)
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // Diálogos de Feedback
    when (val state = exportState) {
        is ExportState.Loading -> {
            AlertDialog(
                onDismissRequest = {},
                containerColor = HunterSurface,
                title = { Text(stringResource(R.string.backup_processing_title), color = HunterTextPrimary, fontWeight = FontWeight.Bold) },
                text = { Text("Generando archivo JSON...", color = HunterTextSecondary) },
                confirmButton = { CircularProgressIndicator(color = HunterPrimary) }
            )
        }
        is ExportState.Success -> {
            HunterConfirmDialog(
                title = "Exportación Exitosa",
                text = state.message,
                confirmText = "Aceptar",
                onConfirm = viewModel::dismissMessage,
                onDismiss = {}
            )
        }
        is ExportState.Error -> {
            HunterConfirmDialog(
                title = "Error",
                text = state.message,
                confirmText = "Cerrar",
                onConfirm = viewModel::dismissMessage,
                onDismiss = {}
            )
        }
        ExportState.Idle -> {}
    }
}