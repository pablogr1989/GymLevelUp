package com.gymlog.app.ui.screens.training

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowRight
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.NotificationsOff
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.gymlog.app.domain.model.Exercise
import com.gymlog.app.domain.model.Set
import com.gymlog.app.util.RequestNotificationPermission

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TrainingModeScreen(
    onNavigateBack: () -> Unit,
    viewModel: TrainingModeViewModel = hiltViewModel()
) {
    RequestNotificationPermission()
    val uiState by viewModel.uiState.collectAsState()

    var showFinishSeriesDialog by remember { mutableStateOf(false) }
    var showFinishExerciseDialog by remember { mutableStateOf(false) }

    // Interceptar botón atrás
    BackHandler(enabled = uiState.isTrainingActive) {
        viewModel.onBackPressed()
    }

    // Diálogo de confirmación de salida
    if (uiState.showExitConfirmation) {
        ConfirmDialog(
            title = "Salir del entrenamiento",
            text = "Tienes un entrenamiento activo. Si sales ahora, se perderá el estado del temporizador. ¿Seguro?",
            confirmText = "Salir",
            onConfirm = {
                viewModel.confirmExit()
                onNavigateBack()
            },
            onDismiss = viewModel::dismissExitConfirmation
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Modo Entrenamiento") },
                navigationIcon = {
                    IconButton(onClick = {
                        if (uiState.isTrainingActive) viewModel.onBackPressed() else onNavigateBack()
                    }) {
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
            // Lista de ejercicios (Resumen)
            ExerciseListCard(
                exercises = uiState.exercises,
                currentIndex = uiState.currentExerciseIndex,
                isTrainingActive = uiState.isTrainingActive
            )

            // Botón Principal (Comenzar/Terminar)
            MainTrainingButton(
                isTrainingActive = uiState.isTrainingActive,
                onStartTraining = viewModel::startTraining,
                onEndTraining = viewModel::endTraining,
                hasExercises = uiState.exercises.isNotEmpty()
            )

            // Botón Parar Alarma (Visible solo si suena)
            if (uiState.isAlarmRinging) {
                Button(
                    onClick = viewModel::stopAlarm,
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.tertiary)
                ) {
                    Icon(Icons.Default.NotificationsOff, null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("DETENER ALARMA", fontWeight = FontWeight.Bold)
                }
            }

            // Tarjeta Ejercicio Activo
            val currentExercise = uiState.exercises.getOrNull(uiState.currentExerciseIndex)
            val activeSet = currentExercise?.sets?.getOrNull(uiState.activeSetIndex)

            if (uiState.isTrainingActive && currentExercise != null) {
                CurrentExerciseCard(
                    exercise = currentExercise,
                    allSets = currentExercise.sets,
                    activeSetIndex = uiState.activeSetIndex,
                    activeSet = activeSet,
                    currentSeries = uiState.currentSeries,
                    currentWeight = uiState.currentWeight,
                    currentNotes = uiState.currentNotes,
                    timerSeconds = uiState.timerSeconds,
                    isTimerRunning = uiState.isTimerRunning,
                    isTrainingActive = uiState.isTrainingActive,
                    isSeriesButtonEnabled = uiState.isSeriesButtonEnabled,
                    isSeriesRunning = uiState.isSeriesRunning,
                    restMinutes = uiState.restMinutes,
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
                    onFinishExercise = { showFinishExerciseDialog = true },
                    onRestMinutesChange = viewModel::updateRestMinutes
                )
            } else if (currentExercise != null) {
                // Vista previa (No activo)
                Text(
                    text = "Pulsa 'Comenzar entrenamiento' para iniciar",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                )
            } else if (uiState.exercises.isEmpty()) {
                Text(
                    text = "No hay ejercicios planificados para hoy.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                )
            }
        }

        // Diálogos de confirmación
        if (showFinishSeriesDialog) {
            ConfirmDialog(
                title = "Terminar ejercicio",
                text = "Estás en la última serie. ¿Deseas terminar este ejercicio?",
                onConfirm = {
                    viewModel.confirmFinishExercise()
                    showFinishSeriesDialog = false
                },
                onDismiss = { showFinishSeriesDialog = false }
            )
        }

        if (showFinishExerciseDialog) {
            ConfirmDialog(
                title = "Terminar manualmente",
                text = "¿Seguro que quieres terminar este ejercicio antes de completar todas las series?",
                onConfirm = {
                    viewModel.finishExerciseManually()
                    showFinishExerciseDialog = false
                },
                onDismiss = { showFinishExerciseDialog = false }
            )
        }
    }
}

// ============ COMPONENTES AUXILIARES ============

@Composable
private fun ConfirmDialog(
    title: String,
    text: String,
    confirmText: String = "Sí, terminar",
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { Text(text) },
        confirmButton = { TextButton(onClick = onConfirm) { Text(confirmText) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } }
    )
}

@Composable
private fun ExerciseListCard(
    exercises: List<Exercise>,
    currentIndex: Int,
    isTrainingActive: Boolean
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Resumen Rutina", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            if (exercises.isEmpty()) {
                Text("No hay ejercicios hoy", style = MaterialTheme.typography.bodyMedium)
            } else {
                exercises.forEachIndexed { index, exercise ->
                    val isCurrent = isTrainingActive && index == currentIndex
                    val isDone = isTrainingActive && index < currentIndex

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (isCurrent) Icon(Icons.Default.ArrowRight, null, tint = MaterialTheme.colorScheme.primary)
                        else if (isDone) Icon(Icons.Default.Check, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                        else Spacer(modifier = Modifier.width(24.dp))

                        Text(
                            text = exercise.name,
                            style = if (isCurrent) MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold) else MaterialTheme.typography.bodyMedium,
                            color = if (isDone) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun MainTrainingButton(isTrainingActive: Boolean, onStartTraining: () -> Unit, onEndTraining: () -> Unit, hasExercises: Boolean) {
    if (hasExercises) {
        Button(
            onClick = { if (isTrainingActive) onEndTraining() else onStartTraining() },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = if (isTrainingActive) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary)
        ) {
            Text(if (isTrainingActive) "Terminar entrenamiento" else "Comenzar entrenamiento")
        }
    }
}

@Composable
private fun RestMinutesInput(minutes: Int, onMinutesChange: (Int) -> Unit, enabled: Boolean) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Tiempo de descanso (min)", style = MaterialTheme.typography.bodyMedium)
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = { onMinutesChange(minutes - 1) }, enabled = enabled && minutes > 1) { Text("-") }
                Text(text = "$minutes min", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                OutlinedButton(onClick = { onMinutesChange(minutes + 1) }, enabled = enabled && minutes < 99) { Text("+") }
            }
        }
    }
}

