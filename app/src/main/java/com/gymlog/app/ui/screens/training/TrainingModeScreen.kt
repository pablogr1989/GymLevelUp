package com.gymlog.app.ui.screens.training

import androidx.activity.compose.BackHandler
import androidx.annotation.DrawableRes
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.gymlog.app.data.local.entity.MuscleGroup
import com.gymlog.app.domain.model.Exercise
import com.gymlog.app.domain.model.Set
import com.gymlog.app.ui.theme.*
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

    BackHandler(enabled = uiState.isTrainingActive) {
        viewModel.onBackPressed()
    }

    if (uiState.showExitConfirmation) {
        HunterConfirmDialog(
            title = "PAUSAR MISIÓN",
            text = "El entrenamiento está activo. Si sales ahora, perderás el estado actual. ¿Retirarse?",
            confirmText = "RETIRARSE",
            onConfirm = {
                viewModel.confirmExit()
                onNavigateBack()
            },
            onDismiss = viewModel::dismissExitConfirmation
        )
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "ZONA DE COMBATE",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Black,
                            letterSpacing = 2.sp,
                            color = if (uiState.isTimerRunning) HunterPurple else Color.White
                        )
                    )
                },
                navigationIcon = {
                    IconButton(onClick = {
                        if (uiState.isTrainingActive) viewModel.onBackPressed() else onNavigateBack()
                    }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Volver", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
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
            // 1. RESUMEN DE MISIÓN
            HunterObjectiveCard(
                exercises = uiState.exercises,
                currentIndex = uiState.currentExerciseIndex,
                isTrainingActive = uiState.isTrainingActive
            )

            // 2. CONFIGURACIÓN DESCANSO (Solo si inactivo)
            if (!uiState.isTrainingActive) {
                HunterRestConfig(
                    minutes = uiState.restMinutes,
                    onMinutesChange = viewModel::updateRestMinutes
                )
            }

            // 3. BOTÓN MAESTRO
            HunterMainButton(
                isActive = uiState.isTrainingActive,
                hasExercises = uiState.exercises.isNotEmpty(),
                onStart = viewModel::startTraining,
                onEnd = viewModel::endTraining
            )

            // 4. BARRA DE PROGRESO
            if (uiState.isTrainingActive) {
                HunterMissionProgress(uiState)
            }

            // 5. ALERTA DE ALARMA
            if (uiState.isAlarmRinging) {
                HunterAlarmButton(onStop = viewModel::stopAlarm)
            }

            // 6. HUD ACTIVO
            val currentExercise = uiState.exercises.getOrNull(uiState.currentExerciseIndex)
            val activeSet = currentExercise?.sets?.getOrNull(uiState.activeSetIndex)

            if (uiState.isTrainingActive && currentExercise != null) {
                HunterActiveExerciseHUD(
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
                Box(
                    modifier = Modifier.fillMaxWidth().padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "SISTEMA EN ESPERA...",
                        style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold, letterSpacing = 1.sp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                    )
                }
            } else if (uiState.exercises.isEmpty()) {
                Text(
                    text = "NO HAY MISIONES ASIGNADAS",
                    style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                )
            }
        }

        // Diálogos
        if (showFinishSeriesDialog) {
            HunterConfirmDialog(
                title = "OBJETIVO CUMPLIDO",
                text = "Has completado todas las series requeridas. ¿Finalizar ejercicio?",
                confirmText = "FINALIZAR",
                onConfirm = {
                    viewModel.confirmFinishExercise()
                    showFinishSeriesDialog = false
                },
                onDismiss = { showFinishSeriesDialog = false }
            )
        }

        if (showFinishExerciseDialog) {
            HunterConfirmDialog(
                title = "¿FORZAR FINALIZACIÓN?",
                text = "Aún quedan series pendientes. ¿Confirmas terminar manualmente?",
                confirmText = "FORZAR",
                onConfirm = {
                    viewModel.finishExerciseManually()
                    showFinishExerciseDialog = false
                },
                onDismiss = { showFinishExerciseDialog = false }
            )
        }
    }
}

// ============ COMPONENTES HUNTER HUD ============

