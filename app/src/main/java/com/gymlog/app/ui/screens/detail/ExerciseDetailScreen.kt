package com.gymlog.app.ui.screens.detail

import androidx.compose.foundation.background
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.gymlog.app.domain.model.ExerciseHistory
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExerciseDetailScreen(
    onNavigateBack: () -> Unit,
    onNavigateToEdit: (String) -> Unit,
    viewModel: ExerciseDetailViewModel = hiltViewModel()
) {
    val exercise by viewModel.exercise.collectAsState()
    val history by viewModel.history.collectAsState()
    val series by viewModel.series.collectAsState()
    val reps by viewModel.reps.collectAsState()
    val weight by viewModel.weight.collectAsState()
    val notes by viewModel.notes.collectAsState()
    val showSaveSuccess by viewModel.showSaveSuccess.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val showDeleteHistoryDialog by viewModel.showDeleteHistoryDialog.collectAsState()
    val showDeleteEntryDialog by viewModel.showDeleteEntryDialog.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Detalles del Ejercicio") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, "Volver")
                    }
                },
                actions = {
                    IconButton(onClick = { exercise?.let { onNavigateToEdit(it.id) } }) {
                        Icon(Icons.Default.Edit, "Editar")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimary,
                    actionIconContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        }
    ) { paddingValues ->
        exercise?.let { ex ->
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Imagen y nombre
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        if (ex.imageUri != null) {
                            AsyncImage(
                                model = ex.imageUri,
                                contentDescription = ex.name,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(200.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(MaterialTheme.colorScheme.surfaceVariant),
                                contentScale = ContentScale.Crop
                            )
                        }

                        Text(
                            text = ex.name,
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold
                        )

                        AssistChip(
                            onClick = {},
                            label = { Text(ex.muscleGroup.displayName) }
                        )

                        if (ex.description.isNotEmpty()) {
                            Text(
                                text = ex.description,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                // NOTAS
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = Color(0xFF2C2C2E)
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Description,
                                    contentDescription = null,
                                    tint = Color(0xFF0A84FF)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Notas",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = Color.White
                                )
                            }

                            OutlinedTextField(
                                value = notes,
                                onValueChange = viewModel::updateNotes,
                                placeholder = {
                                    Text(
                                        "Escribe observaciones sobre este ejercicio...",
                                        color = Color.Gray
                                    )
                                },
                                modifier = Modifier.fillMaxWidth(),
                                minLines = 3,
                                maxLines = 6,
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White,
                                    focusedBorderColor = Color(0xFF0A84FF),
                                    unfocusedBorderColor = Color.Gray
                                )
                            )
                        }
                    }
                }

                // VALORES ACTUALES
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = Color(0xFF2C2C2E)
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Text(
                                text = "Valores Actuales",
                                style = MaterialTheme.typography.titleMedium,
                                color = Color.White
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                OutlinedTextField(
                                    value = series,
                                    onValueChange = viewModel::updateSeries,
                                    label = { Text("Series", color = Color.Gray) },
                                    modifier = Modifier.weight(1f),
                                    leadingIcon = {
                                        Icon(Icons.Default.Repeat, null, tint = Color(0xFF0A84FF))
                                    },
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedTextColor = Color.White,
                                        unfocusedTextColor = Color.White
                                    )
                                )

                                OutlinedTextField(
                                    value = reps,
                                    onValueChange = viewModel::updateReps,
                                    label = { Text("Repeticiones", color = Color.Gray) },
                                    modifier = Modifier.weight(1f),
                                    leadingIcon = {
                                        Icon(Icons.Default.Numbers, null, tint = Color(0xFF0A84FF))
                                    },
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedTextColor = Color.White,
                                        unfocusedTextColor = Color.White
                                    )
                                )
                            }

                            OutlinedTextField(
                                value = weight,
                                onValueChange = viewModel::updateWeight,
                                label = { Text("Peso (kg)", color = Color.Gray) },
                                modifier = Modifier.fillMaxWidth(),
                                leadingIcon = {
                                    Icon(Icons.Default.FitnessCenter, null, tint = Color(0xFF0A84FF))
                                },
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White
                                )
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                OutlinedButton(
                                    onClick = viewModel::resetToCurrentValues,
                                    modifier = Modifier.weight(1f),
                                    enabled = !isLoading
                                ) {
                                    Icon(Icons.Default.Close, null)
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Cancelar")
                                }

                                Button(
                                    onClick = viewModel::saveExerciseStats,
                                    modifier = Modifier.weight(1f),
                                    enabled = !isLoading,
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = Color(0xFF0A84FF)
                                    )
                                ) {
                                    if (isLoading) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(20.dp),
                                            color = Color.White,
                                            strokeWidth = 2.dp
                                        )
                                    } else {
                                        Icon(Icons.Default.Check, null)
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Guardar")
                                    }
                                }
                            }
                        }
                    }
                }

                // HISTORIAL
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.History, null, tint = Color(0xFF0A84FF))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                "Historial",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        if (history.isNotEmpty()) {
                            TextButton(
                                onClick = viewModel::showDeleteHistoryDialog,
                                colors = ButtonDefaults.textButtonColors(
                                    contentColor = MaterialTheme.colorScheme.error
                                )
                            ) {
                                Icon(Icons.Default.DeleteSweep, null)
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Borrar Todo")
                            }
                        }
                    }
                }

                items(history) { entry ->
                    HistoryEntryCard(
                        entry = entry,
                        onDelete = { viewModel.showDeleteEntryDialog(entry) }
                    )
                }
            }
        }
    }

    // DiÃ¡logos
    if (showDeleteHistoryDialog) {
        AlertDialog(
            onDismissRequest = viewModel::dismissDeleteHistoryDialog,
            title = { Text("Borrar todo el historial") },
            text = { Text("Â¿EstÃ¡s seguro? Esta acciÃ³n no se puede deshacer.") },
            confirmButton = {
                TextButton(
                    onClick = viewModel::deleteAllHistory,
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("Borrar")
                }
            },
            dismissButton = {
                TextButton(onClick = viewModel::dismissDeleteHistoryDialog) {
                    Text("Cancelar")
                }
            }
        )
    }

    showDeleteEntryDialog?.let { entry ->
        AlertDialog(
            onDismissRequest = viewModel::dismissDeleteEntryDialog,
            title = { Text("Eliminar entrada") },
            text = { Text("Â¿Eliminar esta entrada del historial?") },
            confirmButton = {
                TextButton(
                    onClick = { viewModel.deleteHistoryEntry(entry) },
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("Eliminar")
                }
            },
            dismissButton = {
                TextButton(onClick = viewModel::dismissDeleteEntryDialog) {
                    Text("Cancelar")
                }
            }
        )
    }
}

@Composable
private fun HistoryEntryCard(
    entry: ExerciseHistory,
    onDelete: () -> Unit
) {
    val dateFormat = remember { SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF2C2C2E)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = dateFormat.format(Date(entry.timestamp)),
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "${entry.series} Ã— ${entry.reps} â€” ${entry.weightKg} kg",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White,
                    fontWeight = FontWeight.Medium
                )
            }

            IconButton(onClick = onDelete) {
                Icon(
                    Icons.Default.Delete,
                    "Eliminar",
                    tint = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}