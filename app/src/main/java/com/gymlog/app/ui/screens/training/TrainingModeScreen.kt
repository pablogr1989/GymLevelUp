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

    var showFinishSeriesDialog by remember { mutableStateOf(false) }
    var showFinishExerciseDialog by remember { mutableStateOf(false) }

    BackHandler(enabled = uiState.isTrainingActive) {
        viewModel.onBackPressed()
    }

    if (uiState.showExitConfirmation) {
        HunterConfirmDialog(
            title = stringResource(R.string.training_dialog_exit_title),
            text = stringResource(R.string.training_dialog_exit_text),
            confirmText = stringResource(R.string.training_dialog_exit_confirm),
            onConfirm = {
                viewModel.confirmExit()
                onNavigateBack()
            },
            onDismiss = viewModel::dismissExitConfirmation
        )
    }

    Scaffold(
        containerColor = HunterBlack,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        stringResource(R.string.training_title),
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Black,
                            letterSpacing = 2.sp,
                            color = if (uiState.isTimerRunning) ScreenColors.TrainingMode.TimerRunningText else ScreenColors.TrainingMode.TitleTextDefault
                        )
                    )
                },
                navigationIcon = {
                    IconButton(onClick = {
                        if (uiState.isTrainingActive) viewModel.onBackPressed() else onNavigateBack()
                    }) {
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
                    isAlarmRinging = uiState.isAlarmRinging,
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
                    onStopAlarm = viewModel::stopAlarm,
                    onFinishExercise = { showFinishExerciseDialog = true },
                    onRestMinutesChange = viewModel::updateRestMinutes
                )
            } else if (currentExercise != null) {
                Box(
                    modifier = Modifier.fillMaxWidth().padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = stringResource(R.string.training_system_waiting),
                        style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold, letterSpacing = 1.sp),
                        color = HunterTextSecondary.copy(alpha = 0.5f)
                    )
                }
            } else if (uiState.exercises.isEmpty()) {
                Text(
                    text = stringResource(R.string.training_no_missions),
                    style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
                    color = ScreenColors.Global.ErrorRed,
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                )
            }
        }

        // Diálogos
        if (showFinishSeriesDialog) {
            HunterConfirmDialog(
                title = stringResource(R.string.training_dialog_finish_series_title),
                text = stringResource(R.string.training_dialog_finish_series_text),
                confirmText = stringResource(R.string.training_dialog_finish_series_confirm),
                onConfirm = {
                    viewModel.confirmFinishExercise()
                    showFinishSeriesDialog = false
                },
                onDismiss = { showFinishSeriesDialog = false }
            )
        }

        if (showFinishExerciseDialog) {
            HunterConfirmDialog(
                title = stringResource(R.string.training_dialog_force_finish_title),
                text = stringResource(R.string.training_dialog_force_finish_text),
                confirmText = stringResource(R.string.training_dialog_force_finish_confirm),
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
            Text(stringResource(R.string.training_progress_label), style = MaterialTheme.typography.labelSmall, color = ScreenColors.TrainingMode.ProgressLabel)
            Text("${(progressFraction * 100).toInt()}%", style = MaterialTheme.typography.labelSmall, color = ScreenColors.TrainingMode.ProgressValue)
        }
        Spacer(modifier = Modifier.height(4.dp))
        LinearProgressIndicator(
            progress = { animatedProgress },
            modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(4.dp)),
            color = ScreenColors.TrainingMode.ProgressIndicator,
            trackColor = ScreenColors.TrainingMode.ProgressTrack
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
                stringResource(R.string.training_section_objectives),
                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold, letterSpacing = 1.sp),
                color = ScreenColors.TrainingMode.ObjectiveTitle
            )

            if (exercises.isEmpty()) {
                Text(stringResource(R.string.training_no_objectives), style = MaterialTheme.typography.bodyMedium, color = ScreenColors.TrainingMode.ObjectiveEmptyText)
            } else {
                exercises.forEachIndexed { index, exercise ->
                    val isCurrent = isTrainingActive && index == currentIndex
                    val isDone = isTrainingActive && index < currentIndex

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                if (isCurrent) ScreenColors.TrainingMode.ObjectiveItemBgCurrent else ScreenColors.TrainingMode.ObjectiveItemBgDefault,
                                RoundedCornerShape(8.dp)
                            )
                            .padding(8.dp)
                    ) {
                        // Indicador de estado
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .background(
                                    when {
                                        isCurrent -> ScreenColors.TrainingMode.ObjectiveDotCurrent
                                        isDone -> ScreenColors.TrainingMode.ObjectiveDotDone
                                        else -> ScreenColors.TrainingMode.ObjectiveDotPending
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
                            color = if (isDone) ScreenColors.TrainingMode.ObjectiveTextDone else ScreenColors.TrainingMode.ObjectiveTextDefault,
                            modifier = Modifier.weight(1f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        if (isDone) {
                            Icon(Icons.Default.Check, null, tint = ScreenColors.TrainingMode.ObjectiveCheckIcon, modifier = Modifier.size(16.dp))
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
            Text(stringResource(R.string.training_rest_config_title), style = MaterialTheme.typography.labelMedium, color = ScreenColors.TrainingMode.RestConfigLabel)

            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilledIconButton(
                    onClick = { onMinutesChange(minutes - 1) },
                    enabled = minutes > 1,
                    colors = IconButtonDefaults.filledIconButtonColors(containerColor = ScreenColors.TrainingMode.RestConfigBtnBg),
                    modifier = Modifier.size(32.dp)
                ) { Icon(Icons.Default.Remove, null, modifier = Modifier.size(16.dp), tint = ScreenColors.TrainingMode.RestConfigBtnContent) }

                Text(
                    text = "$minutes ${stringResource(R.string.training_min_suffix)}",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, letterSpacing = 1.sp),
                    color = ScreenColors.TrainingMode.RestConfigValue
                )

                FilledIconButton(
                    onClick = { onMinutesChange(minutes + 1) },
                    enabled = minutes < 99,
                    colors = IconButtonDefaults.filledIconButtonColors(containerColor = ScreenColors.TrainingMode.RestConfigBtnBg),
                    modifier = Modifier.size(32.dp)
                ) { Icon(Icons.Default.Add, null, modifier = Modifier.size(16.dp), tint = ScreenColors.TrainingMode.RestConfigBtnContent) }
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
            icon = {
                Icon(
                    imageVector = if (isActive) Icons.Default.Flag else Icons.Default.PlayArrow,
                    contentDescription = null,
                    tint = ScreenColors.TrainingMode.MainBtnText
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
                .background(Brush.horizontalGradient(listOf(ScreenColors.TrainingMode.AlarmBtnGradientStart, ScreenColors.TrainingMode.AlarmBtnGradientEnd)))
                .padding(horizontal = 16.dp),
            contentAlignment = Alignment.Center
        ) {
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
    isAlarmRinging: Boolean,
    restMinutes: Int,
    onWeightChange: (Float) -> Unit,
    onNotesChange: (String) -> Unit,
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
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = ScreenColors.TrainingMode.HudCardBg),
        border = BorderStroke(1.dp, ScreenColors.TrainingMode.HudCardBorder),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {

            // CABECERA: Título + Imagen
            if (exercise.imageUri != null) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = exercise.name.uppercase(),
                        style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Black),
                        color = ScreenColors.TrainingMode.HudTitle
                    )
                    Text(
                        text = stringResource(UiMappers.getDisplayNameRes(exercise.muscleGroup)).uppercase(),
                        style = MaterialTheme.typography.labelMedium,
                        color = ScreenColors.TrainingMode.HudMuscleGroup
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .border(1.dp, ScreenColors.TrainingMode.HudImageBorder, RoundedCornerShape(12.dp))
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
                            color = ScreenColors.TrainingMode.HudTitle,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = stringResource(UiMappers.getDisplayNameRes(exercise.muscleGroup)).uppercase(),
                            style = MaterialTheme.typography.labelMedium,
                            color = ScreenColors.TrainingMode.HudMuscleGroup
                        )
                    }
                }
            }

            Divider(color = ScreenColors.TrainingMode.HudDivider)

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
                    colors = CardDefaults.cardColors(containerColor = ScreenColors.TrainingMode.OtherSetsBg),
                    border = BorderStroke(1.dp, ScreenColors.TrainingMode.OtherSetsBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(stringResource(R.string.training_hud_others), style = MaterialTheme.typography.labelSmall, color = ScreenColors.TrainingMode.OtherSetsLabel)
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
                label = { Text(stringResource(R.string.training_hud_notes_label), style = MaterialTheme.typography.labelSmall) },
                modifier = Modifier.fillMaxWidth(),
                minLines = 3,
                maxLines = 6,
                colors = TrainingInputColors(),
                shape = RoundedCornerShape(12.dp)
            )

            // PANEL DE CONTROL DE SERIE
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier
                    .fillMaxWidth()
                    .background(ScreenColors.TrainingMode.ControlPanelBg, RoundedCornerShape(12.dp))
                    .border(1.dp, ScreenColors.TrainingMode.ControlPanelBorder, RoundedCornerShape(12.dp))
                    .padding(12.dp)
            ) {
                Column {
                    Text(stringResource(R.string.training_hud_current_series), style = MaterialTheme.typography.labelSmall, color = ScreenColors.TrainingMode.ControlPanelLabel)
                    Text(
                        text = "$currentSeries / ${activeSet?.series ?: "?"}",
                        style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Black, color = ScreenColors.TrainingMode.ControlPanelValue)
                    )
                }

                // BOTÓN DE ACCIÓN PRINCIPAL
                Button(
                    onClick = { if (isSeriesRunning) onStopSeries() else onStartSeries() },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isSeriesRunning) ScreenColors.TrainingMode.ControlPanelBtnRunningBg else ScreenColors.TrainingMode.ControlPanelBtnStoppedBg
                    ),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = if (isSeriesRunning) stringResource(R.string.training_hud_btn_end_series)
                        else stringResource(R.string.training_hud_btn_start_series),
                        fontWeight = FontWeight.Bold,
                        color = ScreenColors.TrainingMode.ControlPanelBtnText
                    )
                }
            }

            // DESCANSO Y TIMER
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(ScreenColors.TrainingMode.TimerSectionBg, RoundedCornerShape(12.dp))
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Selector Descanso
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    FilledIconButton(
                        onClick = { onRestMinutesChange(restMinutes - 1) },
                        enabled = !isTimerRunning && restMinutes > 1,
                        modifier = Modifier.size(32.dp),
                        colors = IconButtonDefaults.filledIconButtonColors(containerColor = ScreenColors.TrainingMode.TimerAdjustBtnBg)
                    ) { Text("-", fontWeight = FontWeight.Bold, color = ScreenColors.TrainingMode.TimerAdjustBtnContent) }

                    Text(
                        "${stringResource(R.string.training_hud_desc_prefix)} $restMinutes ${stringResource(R.string.training_min_suffix)}",
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold, letterSpacing = 1.sp),
                        color = ScreenColors.TrainingMode.TimerDescLabel
                    )

                    FilledIconButton(
                        onClick = { onRestMinutesChange(restMinutes + 1) },
                        enabled = !isTimerRunning && restMinutes < 99,
                        modifier = Modifier.size(32.dp),
                        colors = IconButtonDefaults.filledIconButtonColors(containerColor = ScreenColors.TrainingMode.TimerAdjustBtnBg)
                    ) { Text("+", fontWeight = FontWeight.Bold, color = ScreenColors.TrainingMode.TimerAdjustBtnContent) }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // RELOJ
                HunterDigitalClock(seconds = timerSeconds, isRunning = isTimerRunning)

                Spacer(modifier = Modifier.height(12.dp))

                // Controles Timer
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                    Button(
                        onClick = { if (isTimerRunning) onPauseTimer() else onResumeTimer() },
                        modifier = Modifier.weight(1f),
                        enabled = timerSeconds > 0 && !isSeriesRunning,
                        colors = ButtonDefaults.buttonColors(containerColor = ScreenColors.TrainingMode.TimerControlBtnBg),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(if (isTimerRunning) Icons.Default.Pause else Icons.Default.PlayArrow, null, tint = ScreenColors.TrainingMode.TimerControlBtnIcon)
                    }

                    Button(
                        onClick = onRestartTimer,
                        modifier = Modifier.weight(1f),
                        enabled = !isSeriesRunning && !isTimerRunning,
                        colors = ButtonDefaults.buttonColors(containerColor = ScreenColors.TrainingMode.TimerControlBtnBg),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(Icons.Default.Refresh, null, tint = ScreenColors.TrainingMode.TimerControlBtnIcon)
                    }

                    Button(
                        onClick = onStopTimer,
                        modifier = Modifier.weight(1f),
                        enabled = timerSeconds > 0 && !isSeriesRunning,
                        colors = ButtonDefaults.buttonColors(containerColor = ScreenColors.TrainingMode.TimerControlBtnBg),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(Icons.Default.Stop, null, tint = ScreenColors.TrainingMode.TimerButtonStop)
                    }
                }

                // ==========================================
                // BOTÓN PARAR ALARMA
                // ==========================================
                if (isAlarmRinging) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Button(
                        onClick = onStopAlarm,
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = ScreenColors.TrainingMode.StopAlarmBtnBg),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(Icons.Default.NotificationsOff, null, tint = ScreenColors.TrainingMode.StopAlarmBtnContent)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(stringResource(R.string.training_btn_stop_alarm), fontWeight = FontWeight.Bold, color = ScreenColors.TrainingMode.StopAlarmBtnContent)
                    }
                }
            }

            // SIGUIENTE OBJETIVO
            Button(
                onClick = onFinishExercise,
                modifier = Modifier.fillMaxWidth().height(56.dp),
                colors = ButtonDefaults.buttonColors(containerColor = ScreenColors.TrainingMode.NextBtnBg),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(stringResource(R.string.training_hud_btn_next), fontWeight = FontWeight.Black, letterSpacing = 1.sp, color = ScreenColors.TrainingMode.NextBtnText)
            }
        }
    }
}

