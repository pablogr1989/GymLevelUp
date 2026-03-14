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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.gymlog.app.R
import com.gymlog.app.data.local.entity.MuscleGroup
import com.gymlog.app.domain.model.Exercise
import com.gymlog.app.domain.model.Set
import com.gymlog.app.ui.theme.*
import com.gymlog.app.ui.util.UiMappers
import com.gymlog.app.util.RequestNotificationPermission

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TrainingModeScreen(
    onNavigateBack: () -> Unit,
    viewModel: TrainingModeViewModel = hiltViewModel()
) {
    RequestNotificationPermission()
    val uiState by viewModel.uiState.collectAsState()

    var showForceFinishDialog by remember { mutableStateOf(false) }

    BackHandler(enabled = uiState.isTrainingActive) {
        viewModel.onBackPressed()
    }

    if (uiState.showExitConfirmation) {
        HunterConfirmDialog(
            title = "Salir del Entrenamiento",
            text = "¿Estás seguro? Perderás el progreso de la sesión actual.",
            confirmText = "Salir",
            onConfirm = { viewModel.confirmExit(); onNavigateBack() },
            onDismiss = viewModel::dismissExitConfirmation
        )
    }

    if (uiState.showWeightChangeConfirmation) {
        HunterConfirmDialog(
            title = "Peso Modificado",
            text = "Has modificado el peso. Al confirmar, este será el nuevo peso de la plantilla para futuros entrenamientos.\n\n¿Deseas sobreescribir el peso?",
            confirmText = "Guardar y Continuar",
            onConfirm = viewModel::confirmWeightChangeAndStopSeries,
            onDismiss = viewModel::cancelWeightChange
        )
    }

    if (uiState.showFinishExercisePrompt) {
        HunterConfirmDialog(
            title = "Variantes Completadas",
            text = "Has completado todas las variantes planificadas para este ejercicio. ¿Avanzar al siguiente?",
            confirmText = "Siguiente Ejercicio",
            onConfirm = viewModel::confirmFinishExercise,
            onDismiss = viewModel::dismissFinishExercise
        )
    }

    if (showForceFinishDialog) {
        HunterConfirmDialog(
            title = "Forzar Fin de Ejercicio",
            text = "¿Deseas finalizar este ejercicio prematuramente y avanzar al siguiente?",
            confirmText = "Finalizar Ejercicio",
            onConfirm = { viewModel.finishExerciseManually(); showForceFinishDialog = false },
            onDismiss = { showForceFinishDialog = false }
        )
    }

    Scaffold(
        containerColor = HunterBlack,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        stringResource(R.string.training_title),
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Black, letterSpacing = 2.sp, color = if (uiState.isTimerRunning) ScreenColors.TrainingMode.TimerRunningText else ScreenColors.TrainingMode.TitleTextDefault)
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { if (uiState.isTrainingActive) viewModel.onBackPressed() else onNavigateBack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = stringResource(R.string.common_back), tint = ScreenColors.TrainingMode.TopBarIcon)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = HunterBlack)
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
            HunterObjectiveCard(blocks = uiState.blocks, currentIndex = uiState.currentBlockIndex, isTrainingActive = uiState.isTrainingActive)

            if (!uiState.isTrainingActive) {
                HunterRestConfig(minutes = uiState.restMinutes, onMinutesChange = viewModel::updateRestMinutes)
            }

            HunterMainButton(isActive = uiState.isTrainingActive, hasExercises = uiState.blocks.isNotEmpty(), onStart = viewModel::startTraining, onEnd = viewModel::endTraining)

            if (uiState.isTrainingActive) {
                HunterMissionProgress(uiState)
            }

            if (uiState.isAlarmRinging) {
                HunterAlarmButton(onStop = viewModel::stopAlarm)
            }

            val currentBlock = uiState.blocks.getOrNull(uiState.currentBlockIndex)

            if (uiState.isTrainingActive && currentBlock != null) {
                HunterActiveExerciseHUD(
                    exercise = currentBlock.exercise,
                    allSets = currentBlock.sets,
                    activeSetIndex = uiState.currentSetIndex,
                    currentSeries = uiState.currentSeries,

                    currentWeight = uiState.currentWeight,
                    actualReps = uiState.actualReps,
                    actualRir = uiState.actualRir,
                    currentNotes = uiState.currentNotes,
                    currentObservations = uiState.currentObservations,

                    timerSeconds = uiState.timerSeconds,
                    isTimerRunning = uiState.isTimerRunning,
                    isSeriesButtonEnabled = uiState.isSeriesButtonEnabled,
                    isSeriesRunning = uiState.isSeriesRunning,
                    isAlarmRinging = uiState.isAlarmRinging,
                    restMinutes = uiState.restMinutes,

                    onWeightChange = viewModel::updateWeight,
                    onNotesChange = viewModel::updateNotes,
                    onActualRepsChange = viewModel::updateActualReps,
                    onActualRirChange = viewModel::updateActualRir,
                    onObservationsChange = viewModel::updateObservations,

                    onStartSeries = viewModel::startSeries,
                    onStopSeries = viewModel::requestStopSeries,

                    onPauseTimer = viewModel::pauseTimer,
                    onResumeTimer = viewModel::resumeTimer,
                    onRestartTimer = viewModel::restartTimer,
                    onStopTimer = viewModel::stopTimer,
                    onStopAlarm = viewModel::stopAlarm,
                    onFinishExercise = { showForceFinishDialog = true },
                    onRestMinutesChange = viewModel::updateRestMinutes
                )
            } else if (currentBlock != null) {
                Box(modifier = Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
                    Text("Esperando para iniciar...", style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold, letterSpacing = 1.sp), color = HunterTextSecondary.copy(alpha = 0.5f))
                }
            }
        }
    }
}

