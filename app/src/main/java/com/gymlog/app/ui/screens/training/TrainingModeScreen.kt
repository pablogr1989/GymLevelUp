package com.gymlog.app.ui.screens.training

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.gymlog.app.domain.model.Exercise

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TrainingModeScreen(
    onNavigateBack: () -> Unit,
    viewModel: TrainingModeViewModel = hiltViewModel()
) {
    val daySlot by viewModel.daySlot.collectAsState()
    val exercises by viewModel.exercises.collectAsState()
    val isTrainingActive by viewModel.isTrainingActive.collectAsState()
    val currentExerciseIndex by viewModel.currentExerciseIndex.collectAsState()
    val currentSeries by viewModel.currentSeries.collectAsState()
    val currentWeight by viewModel.currentWeight.collectAsState()
    val currentNotes by viewModel.currentNotes.collectAsState()
    val timerSeconds by viewModel.timerSeconds.collectAsState()
    val isTimerRunning by viewModel.isTimerRunning.collectAsState()
    val restMinutes by viewModel.restMinutes.collectAsState()
    val isSeriesButtonEnabled by viewModel.isSeriesButtonEnabled.collectAsState()
    val isSeriesRunning by viewModel.isSeriesRunning.collectAsState()
    var showFinishSeriesDialog by remember { mutableStateOf(false) }
    var showFinishExerciseDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Modo Entrenamiento") },
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
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Lista de ejercicios (solo lectura)
            ExerciseListCard(exercises = exercises)

            // Number input para minutos de pausa
            RestMinutesInput(
                minutes = restMinutes,
                onMinutesChange = viewModel::updateRestMinutes,
                enabled = !isTrainingActive
            )

            // Botón principal: Comenzar/Terminar entrenamiento
            MainTrainingButton(
                isTrainingActive = isTrainingActive,
                onStartTraining = viewModel::startTraining,
                onEndTraining = viewModel::endTraining,
                hasExercises = exercises.isNotEmpty()
            )

            // Cuadro ejercicio actual
            CurrentExerciseCard(
                exercise = exercises.getOrNull(currentExerciseIndex),
                currentSeries = currentSeries,
                currentWeight = currentWeight,
                currentNotes = currentNotes,
                timerSeconds = timerSeconds,
                isTimerRunning = isTimerRunning,
                isTrainingActive = isTrainingActive,
                isSeriesButtonEnabled = isSeriesButtonEnabled,
                isSeriesRunning = isSeriesRunning,
                onWeightChange = viewModel::updateWeight,
                onNotesChange = viewModel::updateNotes,
                onStartSeries = viewModel::startSeries,
                onStopSeries = {
                    val action = viewModel.stopSeries()
                    if (action == SeriesAction.CONFIRM_FINISH) {
                        showFinishSeriesDialog = true
                    }
                },
                onPauseTimer = viewModel::pauseTimer,
                onResumeTimer = viewModel::resumeTimer,
                onRestartTimer = viewModel::restartTimer,
                onStopTimer = viewModel::stopTimer,
                onFinishExercise = {
                    showFinishExerciseDialog = true
                }
            )
        }
        // Diálogos de confirmación
        if (showFinishSeriesDialog) {
            ConfirmFinishSeriesDialog(
                onConfirm = {
                    viewModel.confirmFinishExercise()
                    showFinishSeriesDialog = false
                },
                onDismiss = {
                    showFinishSeriesDialog = false
                }
            )
        }

        if (showFinishExerciseDialog) {
            ConfirmFinishExerciseDialog(
                onConfirm = {
                    viewModel.finishExerciseManually()
                    showFinishExerciseDialog = false
                },
                onDismiss = {
                    showFinishExerciseDialog = false
                }
            )
        }
    }
}

// ============ COMPONENTES ============

