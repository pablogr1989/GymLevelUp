package com.gymlog.app.ui.screens.history

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.gymlog.app.ui.theme.*
import com.gymlog.app.ui.util.UiMappers
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun WorkoutHistoryScreen(
    onNavigateToExport: () -> Unit, // RECIBE LA ACCIÓN DE NAVEGACIÓN
    viewModel: WorkoutHistoryViewModel = hiltViewModel()
) {
    val groupedHistory by viewModel.groupedHistory.collectAsState()
    val daySlotHierarchies by viewModel.daySlotHierarchies.collectAsState()
    val selectedIds by viewModel.selectedIds.collectAsState()
    val editingItem by viewModel.editingItem.collectAsState()
    val expandedGroups by viewModel.expandedGroups.collectAsState()

    val isSelectionMode = selectedIds.isNotEmpty()
    val dateFormat = remember { SimpleDateFormat("dd MMM yyyy - HH:mm", Locale.getDefault()) }

    BackHandler(enabled = isSelectionMode) {
        viewModel.clearSelection()
    }

    Scaffold(
        containerColor = HunterBlack,
        topBar = {
            TopAppBar(
                title = {
                    if (isSelectionMode) {
                        Text("${selectedIds.size} seleccionados", color = HunterTextPrimary)
                    } else {
                        Text(
                            "Historial de Entrenamientos",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Black, letterSpacing = 1.sp)
                        )
                    }
                },
                navigationIcon = {
                    if (isSelectionMode) {
                        IconButton(onClick = viewModel::clearSelection) {
                            Icon(Icons.Default.Close, null, tint = HunterTextPrimary)
                        }
                    }
                },
                actions = {
                    if (isSelectionMode) {
                        IconButton(onClick = viewModel::deleteSelected) {
                            Icon(Icons.Default.Delete, "Eliminar", tint = ScreenColors.Global.ErrorRed)
                        }
                    } else {
                        // NUEVO BOTÓN DE EXPORTAR (Estilo MainScreen)
                        IconButton(
                            onClick = onNavigateToExport,
                            modifier = Modifier
                                .padding(end = 8.dp)
                                .clip(CircleShape)
                                .background(HunterSurface)
                        ) {
                            Icon(
                                imageVector = Icons.Default.FileUpload,
                                contentDescription = "Exportar Historial",
                                tint = HunterPrimary
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = HunterBlack, titleContentColor = HunterTextPrimary)
            )
        }
    ) { paddingValues ->
        if (groupedHistory.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize().padding(paddingValues), contentAlignment = Alignment.Center) {
                Text("No hay entrenamientos registrados.", color = HunterTextSecondary)
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(paddingValues),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                groupedHistory.forEach { (daySlotId, items) ->
                    val hierarchy = daySlotHierarchies[daySlotId]
                    val isExpanded = expandedGroups.contains(daySlotId)

                    item {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { viewModel.toggleGroup(daySlotId) }
                                .padding(vertical = 8.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    if (hierarchy != null) {
                                        val monthText = if (hierarchy.monthName.isNotBlank()) "${hierarchy.monthName} (Mes ${hierarchy.monthNumber})" else "Mes ${hierarchy.monthNumber}"
                                        Text(
                                            text = "${hierarchy.calendarName} / $monthText / Semana ${hierarchy.weekNumber}",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = HunterTextSecondary
                                        )

                                        val categoriesText = hierarchy.categories.map { stringResource(UiMappers.getDisplayNameRes(it)) }.joinToString(", ")
                                        Text(
                                            text = "${stringResource(UiMappers.getDisplayNameRes(hierarchy.dayOfWeek)).uppercase()} - $categoriesText",
                                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Black, letterSpacing = 1.sp),
                                            color = HunterPrimary
                                        )
                                    } else {
                                        Text("Día de Entrenamiento", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Black), color = HunterPrimary)
                                    }
                                }

                                Icon(
                                    imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                    contentDescription = if (isExpanded) "Ocultar" else "Mostrar",
                                    tint = HunterPrimary
                                )
                            }
                            Divider(color = HunterPrimary.copy(alpha = 0.3f), modifier = Modifier.padding(top = 12.dp))
                        }
                    }

                    if (isExpanded) {
                        items(items, key = { it.history.id }) { item ->
                            val isSelected = selectedIds.contains(item.history.id)

                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 8.dp)
                                    .combinedClickable(
                                        onClick = {
                                            if (isSelectionMode) viewModel.toggleSelection(item.history.id)
                                            else viewModel.startEditing(item)
                                        },
                                        onLongClick = { viewModel.toggleSelection(item.history.id) }
                                    ),
                                colors = CardDefaults.cardColors(containerColor = if (isSelected) HunterPrimary.copy(alpha = 0.2f) else HunterSurface),
                                border = BorderStroke(1.dp, if (isSelected) HunterPrimary else HunterPrimary.copy(alpha = 0.2f)),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Text(
                                        text = dateFormat.format(Date(item.history.timestamp)),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = HunterTextSecondary.copy(alpha = 0.7f),
                                        modifier = Modifier.padding(bottom = 8.dp)
                                    )

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = item.exerciseName.uppercase(),
                                            style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
                                            color = HunterTextPrimary,
                                            modifier = Modifier.weight(1f),
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = "Serie ${item.history.seriesNumber}",
                                            style = MaterialTheme.typography.labelMedium,
                                            color = HunterPrimary
                                        )
                                    }

                                    Spacer(modifier = Modifier.height(8.dp))

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Column {
                                            val targetReps = if (item.targetMinReps == item.targetMaxReps) "${item.targetMinReps}" else "${item.targetMinReps}-${item.targetMaxReps}"
                                            val targetRir = if (item.targetMinRir != null && item.targetMaxRir != null) {
                                                if (item.targetMinRir == item.targetMaxRir) "RIR ${item.targetMinRir}" else "RIR ${item.targetMinRir}-${item.targetMaxRir}"
                                            } else "Sin RIR"

                                            Text("Objetivo", style = MaterialTheme.typography.labelSmall, color = HunterTextSecondary)
                                            Text("$targetReps reps | $targetRir", style = MaterialTheme.typography.bodySmall, color = HunterTextPrimary)
                                        }

                                        Column(horizontalAlignment = Alignment.End) {
                                            Text("Logrado", style = MaterialTheme.typography.labelSmall, color = HunterTextSecondary)
                                            val rirLogrado = item.history.rir?.let { " | RIR $it" } ?: ""
                                            Text(
                                                "${item.history.reps} reps @ ${item.history.weightKg} kg$rirLogrado",
                                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                                color = HunterPrimary
                                            )
                                        }
                                    }

                                    if (item.history.notes.isNotBlank()) {
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Text(
                                            text = "Obs: ${item.history.notes}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = HunterTextSecondary.copy(alpha = 0.8f)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (editingItem != null) {
        EditHistoryDialog(
            item = editingItem!!,
            onDismiss = viewModel::cancelEditing,
            onSave = viewModel::saveEditedItem,
            onDelete = { viewModel.deleteSingleItem(editingItem!!.history.id) }
        )
    }
}

