package com.gymlog.app.ui.screens.timer

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.gymlog.app.R
import com.gymlog.app.ui.theme.*
import com.gymlog.app.util.RequestNotificationPermission

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimerScreen(
    viewModel: TimerViewModel = hiltViewModel()
) {
    RequestNotificationPermission()
    val uiState by viewModel.uiState.collectAsState()

    val hours by viewModel.hours.collectAsState()
    val minutes by viewModel.minutes.collectAsState()
    val seconds by viewModel.seconds.collectAsState()
    val isRunning by viewModel.isRunning.collectAsState()
    val timeFinished by viewModel.timeFinished.collectAsState()

    Scaffold(
        containerColor = HunterBlack,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        stringResource(R.string.timer_title),
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Black,
                            letterSpacing = 2.sp
                        )
                    )
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
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceEvenly
        ) {
            // 1. SELECTOR DE TIEMPO (HUNTER STYLE)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 32.dp),
                contentAlignment = Alignment.Center
            ) {
                // Fondo de brillo sutil
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(150.dp)
                        .background(
                            brush = Brush.radialGradient(
                                colors = listOf(
                                    if (isRunning) HunterPurple.copy(alpha = 0.2f) else HunterPrimary.copy(alpha = 0.1f),
                                    Color.Transparent
                                )
                            )
                        )
                )

                Row(
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    HunterNumberPicker(
                        value = hours.toIntOrNull() ?: 0,
                        onValueChange = { viewModel.updateHours(String.format("%02d", it)) },
                        range = 0..23,
                        enabled = !isRunning,
                        label = "H"
                    )
                    Text(
                        ":",
                        fontSize = 48.sp,
                        fontWeight = FontWeight.Thin,
                        color = HunterTextSecondary.copy(alpha = 0.5f),
                        modifier = Modifier
                            .padding(horizontal = 8.dp)
                            .padding(bottom = 32.dp)
                    )
                    HunterNumberPicker(
                        value = minutes.toIntOrNull() ?: 0,
                        onValueChange = { viewModel.updateMinutes(String.format("%02d", it)) },
                        range = 0..59,
                        enabled = !isRunning,
                        label = "M"
                    )
                    Text(
                        ":",
                        fontSize = 48.sp,
                        fontWeight = FontWeight.Thin,
                        color = HunterTextSecondary.copy(alpha = 0.5f),
                        modifier = Modifier
                            .padding(horizontal = 8.dp)
                            .padding(bottom = 32.dp)
                    )
                    HunterNumberPicker(
                        value = seconds.toIntOrNull() ?: 0,
                        onValueChange = { viewModel.updateSeconds(String.format("%02d", it)) },
                        range = 0..59,
                        enabled = !isRunning,
                        label = "S"
                    )
                }
            }

            // 2. CONTROLES
            Row(
                horizontalArrangement = Arrangement.spacedBy(24.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (!isRunning) {
                    FilledIconButton(
                        onClick = viewModel::startTimer,
                        modifier = Modifier.size(90.dp),
                        colors = IconButtonDefaults.filledIconButtonColors(containerColor = HunterPrimary)
                    ) {
                        Icon(
                            Icons.Default.PlayArrow,
                            contentDescription = "Iniciar",
                            modifier = Modifier.size(48.dp),
                            tint = Color.Black
                        )
                    }
                } else {
                    FilledIconButton(
                        onClick = viewModel::pauseTimer,
                        modifier = Modifier.size(90.dp),
                        colors = IconButtonDefaults.filledIconButtonColors(containerColor = HunterPurple)
                    ) {
                        Icon(
                            Icons.Default.Pause,
                            contentDescription = "Pausar",
                            modifier = Modifier.size(48.dp),
                            tint = HunterTextPrimary
                        )
                    }
                }

                FilledTonalButton(
                    onClick = viewModel::resetTimer,
                    modifier = Modifier.size(60.dp),
                    shape = CircleShape,
                    colors = ButtonDefaults.filledTonalButtonColors(containerColor = HunterSurface)
                ) {
                    Icon(
                        Icons.Default.Refresh,
                        contentDescription = "Reiniciar",
                        modifier = Modifier.size(24.dp),
                        tint = HunterTextPrimary
                    )
                }
            }

            Text(
                text = if (isRunning) stringResource(R.string.timer_system_active) else stringResource(R.string.timer_system_waiting),
                style = MaterialTheme.typography.labelLarge.copy(letterSpacing = 2.sp),
                color = if (isRunning) HunterPurple else HunterTextSecondary
            )
        }
    }

    if (timeFinished) {
        HunterConfirmDialog(
            title = stringResource(R.string.timer_dialog_finished_title),
            text = stringResource(R.string.timer_dialog_finished_text),
            confirmText = stringResource(R.string.common_accept),
            onConfirm = viewModel::dismissTimeFinished,
            onDismiss = {}
        )
    }
}

@Composable
private fun HunterNumberPicker(
    value: Int,
    onValueChange: (Int) -> Unit,
    range: IntRange,
    enabled: Boolean,
    label: String
) {
    var localValue by remember(value) { mutableIntStateOf(value) }
    var accumulatedDrag by remember { mutableFloatStateOf(0f) }

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .width(80.dp)
                .height(100.dp)
                .pointerInput(enabled) {
                    if (enabled) {
                        detectDragGestures(
                            onDragEnd = { accumulatedDrag = 0f }
                        ) { _, dragAmount ->
                            accumulatedDrag += dragAmount.y
                            val threshold = 40f
                            if (accumulatedDrag <= -threshold) {
                                val newValue = if (localValue + 1 > range.last) range.first else localValue + 1
                                onValueChange(newValue)
                                accumulatedDrag = 0f
                                localValue = newValue
                            } else if (accumulatedDrag >= threshold) {
                                val newValue = if (localValue - 1 < range.first) range.last else localValue - 1
                                onValueChange(newValue)
                                accumulatedDrag = 0f
                                localValue = newValue
                            }
                        }
                    }
                },
            contentAlignment = Alignment.Center
        ) {
            // Número Principal
            Text(
                text = String.format("%02d", value),
                fontSize = 64.sp,
                fontWeight = FontWeight.Black,
                color = if (enabled) HunterTextPrimary else ScreenColors.Timer.PickerTextDisabled,
                textAlign = TextAlign.Center,
                letterSpacing = (-2).sp
            )
        }

        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = ScreenColors.Timer.PickerLabel,
            fontWeight = FontWeight.Bold
        )
    }
}