@Composable
private fun HunterMissionProgress(state: TrainingUiState) {
    val totalSeries = remember(state.exercises, state.selectedSetIds) {
        state.exercises.zip(state.selectedSetIds).sumOf { (ex, setId) ->
            val set = if (setId != null) ex.sets.find { it.id == setId } else ex.sets.firstOrNull()
            set?.series ?: 0
        }
    }

    val currentProgress = remember(state.currentExerciseIndex, state.currentSeries, state.exercises, state.selectedSetIds) {
        var progress = 0
        for (i in 0 until state.currentExerciseIndex) {
            val ex = state.exercises.getOrNull(i)
            val setId = state.selectedSetIds.getOrNull(i)
            val set = if (setId != null) ex?.sets?.find { it.id == setId } else ex?.sets?.firstOrNull()
            progress += set?.series ?: 0
        }
        progress += (state.currentSeries - 1).coerceAtLeast(0)
        progress
    }

    val progressFraction = if (totalSeries > 0) currentProgress.toFloat() / totalSeries.toFloat() else 0f
    val animatedProgress by animateFloatAsState(targetValue = progressFraction, label = "progress")

    Column(modifier = Modifier.fillMaxWidth()) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("PROGRESO DE MISIÓN", style = MaterialTheme.typography.labelSmall, color = HunterPurple)
            Text("${(progressFraction * 100).toInt()}%", style = MaterialTheme.typography.labelSmall, color = Color.White)
        }
        Spacer(modifier = Modifier.height(4.dp))
        LinearProgressIndicator(
            progress = { animatedProgress },
            modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(4.dp)),
            color = HunterPurple,
            trackColor = MaterialTheme.colorScheme.surfaceVariant
        )
    }
}

@Composable
private fun HunterObjectiveCard(
    exercises: List<Exercise>,
    currentIndex: Int,
    isTrainingActive: Boolean
) {
    HunterCard {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(
                "OBJETIVOS DE MISIÓN",
                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold, letterSpacing = 1.sp),
                color = HunterPurple
            )

            if (exercises.isEmpty()) {
                Text("Sin objetivos.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            } else {
                exercises.forEachIndexed { index, exercise ->
                    val isCurrent = isTrainingActive && index == currentIndex
                    val isDone = isTrainingActive && index < currentIndex

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                if (isCurrent) HunterPurple.copy(alpha = 0.1f) else Color.Transparent,
                                RoundedCornerShape(8.dp)
                            )
                            .padding(8.dp)
                    ) {
                        // Indicador de estado: Morado si actual, Cian si hecho
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .background(
                                    when {
                                        isCurrent -> HunterPurple
                                        isDone -> HunterCyan // Azul Cian para completado
                                        else -> MaterialTheme.colorScheme.onSurfaceVariant
                                    },
                                    CircleShape
                                )
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = exercise.name.uppercase(),
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontWeight = if (isCurrent) FontWeight.Black else FontWeight.Normal
                            ),
                            color = if (isDone) MaterialTheme.colorScheme.onSurfaceVariant else Color.White,
                            modifier = Modifier.weight(1f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        if (isDone) {
                            Icon(Icons.Default.Check, null, tint = HunterCyan, modifier = Modifier.size(16.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun HunterRestConfig(minutes: Int, onMinutesChange: (Int) -> Unit) {
    HunterCard {
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("TIEMPO DE DESCANSO BASE", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)

            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilledIconButton(
                    onClick = { onMinutesChange(minutes - 1) },
                    enabled = minutes > 1,
                    colors = IconButtonDefaults.filledIconButtonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    modifier = Modifier.size(32.dp)
                ) { Icon(Icons.Default.Remove, null, modifier = Modifier.size(16.dp)) }

                Text(
                    text = "$minutes MIN",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, letterSpacing = 1.sp),
                    color = HunterPurple
                )

                FilledIconButton(
                    onClick = { onMinutesChange(minutes + 1) },
                    enabled = minutes < 99,
                    colors = IconButtonDefaults.filledIconButtonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    modifier = Modifier.size(32.dp)
                ) { Icon(Icons.Default.Add, null, modifier = Modifier.size(16.dp)) }
            }
        }
    }
}

@Composable
private fun HunterMainButton(isActive: Boolean, onStart: () -> Unit, onEnd: () -> Unit, hasExercises: Boolean) {
    if (hasExercises) {
        HunterButton(
            text = if (isActive) "FINALIZAR MISIÓN" else "INICIAR MISIÓN",
            onClick = { if (isActive) onEnd() else onStart() },
            color = if (isActive) HunterPurple else MaterialTheme.colorScheme.primary,
            textColor = Color.White,
            icon = {
                Icon(
                    imageVector = if (isActive) Icons.Default.Flag else Icons.Default.PlayArrow,
                    contentDescription = null,
                    tint = Color.White
                )
            }
        )
    }
}

@Composable
private fun HunterAlarmButton(onStop: () -> Unit) {
    Button(
        onClick = onStop,
        modifier = Modifier.fillMaxWidth().height(64.dp),
        colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
        shape = RoundedCornerShape(12.dp),
        contentPadding = PaddingValues(0.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Brush.horizontalGradient(listOf(HunterPurple, HunterPrimary)))
                .padding(horizontal = 16.dp),
            contentAlignment = Alignment.Center
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.NotificationsOff, null, modifier = Modifier.size(28.dp), tint = Color.White)
                Spacer(modifier = Modifier.width(12.dp))
                Text("DETENER ALARMA", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Black, letterSpacing = 2.sp), color = Color.White)
            }
        }
    }
}