@Composable
fun EditHistoryDialog(
    item: HistoryItemUi,
    onDismiss: () -> Unit,
    onSave: (Float, Int, Int?, String) -> Unit,
    onDelete: () -> Unit
) {
    var weight by remember { mutableStateOf(item.history.weightKg.toString()) }
    var reps by remember { mutableStateOf(item.history.reps.toString()) }
    var rir by remember { mutableStateOf(item.history.rir?.toString() ?: "") }
    var notes by remember { mutableStateOf(item.history.notes) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = HunterSurface,
        title = { Text("Editar Serie ${item.history.seriesNumber}", color = HunterTextPrimary, fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(item.exerciseName, color = HunterPrimary, style = MaterialTheme.typography.bodyMedium)

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = weight, onValueChange = { weight = it }, label = { Text("Peso") },
                        modifier = Modifier.weight(1f), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = HunterTextPrimary, unfocusedTextColor = HunterTextPrimary, focusedBorderColor = HunterPrimary, unfocusedBorderColor = HunterPrimary.copy(alpha = 0.5f), focusedLabelColor = HunterPrimary, unfocusedLabelColor = HunterTextSecondary)
                    )
                    OutlinedTextField(
                        value = reps, onValueChange = { reps = it }, label = { Text("REPS") },
                        modifier = Modifier.weight(1f), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = HunterTextPrimary, unfocusedTextColor = HunterTextPrimary, focusedBorderColor = HunterPrimary, unfocusedBorderColor = HunterPrimary.copy(alpha = 0.5f), focusedLabelColor = HunterPrimary, unfocusedLabelColor = HunterTextSecondary)
                    )
                }

                OutlinedTextField(
                    value = rir, onValueChange = { rir = it }, label = { Text("RIR") },
                    modifier = Modifier.fillMaxWidth(), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    colors = OutlinedTextFieldDefaults.colors(focusedTextColor = HunterTextPrimary, unfocusedTextColor = HunterTextPrimary, focusedBorderColor = HunterPrimary, unfocusedBorderColor = HunterPrimary.copy(alpha = 0.5f), focusedLabelColor = HunterPrimary, unfocusedLabelColor = HunterTextSecondary)
                )

                OutlinedTextField(
                    value = notes, onValueChange = { notes = it }, label = { Text("Observaciones") },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(focusedTextColor = HunterTextPrimary, unfocusedTextColor = HunterTextPrimary, focusedBorderColor = HunterPrimary, unfocusedBorderColor = HunterPrimary.copy(alpha = 0.5f), focusedLabelColor = HunterPrimary, unfocusedLabelColor = HunterTextSecondary)
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val w = weight.toFloatOrNull() ?: 0f
                    val r = reps.toIntOrNull() ?: 0
                    val ri = rir.toIntOrNull()
                    onSave(w, r, ri, notes)
                },
                colors = ButtonDefaults.buttonColors(containerColor = HunterPrimary)
            ) { Text("Guardar", color = HunterBlack, fontWeight = FontWeight.Bold) }
        },
        dismissButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                IconButton(onClick = onDelete) { Icon(Icons.Default.Delete, "Eliminar", tint = ScreenColors.Global.ErrorRed) }
                TextButton(onClick = onDismiss) { Text("Cancelar", color = HunterTextSecondary) }
            }
        }
    )
}