package com.gymlog.app.ui.screens.calendars

import androidx.annotation.DrawableRes
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.gymlog.app.data.local.entity.DayCategory
import com.gymlog.app.domain.model.DaySlot
import com.gymlog.app.domain.model.MonthWithWeeks
import com.gymlog.app.domain.model.WeekWithDays
import com.gymlog.app.ui.theme.HunterConfirmDialog
import com.gymlog.app.ui.theme.RankB
import com.gymlog.app.ui.theme.RankD

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun CalendarDetailScreen(
    onNavigateBack: () -> Unit,
    onNavigateToEdit: (String) -> Unit,
    onNavigateToExercise: (String) -> Unit = {},
    viewModel: CalendarDetailViewModel = hiltViewModel()
) {
    val calendarWithMonths by viewModel.calendarWithMonths.collectAsState()
    val currentMonthIndex by viewModel.currentMonthIndex.collectAsState()
    val isSelectionMode by viewModel.isSelectionMode.collectAsState()
    val selectedDayIds by viewModel.selectedDayIds.collectAsState()
    val showClearAllDialog by viewModel.showClearAllDialog.collectAsState()
    val swapSourceDayId by viewModel.swapSourceDayId.collectAsState()

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text(calendarWithMonths?.calendar?.name?.uppercase() ?: "CALENDARIO", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Black)) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Volver", tint = Color.White)
                    }
                },
                actions = {
                    if (!isSelectionMode && swapSourceDayId == null) {
                        IconButton(onClick = viewModel::showClearAllDialog) {
                            Icon(Icons.Default.CleaningServices, contentDescription = "Limpiar", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        },
        bottomBar = {
            if (swapSourceDayId != null) {
                // Barra de intercambio Hunter Style
                Surface(
                    color = MaterialTheme.colorScheme.tertiaryContainer,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "SELECCIONA DESTINO...",
                            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onTertiaryContainer
                        )
                        TextButton(onClick = viewModel::cancelSwap) {
                            Text("CANCELAR", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            } else if (isSelectionMode) {
                MultiSelectControls(
                    onMarkAll = viewModel::markSelectedAsCompleted,
                    onUnmarkAll = viewModel::clearSelectedCompleted,
                    onCancel = viewModel::clearSelection
                )
            }
        }
    ) { paddingValues ->
        calendarWithMonths?.let { data ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                MonthNavigation(
                    currentMonth = data.months.getOrNull(currentMonthIndex),
                    onPreviousMonth = { viewModel.changeMonth(-1) },
                    onNextMonth = { viewModel.changeMonth(1) },
                    canGoNext = currentMonthIndex < data.months.size - 1,
                    canGoPrevious = currentMonthIndex > 0
                )

                DaysOfWeekHeader()

                // GRID DE DÍAS
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    data.months.getOrNull(currentMonthIndex)?.let { monthWithWeeks ->
                        items(monthWithWeeks.weeks) { weekWithDays ->
                            WeekRow(
                                weekWithDays = weekWithDays,
                                isSelectionMode = isSelectionMode,
                                selectedDayIds = selectedDayIds,
                                swapSourceDayId = swapSourceDayId,
                                onDayClick = { daySlot ->
                                    viewModel.onDayClick(daySlot.id) { id -> onNavigateToEdit(id) }
                                },
                                onDayLongPress = { daySlot -> viewModel.onDayLongPress(daySlot.id) }
                            )
                        }
                    }
                }
            }
        }
    }

    if (showClearAllDialog) {
        HunterConfirmDialog(
            title = "RESETEAR PROGRESO",
            text = "¿Limpiar todos los completados de este calendario?",
            confirmText = "LIMPIAR",
            onConfirm = viewModel::clearAllCompleted,
            onDismiss = viewModel::dismissClearAllDialog
        )
    }
}

@Composable
private fun DaysOfWeekHeader() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface)
            .padding(vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        listOf("L", "M", "X", "J", "V", "S", "D").forEach { day ->
            Text(
                text = day,
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.weight(1f),
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun MonthNavigation(
    currentMonth: MonthWithWeeks?,
    onPreviousMonth: () -> Unit,
    onNextMonth: () -> Unit,
    canGoNext: Boolean,
    canGoPrevious: Boolean
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onPreviousMonth, enabled = canGoPrevious) {
            Icon(Icons.Default.ChevronLeft, null, tint = if (canGoPrevious) Color.White else Color.Gray)
        }

        Text(
            text = currentMonth?.month?.name?.uppercase() ?: "",
            style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Black, letterSpacing = 2.sp),
            color = MaterialTheme.colorScheme.primary
        )

        IconButton(onClick = onNextMonth, enabled = canGoNext) {
            Icon(Icons.Default.ChevronRight, null, tint = if (canGoNext) Color.White else Color.Gray)
        }
    }
}