@Composable
private fun HunterActiveExerciseHUD(
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
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, HunterPurple.copy(alpha = 0.5f)),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {

            // CABECERA: Título + Imagen
            if (exercise.imageUri != null) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = exercise.name.uppercase(),
                        style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Black),
                        color = Color.White
                    )
                    Text(
                        text = exercise.muscleGroup.displayName.uppercase(),
                        style = MaterialTheme.typography.labelMedium,
                        color = HunterPurple
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .border(1.dp, HunterPurple.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                    ) {
                        AsyncImage(
                            model = exercise.imageUri,
                            contentDescription = null,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    }
                }
            } else {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    HunterTrainingExerciseImage(exercise)
                    Spacer(modifier = Modifier.width(16.dp))
                    Column {
                        Text(
                            text = exercise.name.uppercase(),
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Black, lineHeight = 24.sp),
                            color = Color.White,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = exercise.muscleGroup.displayName.uppercase(),
                            style = MaterialTheme.typography.labelMedium,
                            color = HunterPurple
                        )
                    }
                }
            }

            Divider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))

            // SETS: Activo
            activeSet?.let { set ->
                HunterActiveSetRow(
                    set = set,
                    currentWeight = currentWeight,
                    onWeightChange = onWeightChange
                )
            }

            // OTRAS VARIANTES (Si hay más de 1)
            if (allSets.size > 1) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.background),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("OTRAS VARIANTES", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        allSets.forEachIndexed { index, set ->
                            if (index != activeSetIndex) {
                                HunterInactiveSetRow(index + 1, set)
                            }
                        }
                    }
                }
            }

            // NOTAS (Expandidas)
            OutlinedTextField(
                value = currentNotes,
                onValueChange = onNotesChange,
                label = { Text("NOTAS TÁCTICAS", style = MaterialTheme.typography.labelSmall) },
                modifier = Modifier.fillMaxWidth(),
                minLines = 3,
                maxLines = 6,
                colors = HunterInputColors(),
                shape = RoundedCornerShape(12.dp)
            )

            // PANEL DE CONTROL DE SERIE
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.background, RoundedCornerShape(12.dp))
                    .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f), RoundedCornerShape(12.dp))
                    .padding(12.dp)
            ) {
                Column {
                    Text("SERIE ACTUAL", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(
                        text = "$currentSeries / ${activeSet?.series ?: "?"}",
                        style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Black, color = Color.White)
                    )
                }

                // BOTÓN DE ACCIÓN PRINCIPAL (Morado/Cian)
                Button(
                    onClick = { if (isSeriesRunning) onStopSeries() else onStartSeries() },
                    colors = ButtonDefaults.buttonColors(
                        // Si está corriendo (para parar) -> Azul/Morado suave. Si hay que iniciar -> Morado fuerte.
                        containerColor = if (isSeriesRunning) MaterialTheme.colorScheme.surfaceVariant else HunterPurple
                    ),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = if (isSeriesRunning) "TERMINAR SERIE" else "INICIAR SERIE",
                        fontWeight = FontWeight.Bold,
                        color = if (isSeriesRunning) Color.White else Color.White
                    )
                }
            }

            // DESCANSO Y TIMER
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Selector Descanso
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    FilledIconButton(
                        onClick = { onRestMinutesChange(restMinutes - 1) },
                        enabled = !isTimerRunning && restMinutes > 1,
                        modifier = Modifier.size(32.dp),
                        colors = IconButtonDefaults.filledIconButtonColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) { Text("-", fontWeight = FontWeight.Bold) }

                    Text(
                        "DESC: $restMinutes MIN",
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold, letterSpacing = 1.sp),
                        color = HunterPurple
                    )

                    FilledIconButton(
                        onClick = { onRestMinutesChange(restMinutes + 1) },
                        enabled = !isTimerRunning && restMinutes < 99,
                        modifier = Modifier.size(32.dp),
                        colors = IconButtonDefaults.filledIconButtonColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) { Text("+", fontWeight = FontWeight.Bold) }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // RELOJ
                HunterDigitalClock(seconds = timerSeconds, isRunning = isTimerRunning)

                Spacer(modifier = Modifier.height(12.dp))

                // Controles Timer (Estilo minimalista para no distraer)
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                    Button(
                        onClick = { if (isTimerRunning) onPauseTimer() else onResumeTimer() },
                        modifier = Modifier.weight(1f),
                        enabled = timerSeconds > 0 && !isSeriesRunning,
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surface),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(if (isTimerRunning) Icons.Default.Pause else Icons.Default.PlayArrow, null, tint = Color.White)
                    }

                    Button(
                        onClick = onRestartTimer,
                        modifier = Modifier.weight(1f),
                        enabled = !isSeriesRunning && !isTimerRunning,
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surface),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(Icons.Default.Refresh, null, tint = Color.White)
                    }

                    Button(
                        onClick = onStopTimer,
                        modifier = Modifier.weight(1f),
                        enabled = timerSeconds > 0 && !isSeriesRunning,
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surface), // Gris oscuro
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(Icons.Default.Stop, null, tint = HunterSecondary) // Icono rojo para stop
                    }
                }
            }

            // SIGUIENTE OBJETIVO (Morado)
            Button(
                onClick = onFinishExercise,
                modifier = Modifier.fillMaxWidth().height(56.dp),
                colors = ButtonDefaults.buttonColors(containerColor = HunterPurple),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("SIGUIENTE OBJETIVO >>", fontWeight = FontWeight.Black, letterSpacing = 1.sp, color = Color.White)
            }
        }
    }
}