// ============ COMPONENTES HUNTER HUD ============

@Composable
private fun HunterMissionProgress(state: TrainingUiState) {
    val totalSeries = remember(state.blocks) { state.blocks.sumOf { block -> block.sets.sumOf { it.series } } }
    val currentProgress = remember(state.currentBlockIndex, state.currentSetIndex, state.currentSeries, state.blocks) {
        var progress = 0
        for (i in 0 until state.currentBlockIndex) { progress += state.blocks[i].sets.sumOf { it.series } }
        val currentBlock = state.blocks.getOrNull(state.currentBlockIndex)
        if (currentBlock != null) {
            for (j in 0 until state.currentSetIndex) { progress += currentBlock.sets[j].series }
        }
        progress += (state.currentSeries - 1).coerceAtLeast(0)
        progress
    }
    val progressFraction = if (totalSeries > 0) currentProgress.toFloat() / totalSeries.toFloat() else 0f
    val animatedProgress by animateFloatAsState(targetValue = progressFraction, label = "progress")

    Column(modifier = Modifier.fillMaxWidth()) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(stringResource(R.string.training_progress_label), style = MaterialTheme.typography.labelSmall, color = ScreenColors.TrainingMode.ProgressLabel)
            Text("${(progressFraction * 100).toInt()}%", style = MaterialTheme.typography.labelSmall, color = ScreenColors.TrainingMode.ProgressValue)
        }
        Spacer(modifier = Modifier.height(4.dp))
        LinearProgressIndicator(progress = { animatedProgress }, modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(4.dp)), color = ScreenColors.TrainingMode.ProgressIndicator, trackColor = ScreenColors.TrainingMode.ProgressTrack)
    }
}