@Composable
private fun WeekRow(
    weekWithDays: WeekWithDays,
    isSelectionMode: Boolean,
    selectedDayIds: Set<String>,
    swapSourceDayId: String?,
    onDayClick: (DaySlot) -> Unit,
    onDayLongPress: (DaySlot) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        weekWithDays.days.forEach { daySlot ->
            DayBox(
                daySlot = daySlot,
                isSelected = selectedDayIds.contains(daySlot.id),
                isSwapSource = swapSourceDayId == daySlot.id,
                onDayClick = onDayClick,
                onDayLongPress = onDayLongPress,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun DayBox(
    daySlot: DaySlot,
    isSelected: Boolean,
    isSwapSource: Boolean,
    onDayClick: (DaySlot) -> Unit,
    onDayLongPress: (DaySlot) -> Unit,
    modifier: Modifier = Modifier
) {
    val category = daySlot.categories.firstOrNull()

    // Colores y Bordes dinámicos
    val borderColor = when {
        isSwapSource -> MaterialTheme.colorScheme.tertiary // Origen del movimiento (Ámbar/Rojo)
        isSelected -> MaterialTheme.colorScheme.primary // Seleccionado (Azul)
        daySlot.completed -> RankB // Completado (Verde - RankD importado de Color.kt)
        else -> MaterialTheme.colorScheme.outline.copy(alpha = 0.1f) // Normal (Gris suave)
    }

    val backgroundColor = when {
        daySlot.completed -> RankB.copy(alpha = 0.1f) // Fondo sutil verde si completado
        else -> MaterialTheme.colorScheme.surface
    }

    val glowModifier = if (isSwapSource || isSelected) {
        Modifier.border(2.dp, borderColor, RoundedCornerShape(8.dp))
    } else {
        Modifier.border(1.dp, borderColor, RoundedCornerShape(8.dp))
    }

    Box(
        modifier = modifier
            .aspectRatio(1f)
            .clip(RoundedCornerShape(8.dp))
            .background(backgroundColor)
            .then(glowModifier)
            .combinedClickable(
                onClick = { onDayClick(daySlot) },
                onLongClick = { onDayLongPress(daySlot) }
            ),
        contentAlignment = Alignment.Center
    ) {
        if (category != null) {
            // Icono de categoría
            Icon(
                painter = painterResource(id = getCategoryIcon(category)),
                contentDescription = null,
                modifier = Modifier.size(24.dp),
                tint = if (daySlot.completed) RankB else MaterialTheme.colorScheme.onSurfaceVariant
            )
        } else {
            // Día vacío (Punto discreto)
            Box(
                modifier = Modifier
                    .size(4.dp)
                    .background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.2f), CircleShape)
            )
        }

        // Indicador de día de la semana (opcional, número)
        // Text(text = daySlot.dayOfWeek.dayNumber.toString()...)
    }
}

@Composable
private fun MultiSelectControls(
    onMarkAll: () -> Unit,
    onUnmarkAll: () -> Unit,
    onCancel: () -> Unit
) {
    Surface(
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 8.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            TextButton(onClick = onMarkAll) { Text("MARCAR", color = RankD, fontWeight = FontWeight.Bold) }
            TextButton(onClick = onUnmarkAll) { Text("DESMARCAR", color = MaterialTheme.colorScheme.error) }
            TextButton(onClick = onCancel) { Text("LISTO", color = Color.White) }
        }
    }
}

@DrawableRes
private fun getCategoryIcon(category: DayCategory?): Int {
    return when (category) {
        DayCategory.BACK -> com.gymlog.app.R.drawable.ic_espalda
        DayCategory.BICEPS -> com.gymlog.app.R.drawable.ic_biceps
        DayCategory.LEGS -> com.gymlog.app.R.drawable.ic_pierna
        DayCategory.GLUTES -> com.gymlog.app.R.drawable.ic_gluteos
        DayCategory.CHEST -> com.gymlog.app.R.drawable.ic_torso
        DayCategory.TRICEPS -> com.gymlog.app.R.drawable.ic_triceps
        DayCategory.SHOULDERS -> com.gymlog.app.R.drawable.ic_hombros
        DayCategory.CARDIO -> com.gymlog.app.R.drawable.ic_cardio
        DayCategory.REST -> com.gymlog.app.R.drawable.ic_rest
        DayCategory.FULL_BODY -> com.gymlog.app.R.drawable.ic_fullbody
        else -> com.gymlog.app.R.drawable.ic_exercise_placeholder
    }
}