@Composable
private fun HunterActiveSetRow(set: Set, currentWeight: Float, onWeightChange: (Float) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(HunterPurple.copy(alpha = 0.1f), RoundedCornerShape(8.dp))
            .border(1.dp, HunterPurple, RoundedCornerShape(8.dp))
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text("ACTIVO", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold), color = HunterPurple)

        Row(horizontalArrangement = Arrangement.spacedBy(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("${set.series}", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold), color = Color.White)
                Text("SERIES", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("${set.reps}", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold), color = Color.White)
                Text("REPS", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }

            OutlinedTextField(
                value = if (currentWeight == 0f) "" else currentWeight.toString(),
                onValueChange = { str ->
                    if (str.isEmpty()) onWeightChange(0f)
                    else str.toFloatOrNull()?.let { onWeightChange(it) }
                },
                label = { Text("KG", style = MaterialTheme.typography.labelSmall) },
                modifier = Modifier.width(100.dp),
                singleLine = true,
                textStyle = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, textAlign = TextAlign.Center),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                colors = HunterInputColors()
            )
        }
    }
}

@Composable
private fun HunterInactiveSetRow(index: Int, set: Set) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text("VARIANTE #$index", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(
            "${set.series} × ${set.reps} @ ${set.weightKg} KG",
            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun HunterDigitalClock(seconds: Int, isRunning: Boolean) {
    val m = seconds / 60
    val s = seconds % 60
    val timeStr = String.format("%02d:%02d", m, s)

    Text(
        text = timeStr,
        style = MaterialTheme.typography.displayLarge.copy(
            fontWeight = FontWeight.Black,
            letterSpacing = 4.sp,
        ),
        color = if (isRunning) HunterPurple else Color.White,
        modifier = Modifier.fillMaxWidth(),
        textAlign = TextAlign.Center
    )
}

@Composable
private fun HunterTrainingExerciseImage(exercise: Exercise) {
    Box(
        modifier = Modifier
            .size(80.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.background)
            .border(1.dp, HunterPurple.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
    ) {
        val iconRes = getGroupIcon(exercise.muscleGroup)

        Box(
            modifier = Modifier
                .align(Alignment.Center)
                .size(60.dp)
                .background(
                    brush = Brush.radialGradient(
                        colors = listOf(HunterPurple.copy(alpha = 0.5f), Color.Transparent)
                    )
                )
        )
        Icon(
            painter = painterResource(id = iconRes),
            contentDescription = null,
            tint = HunterPurple,
            modifier = Modifier.align(Alignment.Center).size(40.dp)
        )
    }
}

@Composable
fun HunterInputColors() = OutlinedTextFieldDefaults.colors(
    focusedContainerColor = MaterialTheme.colorScheme.background,
    unfocusedContainerColor = MaterialTheme.colorScheme.background,
    focusedBorderColor = HunterPurple,
    unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
    focusedLabelColor = HunterPurple,
    unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
    cursorColor = HunterPurple,
    focusedTextColor = Color.White,
    unfocusedTextColor = Color.White
)

@DrawableRes
private fun getGroupIcon(group: MuscleGroup): Int {
    return when (group) {
        MuscleGroup.LEGS -> com.gymlog.app.R.drawable.ic_pierna
        MuscleGroup.GLUTES -> com.gymlog.app.R.drawable.ic_gluteos
        MuscleGroup.BACK -> com.gymlog.app.R.drawable.ic_espalda
        MuscleGroup.CHEST -> com.gymlog.app.R.drawable.ic_torso
        MuscleGroup.BICEPS -> com.gymlog.app.R.drawable.ic_biceps
        MuscleGroup.TRICEPS -> com.gymlog.app.R.drawable.ic_triceps
        MuscleGroup.SHOULDERS -> com.gymlog.app.R.drawable.ic_hombros
    }
}