@Composable
private fun HunterObjectiveCard(blocks: List<TrainingBlock>, currentIndex: Int, isTrainingActive: Boolean) {
    HunterCard {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Ejercicios a completar", style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold, letterSpacing = 1.sp), color = ScreenColors.TrainingMode.ObjectiveTitle)
            if (blocks.isEmpty()) {
                Text("No hay ejercicios.", style = MaterialTheme.typography.bodyMedium, color = ScreenColors.TrainingMode.ObjectiveEmptyText)
            } else {
                blocks.forEachIndexed { index, block ->
                    val isCurrent = isTrainingActive && index == currentIndex
                    val isDone = isTrainingActive && index < currentIndex
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth().background(if (isCurrent) ScreenColors.TrainingMode.ObjectiveItemBgCurrent else ScreenColors.TrainingMode.ObjectiveItemBgDefault, RoundedCornerShape(8.dp)).padding(8.dp)
                    ) {
                        Box(modifier = Modifier.size(8.dp).background(when { isCurrent -> ScreenColors.TrainingMode.ObjectiveDotCurrent; isDone -> ScreenColors.TrainingMode.ObjectiveDotDone; else -> ScreenColors.TrainingMode.ObjectiveDotPending }, CircleShape))
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(text = block.exercise.name.uppercase(), style = MaterialTheme.typography.bodyMedium.copy(fontWeight = if (isCurrent) FontWeight.Black else FontWeight.Normal), color = if (isDone) ScreenColors.TrainingMode.ObjectiveTextDone else ScreenColors.TrainingMode.ObjectiveTextDefault, modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                        if (isDone) { Icon(Icons.Default.Check, null, tint = ScreenColors.TrainingMode.ObjectiveCheckIcon, modifier = Modifier.size(16.dp)) }
                    }
                }
            }
        }
    }
}

@Composable
private fun HunterRestConfig(minutes: Int, onMinutesChange: (Int) -> Unit) {
    HunterCard {
        Row(modifier = Modifier.padding(16.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text(stringResource(R.string.training_rest_config_title), style = MaterialTheme.typography.labelMedium, color = ScreenColors.TrainingMode.RestConfigLabel)
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilledIconButton(onClick = { onMinutesChange(minutes - 1) }, enabled = minutes > 1, colors = IconButtonDefaults.filledIconButtonColors(containerColor = ScreenColors.TrainingMode.RestConfigBtnBg), modifier = Modifier.size(32.dp)) { Icon(Icons.Default.Remove, null, modifier = Modifier.size(16.dp), tint = ScreenColors.TrainingMode.RestConfigBtnContent) }
                Text("$minutes ${stringResource(R.string.training_min_suffix)}", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, letterSpacing = 1.sp), color = ScreenColors.TrainingMode.RestConfigValue)
                FilledIconButton(onClick = { onMinutesChange(minutes + 1) }, enabled = minutes < 99, colors = IconButtonDefaults.filledIconButtonColors(containerColor = ScreenColors.TrainingMode.RestConfigBtnBg), modifier = Modifier.size(32.dp)) { Icon(Icons.Default.Add, null, modifier = Modifier.size(16.dp), tint = ScreenColors.TrainingMode.RestConfigBtnContent) }
            }
        }
    }
}

@Composable
private fun HunterMainButton(isActive: Boolean, onStart: () -> Unit, onEnd: () -> Unit, hasExercises: Boolean) {
    if (hasExercises) {
        HunterButton(
            text = if (isActive) stringResource(R.string.training_btn_finish) else stringResource(R.string.training_btn_start),
            onClick = { if (isActive) onEnd() else onStart() },
            color = if (isActive) ScreenColors.TrainingMode.MainBtnActiveBg else ScreenColors.TrainingMode.MainBtnDefaultBg,
            textColor = ScreenColors.TrainingMode.MainBtnText,
            icon = { Icon(if (isActive) Icons.Default.Flag else Icons.Default.PlayArrow, null, tint = ScreenColors.TrainingMode.MainBtnText) }
        )
    }
}

@Composable
private fun HunterAlarmButton(onStop: () -> Unit) {
    Button(
        onClick = onStop, modifier = Modifier.fillMaxWidth().height(64.dp), colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent), shape = RoundedCornerShape(12.dp), contentPadding = PaddingValues(0.dp)
    ) {
        Box(modifier = Modifier.fillMaxSize().background(Brush.horizontalGradient(listOf(ScreenColors.TrainingMode.AlarmBtnGradientStart, ScreenColors.TrainingMode.AlarmBtnGradientEnd))).padding(horizontal = 16.dp), contentAlignment = Alignment.Center) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.NotificationsOff, null, modifier = Modifier.size(28.dp), tint = ScreenColors.TrainingMode.AlarmBtnContent)
                Spacer(modifier = Modifier.width(12.dp))
                Text(stringResource(R.string.training_btn_stop_alarm), style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Black, letterSpacing = 2.sp), color = ScreenColors.TrainingMode.AlarmBtnContent)
            }
        }
    }
}