@Composable
private fun HunterActiveSetRow(set: Set, currentWeight: Float, onWeightChange: (Float) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(ScreenColors.TrainingMode.ActiveSetBg, RoundedCornerShape(8.dp))
            .border(1.dp, ScreenColors.TrainingMode.ActiveSetBorder, RoundedCornerShape(8.dp))
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(stringResource(R.string.training_hud_active_label), style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold), color = ScreenColors.TrainingMode.ActiveSetLabel)

        Row(horizontalArrangement = Arrangement.spacedBy(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("${set.series}", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold), color = ScreenColors.TrainingMode.ActiveSetValue)
                Text(stringResource(R.string.common_series), style = MaterialTheme.typography.labelSmall, color = ScreenColors.TrainingMode.ActiveSetSubLabel)
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("${set.reps}", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold), color = ScreenColors.TrainingMode.ActiveSetValue)
                Text(stringResource(R.string.common_reps), style = MaterialTheme.typography.labelSmall, color = ScreenColors.TrainingMode.ActiveSetSubLabel)
            }

            OutlinedTextField(
                value = if (currentWeight == 0f) "" else currentWeight.toString(),
                onValueChange = { str ->
                    if (str.isEmpty()) onWeightChange(0f)
                    else str.toFloatOrNull()?.let { onWeightChange(it) }
                },
                label = { Text(stringResource(R.string.common_kg), style = MaterialTheme.typography.labelSmall) },
                modifier = Modifier.width(100.dp),
                singleLine = true,
                textStyle = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, textAlign = TextAlign.Center),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                colors = TrainingInputColors()
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
        Text(stringResource(R.string.training_hud_variant_fmt, index), style = MaterialTheme.typography.bodyMedium, color = ScreenColors.TrainingMode.OtherSetsLabel)
        Text(
            "${set.series} × ${set.reps} @ ${set.weightKg} ${stringResource(R.string.common_kg)}",
            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
            color = ScreenColors.TrainingMode.OtherSetsValue
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
        color = if (isRunning) ScreenColors.TrainingMode.TimerRunningText else ScreenColors.TrainingMode.TimerStoppedText,
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
            .background(ScreenColors.TrainingMode.ExImageBg)
            .border(1.dp, ScreenColors.TrainingMode.ExImageBorder, RoundedCornerShape(12.dp))
    ) {
        val iconRes = getGroupIcon(exercise.muscleGroup)

        Box(
            modifier = Modifier
                .align(Alignment.Center)
                .size(60.dp)
                .background(
                    brush = Brush.radialGradient(
                        colors = listOf(ScreenColors.TrainingMode.ExImageGlowStart, ScreenColors.TrainingMode.ExImageGlowEnd)
                    )
                )
        )
        Icon(
            painter = painterResource(id = iconRes),
            contentDescription = null,
            tint = ScreenColors.TrainingMode.ExImageIcon,
            modifier = Modifier.align(Alignment.Center).size(40.dp)
        )
    }
}

// NUEVA FUNCIÓN ESPECÍFICA PARA COLORES DE INPUT EN TRAINING
@Composable
fun TrainingInputColors() = OutlinedTextFieldDefaults.colors(
    focusedContainerColor = HunterBlack,
    unfocusedContainerColor = HunterBlack,
    focusedBorderColor = ScreenColors.TrainingMode.NoteInputFocusedBorder,
    unfocusedBorderColor = ScreenColors.TrainingMode.NoteInputUnfocusedBorder,
    focusedLabelColor = ScreenColors.TrainingMode.NoteInputLabel,
    unfocusedLabelColor = ScreenColors.TrainingMode.NoteInputLabel,
    cursorColor = ScreenColors.TrainingMode.NoteInputCursor,
    focusedTextColor = HunterTextPrimary,
    unfocusedTextColor = HunterTextPrimary
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