@Composable
private fun CurrentExerciseCard(
    exercise: Exercise,
    activeSet: Set?,
    allSets: List<Set>,
    activeSetIndex: Int,
    currentSeries: Int,
    currentWeight: Float,
    currentNotes: String,
    timerSeconds: Int,
    isTimerRunning: Boolean,
    isTrainingActive: Boolean,
    isSeriesButtonEnabled: Boolean,
    isSeriesRunning: Boolean,
    restMinutes: Int,
    onWeightChange: (Float) -> Unit,
    onNotesChange: (String) -> Unit,
    onStartSeries: () -> Unit,
    onStopSeries: () -> Unit,
    onPauseTimer: () -> Unit,
    onResumeTimer: () -> Unit,
    onRestartTimer: () -> Unit,
    onStopTimer: () -> Unit,
    onFinishExercise: () -> Unit,
    onRestMinutesChange: (Int) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            // Imagen
            AsyncImage(
                model = exercise.imageUri,
                contentDescription = null,
                modifier = Modifier.fillMaxWidth().height(200.dp).clip(RoundedCornerShape(12.dp)).background(Color.Gray),
                contentScale = ContentScale.Crop
            )

            Text(exercise.name, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)

            // SETS VISUALIZER
            Text("Sets:", style = MaterialTheme.typography.labelLarge)
            allSets.forEachIndexed { index, set ->
                val isActive = index == activeSetIndex

                if (isActive) {
                    // Set Activo (Editable)
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text("Variante Activa", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                                // Series (Objetivo)
                                OutlinedTextField(
                                    value = "${set.series}",
                                    onValueChange = {},
                                    label = { Text("Series") },
                                    enabled = false,
                                    modifier = Modifier.weight(1f)
                                )
                                // Reps (Objetivo)
                                OutlinedTextField(
                                    value = "${set.reps}",
                                    onValueChange = {},
                                    label = { Text("Reps") },
                                    enabled = false,
                                    modifier = Modifier.weight(1f)
                                )
                                // Peso (Editable)
                                OutlinedTextField(
                                    value = if (currentWeight == 0f) "" else currentWeight.toString(),
                                    onValueChange = { str ->
                                        if (str.isEmpty()) onWeightChange(0f)
                                        else str.toFloatOrNull()?.let { onWeightChange(it) }
                                    },
                                    label = { Text("Peso") },
                                    modifier = Modifier.weight(1f),
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                                )
                            }
                        }
                    }
                } else {
                    // Otros Sets (Lectura)
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Variante #${index + 1}", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("${set.series} x ${set.reps} @ ${set.weightKg}kg", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                    }
                }
            }

            Divider()

            // NOTAS
            OutlinedTextField(
                value = currentNotes,
                onValueChange = onNotesChange,
                label = { Text("Notas") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 2
            )

            // CONTROLES SERIE (Movido encima de Timer)
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = "$currentSeries / ${activeSet?.series ?: "?"}",
                    onValueChange = {},
                    label = { Text("Serie Actual") },
                    enabled = false,
                    modifier = Modifier.weight(1f)
                )
                Button(
                    onClick = { if (isSeriesRunning) onStopSeries() else onStartSeries() },
                    modifier = Modifier.weight(1f),
                    enabled = isSeriesButtonEnabled
                ) {
                    Text(if (isSeriesRunning) "Terminar Serie" else "Iniciar Serie")
                }
            }

            // CONTROLES DE DESCANSO (Movidos aquí, encima del timer)
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Descanso:", style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(end = 4.dp))
                OutlinedButton(
                    onClick = { onRestMinutesChange(restMinutes - 1) },
                    enabled = !isTimerRunning && restMinutes > 1,
                    modifier = Modifier.size(40.dp),
                    contentPadding = PaddingValues(0.dp)
                ) { Text("-") }

                Text(
                    text = "$restMinutes min",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 4.dp)
                )

                OutlinedButton(
                    onClick = { onRestMinutesChange(restMinutes + 1) },
                    enabled = !isTimerRunning && restMinutes < 99,
                    modifier = Modifier.size(40.dp),
                    contentPadding = PaddingValues(0.dp)
                ) { Text("+") }
            }

            // TIMER
            TimerDisplay(seconds = timerSeconds, isRunning = isTimerRunning)

            // BOTONES TIMER (Pausar, Reiniciar, Parar)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = { if (isTimerRunning) onPauseTimer() else onResumeTimer() },
                    modifier = Modifier.weight(1f),
                    enabled = timerSeconds > 0 && !isSeriesRunning
                ) {
                    Text(if (isTimerRunning) "Pausar" else "Reanudar")
                }

                // Botón Reiniciar
                FilledTonalButton(
                    onClick = onRestartTimer,
                    modifier = Modifier.weight(0.7f),
                    enabled = !isSeriesRunning && !isTimerRunning
                ) {
                    Icon(Icons.Default.Refresh, contentDescription = "Reiniciar")
                }

                Button(
                    onClick = onStopTimer,
                    modifier = Modifier.weight(1f),
                    enabled = timerSeconds > 0 && !isSeriesRunning
                ) {
                    Text("Parar")
                }
            }

            Button(onClick = onFinishExercise, modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)) {
                Text("Siguiente Ejercicio")
            }
        }
    }
}

@Composable
fun TimerDisplay(seconds: Int, isRunning: Boolean) {
    val m = seconds / 60
    val s = seconds % 60
    Text(
        text = String.format("%02d:%02d", m, s),
        style = MaterialTheme.typography.displayLarge,
        fontWeight = FontWeight.Bold,
        color = if (isRunning) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
        modifier = Modifier.fillMaxWidth(),
        textAlign = androidx.compose.ui.text.style.TextAlign.Center
    )
}