@Composable
private fun HunterActiveExerciseHUD(
    exercise: Exercise,
    allSets: List<Set>,
    activeSetIndex: Int,
    currentSeries: Int,

    currentWeight: Float,
    actualReps: String,
    actualRir: String,
    currentNotes: String,
    currentObservations: String,

    timerSeconds: Int,
    isTimerRunning: Boolean,
    isSeriesButtonEnabled: Boolean,
    isSeriesRunning: Boolean,
    isAlarmRinging: Boolean,
    restMinutes: Int,

    onWeightChange: (Float) -> Unit,
    onNotesChange: (String) -> Unit,
    onActualRepsChange: (String) -> Unit,
    onActualRirChange: (String) -> Unit,
    onObservationsChange: (String) -> Unit,

    onStartSeries: () -> Unit,
    onStopSeries: () -> Unit,

    onPauseTimer: () -> Unit,
    onResumeTimer: () -> Unit,
    onRestartTimer: () -> Unit,
    onStopTimer: () -> Unit,
    onStopAlarm: () -> Unit,
    onFinishExercise: () -> Unit,
    onRestMinutesChange: (Int) -> Unit
) {
    val activeSet = allSets.getOrNull(activeSetIndex)

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = ScreenColors.TrainingMode.HudCardBg),
        border = BorderStroke(1.dp, ScreenColors.TrainingMode.HudCardBorder),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {

            // --- SECCIÓN: INFO DEL EJERCICIO (Lectura + Notas Ejercicio) ---
            if (exercise.imageUri != null) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(text = exercise.name.uppercase(), style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Black), color = ScreenColors.TrainingMode.HudTitle)
                    Text(text = stringResource(UiMappers.getDisplayNameRes(exercise.muscleGroup)).uppercase(), style = MaterialTheme.typography.labelMedium, color = ScreenColors.TrainingMode.HudMuscleGroup)
                    Spacer(modifier = Modifier.height(12.dp))
                    Box(modifier = Modifier.fillMaxWidth().height(200.dp).clip(RoundedCornerShape(12.dp)).border(1.dp, ScreenColors.TrainingMode.HudImageBorder, RoundedCornerShape(12.dp))) {
                        AsyncImage(model = exercise.imageUri, contentDescription = null, modifier = Modifier.fillMaxSize().background(Color.White), contentScale = ContentScale.Fit)
                    }
                }
            } else {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    HunterTrainingExerciseImage(exercise)
                    Spacer(modifier = Modifier.width(16.dp))
                    Column {
                        Text(text = exercise.name.uppercase(), style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Black, lineHeight = 24.sp), color = ScreenColors.TrainingMode.HudTitle, maxLines = 2, overflow = TextOverflow.Ellipsis)
                        Text(text = stringResource(UiMappers.getDisplayNameRes(exercise.muscleGroup)).uppercase(), style = MaterialTheme.typography.labelMedium, color = ScreenColors.TrainingMode.HudMuscleGroup)
                    }
                }
            }

            // Variantes de lectura (Contexto)
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("Variantes Planificadas:", style = MaterialTheme.typography.labelSmall, color = HunterTextSecondary)
                allSets.forEachIndexed { index, set ->
                    val isDone = index < activeSetIndex
                    val isCurrent = index == activeSetIndex
                    val repsText = if (set.minReps == set.maxReps) "${set.minReps}" else "${set.minReps}-${set.maxReps}"
                    val rirText = if (set.minRir != null && set.maxRir != null) { if (set.minRir == set.maxRir) " | RIR ${set.minRir}" else " | RIR ${set.minRir}-${set.maxRir}" } else ""

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = if (isDone) Icons.Default.CheckCircle else if (isCurrent) Icons.Default.PlayArrow else Icons.Default.Circle,
                            contentDescription = null,
                            tint = if (isDone) HunterPrimary.copy(alpha = 0.5f) else if (isCurrent) HunterPrimary else HunterTextSecondary.copy(alpha = 0.3f),
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "${set.series} × $repsText @ ${set.weightKg} kg$rirText",
                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Normal),
                            color = if (isCurrent) HunterPrimary else HunterTextSecondary
                        )
                    }
                }
            }

            // Notas Generales del Ejercicio (Bloqueadas si la serie no está iniciada)
            OutlinedTextField(
                value = currentNotes,
                onValueChange = onNotesChange,
                label = { Text("Notas del Ejercicio (Global)", style = MaterialTheme.typography.labelSmall) },
                modifier = Modifier.fillMaxWidth(),
                minLines = 2,
                maxLines = 4,
                enabled = isSeriesRunning, // Solo editable al iniciar serie
                colors = TrainingInputColors(),
                shape = RoundedCornerShape(12.dp)
            )

            Divider(color = ScreenColors.TrainingMode.HudDivider)

            // --- SECCIÓN: SERIE ACTIVA (Registro y Controles) ---
            if (activeSet != null) {
                Column(
                    modifier = Modifier.fillMaxWidth().background(ScreenColors.TrainingMode.ActiveSetBg, RoundedCornerShape(12.dp)).border(1.dp, ScreenColors.TrainingMode.ActiveSetBorder, RoundedCornerShape(12.dp)).padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Cabecera Activa
                    Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                        Text("SERIE ACTIVA", style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Black), color = ScreenColors.TrainingMode.ActiveSetLabel)
                        Text("$currentSeries / ${activeSet.series}", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Black), color = ScreenColors.TrainingMode.ActiveSetValue)
                    }

                    // Objetivo de Lectura
                    val targetReps = if (activeSet.minReps == activeSet.maxReps) "${activeSet.minReps}" else "${activeSet.minReps}-${activeSet.maxReps}"
                    val targetRir = if (activeSet.minRir != null && activeSet.maxRir != null) { if (activeSet.minRir == activeSet.maxRir) " | RIR ${activeSet.minRir}" else " | RIR ${activeSet.minRir}-${activeSet.maxRir}" } else ""
                    Text("Objetivo: $targetReps Reps$targetRir", style = MaterialTheme.typography.bodyMedium, color = HunterTextSecondary)

                    // Inputs Reales (Bloqueados si isSeriesRunning == false)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        // Peso Real (Modifica Plantilla)
                        OutlinedTextField(
                            value = if (currentWeight == 0f) "" else currentWeight.toString(),
                            onValueChange = { str -> if (str.isEmpty()) onWeightChange(0f) else str.toFloatOrNull()?.let { onWeightChange(it) } },
                            label = { Text("Peso") },
                            modifier = Modifier.weight(1f),
                            singleLine = true,
                            enabled = isSeriesRunning,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            colors = TrainingInputColors(),
                            textStyle = MaterialTheme.typography.bodyLarge.copy(textAlign = TextAlign.Center, fontWeight = FontWeight.Bold)
                        )
                        // Reps Reales (Registro)
                        OutlinedTextField(
                            value = actualReps,
                            onValueChange = onActualRepsChange,
                            label = { Text("REPS") },
                            modifier = Modifier.weight(1f),
                            singleLine = true,
                            enabled = isSeriesRunning,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            colors = TrainingInputColors(),
                            textStyle = MaterialTheme.typography.bodyLarge.copy(textAlign = TextAlign.Center, fontWeight = FontWeight.Bold)
                        )
                        // RIR Real (Registro)
                        OutlinedTextField(
                            value = actualRir,
                            onValueChange = onActualRirChange,
                            label = { Text("RIR") },
                            modifier = Modifier.weight(1f),
                            singleLine = true,
                            enabled = isSeriesRunning,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            colors = TrainingInputColors(),
                            textStyle = MaterialTheme.typography.bodyLarge.copy(textAlign = TextAlign.Center, fontWeight = FontWeight.Bold)
                        )
                    }

                    // Observaciones de la Serie (Bloqueadas si isSeriesRunning == false)
                    OutlinedTextField(
                        value = currentObservations,
                        onValueChange = onObservationsChange,
                        label = { Text("Observaciones", style = MaterialTheme.typography.labelSmall) },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 1,
                        maxLines = 3,
                        enabled = isSeriesRunning,
                        colors = TrainingInputColors(),
                        shape = RoundedCornerShape(8.dp)
                    )

                    // BOTÓN INICIAR/TERMINAR SERIE
                    Button(
                        onClick = { if (isSeriesRunning) onStopSeries() else onStartSeries() },
                        modifier = Modifier.fillMaxWidth().height(50.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = if (isSeriesRunning) ScreenColors.TrainingMode.ControlPanelBtnRunningBg else ScreenColors.TrainingMode.ControlPanelBtnStoppedBg),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = if (isSeriesRunning) stringResource(R.string.training_hud_btn_end_series) else stringResource(R.string.training_hud_btn_start_series),
                            fontWeight = FontWeight.Bold,
                            color = ScreenColors.TrainingMode.ControlPanelBtnText,
                            letterSpacing = 1.sp
                        )
                    }
                }
            }

            // --- SECCIÓN: DESCANSO Y TIMER ---
            Column(
                modifier = Modifier.fillMaxWidth().background(ScreenColors.TrainingMode.TimerSectionBg, RoundedCornerShape(12.dp)).padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    FilledIconButton(onClick = { onRestMinutesChange(restMinutes - 1) }, enabled = !isTimerRunning && restMinutes > 1, modifier = Modifier.size(32.dp), colors = IconButtonDefaults.filledIconButtonColors(containerColor = ScreenColors.TrainingMode.TimerAdjustBtnBg)) { Text("-", fontWeight = FontWeight.Bold, color = ScreenColors.TrainingMode.TimerAdjustBtnContent) }
                    Text("${stringResource(R.string.training_hud_desc_prefix)} $restMinutes ${stringResource(R.string.training_min_suffix)}", style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold, letterSpacing = 1.sp), color = ScreenColors.TrainingMode.TimerDescLabel)
                    FilledIconButton(onClick = { onRestMinutesChange(restMinutes + 1) }, enabled = !isTimerRunning && restMinutes < 99, modifier = Modifier.size(32.dp), colors = IconButtonDefaults.filledIconButtonColors(containerColor = ScreenColors.TrainingMode.TimerAdjustBtnBg)) { Text("+", fontWeight = FontWeight.Bold, color = ScreenColors.TrainingMode.TimerAdjustBtnContent) }
                }

                Spacer(modifier = Modifier.height(12.dp))
                HunterDigitalClock(seconds = timerSeconds, isRunning = isTimerRunning)
                Spacer(modifier = Modifier.height(12.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                    Button(onClick = { if (isTimerRunning) onPauseTimer() else onResumeTimer() }, modifier = Modifier.weight(1f), enabled = timerSeconds > 0 && !isSeriesRunning, colors = ButtonDefaults.buttonColors(containerColor = ScreenColors.TrainingMode.TimerControlBtnBg), shape = RoundedCornerShape(8.dp)) { Icon(if (isTimerRunning) Icons.Default.Pause else Icons.Default.PlayArrow, null, tint = ScreenColors.TrainingMode.TimerControlBtnIcon) }
                    Button(onClick = onRestartTimer, modifier = Modifier.weight(1f), enabled = !isSeriesRunning && !isTimerRunning, colors = ButtonDefaults.buttonColors(containerColor = ScreenColors.TrainingMode.TimerControlBtnBg), shape = RoundedCornerShape(8.dp)) { Icon(Icons.Default.Refresh, null, tint = ScreenColors.TrainingMode.TimerControlBtnIcon) }
                    Button(onClick = onStopTimer, modifier = Modifier.weight(1f), enabled = timerSeconds > 0 && !isSeriesRunning, colors = ButtonDefaults.buttonColors(containerColor = ScreenColors.TrainingMode.TimerControlBtnBg), shape = RoundedCornerShape(8.dp)) { Icon(Icons.Default.Stop, null, tint = ScreenColors.TrainingMode.TimerButtonStop) }
                }

                if (isAlarmRinging) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Button(onClick = onStopAlarm, modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColors(containerColor = ScreenColors.TrainingMode.StopAlarmBtnBg), shape = RoundedCornerShape(8.dp)) {
                        Icon(Icons.Default.NotificationsOff, null, tint = ScreenColors.TrainingMode.StopAlarmBtnContent)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(stringResource(R.string.training_btn_stop_alarm), fontWeight = FontWeight.Bold, color = ScreenColors.TrainingMode.StopAlarmBtnContent)
                    }
                }
            }

            Button(
                onClick = onFinishExercise,
                modifier = Modifier.fillMaxWidth().height(56.dp),
                colors = ButtonDefaults.buttonColors(containerColor = ScreenColors.TrainingMode.NextBtnBg),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Finalizar Ejercicio", fontWeight = FontWeight.Black, letterSpacing = 1.sp, color = ScreenColors.TrainingMode.NextBtnText)
            }
        }
    }
}