@Composable
private fun ExerciseListCard(
    exercises: List<Exercise>
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "Ejercicios",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            if (exercises.isEmpty()) {
                Text(
                    text = "No hay ejercicios en este día",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                exercises.forEachIndexed { index, exercise ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "${index + 1}.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "${exercise.name} - ${exercise.currentSeries} x ${exercise.currentReps} - ${exercise.currentWeightKg}kg",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun RestMinutesInput(
    minutes: Int,
    onMinutesChange: (Int) -> Unit,
    enabled: Boolean
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "Pausa entre series - Mínimo 1 máximo 99",
                style = MaterialTheme.typography.bodyMedium
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedButton(
                    onClick = { onMinutesChange(minutes - 1) },
                    enabled = enabled && minutes > 1
                ) {
                    Text("-")
                }

                OutlinedTextField(
                    value = minutes.toString(),
                    onValueChange = {
                        it.toIntOrNull()?.let { value ->
                            if (value in 1..99) {
                                onMinutesChange(value)
                            }
                        }
                    },
                    modifier = Modifier.weight(1f),
                    enabled = enabled,
                    singleLine = true,
                    textStyle = MaterialTheme.typography.bodyLarge.copy(
                        fontWeight = FontWeight.Bold
                    )
                )

                OutlinedButton(
                    onClick = { onMinutesChange(minutes + 1) },
                    enabled = enabled && minutes < 99
                ) {
                    Text("+")
                }

                Text(
                    text = "min",
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }
}

@Composable
private fun MainTrainingButton(
    isTrainingActive: Boolean,
    onStartTraining: () -> Unit,
    onEndTraining: () -> Unit,
    hasExercises: Boolean
) {
    if (hasExercises) {
        Button(
            onClick = {
                if (isTrainingActive) {
                    onEndTraining()
                } else {
                    onStartTraining()
                }
            },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(
                containerColor = if (isTrainingActive) {
                    MaterialTheme.colorScheme.error
                } else {
                    MaterialTheme.colorScheme.primary
                }
            )
        ) {
            Text(
                text = if (isTrainingActive) "Terminar entrenamiento" else "Comenzar entrenamiento",
                style = MaterialTheme.typography.titleMedium
            )
        }
    }
}

@Composable
private fun CurrentExerciseCard(
    exercise: Exercise?,
    currentSeries: Int,
    currentWeight: Float,
    currentNotes: String,
    timerSeconds: Int,
    isTimerRunning: Boolean,
    isTrainingActive: Boolean,
    isSeriesButtonEnabled: Boolean,
    isSeriesRunning: Boolean,
    onWeightChange: (Float) -> Unit,
    onNotesChange: (String) -> Unit,
    onStartSeries: () -> Unit,
    onStopSeries: () -> Unit,
    onPauseTimer: () -> Unit,
    onResumeTimer: () -> Unit,
    onRestartTimer: () -> Unit,
    onStopTimer: () -> Unit,
    onFinishExercise: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (isTrainingActive) {
                MaterialTheme.colorScheme.primaryContainer
            } else {
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            }
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (exercise != null) {
                // Imagen del ejercicio
                /*ExerciseImage(
                    imageUri = exercise.imageUri,
                    enabled = isTrainingActive
                )*/

                AsyncImage(
                    model = exercise.imageUri,
                    contentDescription = exercise.name,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                    contentScale = ContentScale.Crop
                )

                // Nombre del ejercicio
                Text(
                    text = exercise.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )

                // Series, Repeticiones, Peso
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Series (solo lectura)
                    OutlinedTextField(
                        value = exercise.currentSeries.toString(),
                        onValueChange = {},
                        label = { Text("Series") },
                        enabled = false,
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )

                    // Repeticiones (solo lectura)
                    OutlinedTextField(
                        value = exercise.currentReps.toString(),
                        onValueChange = {},
                        label = { Text("Repeticiones") },
                        enabled = false,
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )

                    // Peso (editable)
                    OutlinedTextField(
                        value = currentWeight.toString(),
                        onValueChange = {
                            it.toFloatOrNull()?.let { weight ->
                                onWeightChange(weight)
                            }
                        },
                        label = { Text("Peso") },
                        enabled = isTrainingActive,
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        suffix = { Text("kg") }
                    )
                }

                // Notas ejercicio
                OutlinedTextField(
                    value = currentNotes,
                    onValueChange = onNotesChange,
                    label = { Text("Notas") },
                    enabled = isTrainingActive,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp),
                    maxLines = 4
                )

                // Serie actual y botón Iniciar/Parar Serie
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Serie actual (solo lectura)
                    OutlinedTextField(
                        value = "$currentSeries de ${exercise.currentSeries + 1}",
                        onValueChange = {},
                        label = { Text("Serie actual") },
                        enabled = false,
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )

                    // Botón Iniciar/Parar Serie
                    Button(
                        onClick = {
                            if (isSeriesRunning) {
                                onStopSeries()
                            } else {
                                onStartSeries()
                            }
                        },
                        enabled = isTrainingActive && isSeriesButtonEnabled,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(if (isSeriesRunning) "Parar Serie" else "Iniciar Serie")
                    }
                }

                // Timer
                TimerDisplay(
                    timerSeconds = timerSeconds,
                    isTimerRunning = isTimerRunning,
                    isTrainingActive = isTrainingActive
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = {
                            if (isTimerRunning) {
                                onPauseTimer()
                            } else {
                                onResumeTimer()
                            }
                        },
                        enabled = isTrainingActive && !isSeriesRunning && timerSeconds > 0,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(if (isTimerRunning) "Pausar" else "Reanudar")
                    }

                    Button(
                        onClick = onRestartTimer,
                        enabled = isTrainingActive && !isSeriesRunning && timerSeconds > 0,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Reiniciar")
                    }

                    Button(
                        onClick = onStopTimer,
                        enabled = isTrainingActive && !isSeriesRunning && timerSeconds > 0,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Parar")
                    }
                }

                // Botón Terminar ejercicio
                Button(
                    onClick = onFinishExercise,
                    enabled = isTrainingActive,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.secondary
                    )
                ) {
                    Text("Terminar ejercicio")
                }

            } else {
                Text(
                    text = "No hay ejercicio seleccionado",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun ExerciseImage(
    imageUri: String?,
    enabled: Boolean
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(200.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (enabled) {
                MaterialTheme.colorScheme.surface
            } else {
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
            }
        )
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            if (imageUri != null) {
                // TODO: Cargar imagen real con Coil o similar
                Text(
                    text = "Imagen del ejercicio",
                    style = MaterialTheme.typography.bodyMedium
                )
            } else {
                Text(
                    text = "Placeholder - Sin imagen",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun TimerDisplay(
    timerSeconds: Int,
    isTimerRunning: Boolean,
    isTrainingActive: Boolean
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (isTimerRunning) {
                MaterialTheme.colorScheme.tertiaryContainer
            } else {
                MaterialTheme.colorScheme.surface
            }
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                val minutes = timerSeconds / 60
                val seconds = timerSeconds % 60
                Text(
                    text = String.format("%02d:%02d", minutes, seconds),
                    style = MaterialTheme.typography.displayLarge,
                    fontWeight = FontWeight.Bold,
                    color = if (isTimerRunning) {
                        MaterialTheme.colorScheme.onTertiaryContainer
                    } else {
                        MaterialTheme.colorScheme.onSurface
                    }
                )
            }
        }
    }
}

@Composable
private fun ConfirmFinishSeriesDialog(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Terminar ejercicio") },
        text = { Text("Estás en la última serie. ¿Estás seguro de terminar el ejercicio? Aún puedes modificar el peso o las notas si lo necesitas.") },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text("Sí, terminar")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar")
            }
        }
    )
}

@Composable
private fun ConfirmFinishExerciseDialog(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Terminar ejercicio manualmente") },
        text = { Text("¿Estás seguro de que quieres terminar este ejercicio antes de completar todas las series?") },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text("Sí, terminar")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar")
            }
        }
    )
}