@Composable
private fun HunterDigitalClock(seconds: Int, isRunning: Boolean) {
    val m = seconds / 60; val s = seconds % 60; val timeStr = String.format("%02d:%02d", m, s)
    Text(text = timeStr, style = MaterialTheme.typography.displayLarge.copy(fontWeight = FontWeight.Black, letterSpacing = 4.sp), color = if (isRunning) ScreenColors.TrainingMode.TimerRunningText else ScreenColors.TrainingMode.TimerStoppedText, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center)
}

@Composable
private fun HunterTrainingExerciseImage(exercise: Exercise) {
    Box(modifier = Modifier.size(80.dp).clip(RoundedCornerShape(12.dp)).background(ScreenColors.TrainingMode.ExImageBg).border(1.dp, ScreenColors.TrainingMode.ExImageBorder, RoundedCornerShape(12.dp))) {
        val iconRes = getGroupIcon(exercise.muscleGroup)
        Box(modifier = Modifier.align(Alignment.Center).size(60.dp).background(brush = Brush.radialGradient(colors = listOf(ScreenColors.TrainingMode.ExImageGlowStart, ScreenColors.TrainingMode.ExImageGlowEnd))))
        Icon(painter = painterResource(id = iconRes), contentDescription = null, tint = ScreenColors.TrainingMode.ExImageIcon, modifier = Modifier.align(Alignment.Center).size(40.dp))
    }
}

// Actualizado para manejar colores de campos deshabilitados (lectura)
@Composable
fun TrainingInputColors() = OutlinedTextFieldDefaults.colors(
    focusedContainerColor = HunterBlack,
    unfocusedContainerColor = HunterBlack,
    disabledContainerColor = HunterBlack,

    focusedBorderColor = ScreenColors.TrainingMode.NoteInputFocusedBorder,
    unfocusedBorderColor = ScreenColors.TrainingMode.NoteInputUnfocusedBorder,
    disabledBorderColor = ScreenColors.TrainingMode.NoteInputUnfocusedBorder.copy(alpha = 0.3f), // Borde atenuado al estar inactivo

    focusedLabelColor = ScreenColors.TrainingMode.NoteInputLabel,
    unfocusedLabelColor = ScreenColors.TrainingMode.NoteInputLabel,
    disabledLabelColor = ScreenColors.TrainingMode.NoteInputLabel.copy(alpha = 0.5f), // Label atenuado

    cursorColor = ScreenColors.TrainingMode.NoteInputCursor,

    focusedTextColor = HunterTextPrimary,
    unfocusedTextColor = HunterTextPrimary,
    disabledTextColor = HunterTextSecondary // Texto blanco/gris claro al estar inactivo
)

@DrawableRes
private fun getGroupIcon(group: MuscleGroup): Int {
    return when (group) {
        MuscleGroup.LEGS -> com.gymlog.app.R.drawable.ic_pierna; MuscleGroup.GLUTES -> com.gymlog.app.R.drawable.ic_gluteos
        MuscleGroup.BACK -> com.gymlog.app.R.drawable.ic_espalda; MuscleGroup.CHEST -> com.gymlog.app.R.drawable.ic_torso
        MuscleGroup.BICEPS -> com.gymlog.app.R.drawable.ic_biceps; MuscleGroup.TRICEPS -> com.gymlog.app.R.drawable.ic_triceps
        MuscleGroup.SHOULDERS -> com.gymlog.app.R.drawable.ic_hombros
    }
}