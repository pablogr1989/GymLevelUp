package com.gymlog.app.ui.screens.calendars

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.annotation.DrawableRes
import androidx.hilt.navigation.compose.hiltViewModel
import com.gymlog.app.data.local.entity.DayCategory
import com.gymlog.app.domain.model.DaySlot
import com.gymlog.app.domain.model.MonthWithWeeks
import com.gymlog.app.domain.model.WeekWithDays
import com.gymlog.app.ui.theme.AppColors

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
        topBar = {
            TopAppBar(
                title = { Text(calendarWithMonths?.calendar?.name ?: "Calendario") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Volver")
                    }
                },
                actions = {
                    if (!isSelectionMode && swapSourceDayId == null) {
                        IconButton(onClick = viewModel::showClearAllDialog) {
                            Icon(Icons.Default.ClearAll, contentDescription = "Limpiar checks")
                        }
                    }
                }
            )
        },
        bottomBar = {
            if (swapSourceDayId != null) {
                BottomAppBar(
                    containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                    contentColor = MaterialTheme.colorScheme.onTertiaryContainer
                ) {
                    Text(
                        text = "Selecciona el día destino para mover",
                        modifier = Modifier.weight(1f).padding(start = 16.dp),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold
                    )
                    TextButton(onClick = viewModel::cancelSwap) {
                        Text("Cancelar")
                    }
                }
            } else if (isSelectionMode) {
                MultiSelectControls(
                    selectedCount = selectedDayIds.size,
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

                Column(
                    modifier = Modifier.fillMaxSize()
                ) {
                    data.months.getOrNull(currentMonthIndex)?.let { monthWithWeeks ->
                        MonthGrid(
                            monthWithWeeks = monthWithWeeks,
                            modifier = Modifier.weight(1f),
                            isSelectionMode = isSelectionMode,
                            selectedDayIds = selectedDayIds,
                            swapSourceDayId = swapSourceDayId,
                            onDayClick = { daySlot ->
                                viewModel.onDayClick(daySlot.id) { id ->
                                    onNavigateToEdit(id)
                                }
                            },
                            onDayLongPress = { daySlot ->
                                viewModel.onDayLongPress(daySlot.id)
                            }
                        )
                    }
                }
            }
        }
    }

    if (showClearAllDialog) {
        AlertDialog(
            onDismissRequest = viewModel::dismissClearAllDialog,
            title = { Text("Limpiar checks") },
            text = { Text("¿Estás seguro de que quieres limpiar todos los checks del calendario?") },
            confirmButton = {
                TextButton(onClick = viewModel::clearAllCompleted) {
                    Text("Confirmar")
                }
            },
            dismissButton = {
                TextButton(onClick = viewModel::dismissClearAllDialog) {
                    Text("Cancelar")
                }
            }
        )
    }
}

@Composable
private fun DaysOfWeekHeader() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        val dayNames = listOf("L", "M", "X", "J", "V", "S", "D")
        dayNames.forEach { dayName ->
            Text(
                text = dayName,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
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
        IconButton(
            onClick = onPreviousMonth,
            enabled = canGoPrevious
        ) {
            Icon(
                imageVector = Icons.Default.ChevronLeft,
                contentDescription = "Mes anterior"
            )
        }

        Text(
            text = currentMonth?.month?.name ?: "",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )

        IconButton(
            onClick = onNextMonth,
            enabled = canGoNext
        ) {
            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = "Mes siguiente"
            )
        }
    }
}

@Composable
private fun MonthGrid(
    monthWithWeeks: MonthWithWeeks,
    isSelectionMode: Boolean,
    selectedDayIds: Set<String>,
    swapSourceDayId: String?,
    onDayClick: (DaySlot) -> Unit,
    onDayLongPress: (DaySlot) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier,
        contentPadding = PaddingValues(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(monthWithWeeks.weeks) { weekWithDays ->
            WeekRow(
                weekWithDays = weekWithDays,
                isSelectionMode = isSelectionMode,
                selectedDayIds = selectedDayIds,
                swapSourceDayId = swapSourceDayId,
                onDayClick = onDayClick,
                onDayLongPress = onDayLongPress
            )
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
                isSelectionMode = isSelectionMode,
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
    isSelectionMode: Boolean,
    isSelected: Boolean,
    isSwapSource: Boolean,
    onDayClick: (DaySlot) -> Unit,
    onDayLongPress: (DaySlot) -> Unit,
    modifier: Modifier = Modifier
) {
    val iconRes = getCategoryIcon(daySlot.categories.firstOrNull())

    val borderModifier = if (isSwapSource) {
        Modifier.border(2.dp, MaterialTheme.colorScheme.tertiary, RoundedCornerShape(12.dp))
    } else {
        Modifier
    }

    Card(
        modifier = modifier
            .aspectRatio(1f)
            .then(borderModifier)
            .combinedClickable(
                onClick = { onDayClick(daySlot) },
                onLongClick = { onDayLongPress(daySlot) }
            ),
        colors = CardDefaults.cardColors(
            containerColor = when {
                isSwapSource -> MaterialTheme.colorScheme.tertiaryContainer
                isSelected -> MaterialTheme.colorScheme.primaryContainer
                daySlot.completed -> MaterialTheme.colorScheme.secondaryContainer
                else -> AppColors.Global.CalendarBox
            }
        )
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                painter = painterResource(id = iconRes),
                contentDescription = null,
                modifier = Modifier.size(24.dp),
                tint = when {
                    isSwapSource -> MaterialTheme.colorScheme.onTertiaryContainer
                    isSelected -> MaterialTheme.colorScheme.onPrimaryContainer
                    daySlot.completed -> MaterialTheme.colorScheme.onSecondaryContainer
                    else -> MaterialTheme.colorScheme.surface
                }
            )
        }
    }
}

@Composable
private fun MultiSelectControls(
    selectedCount: Int,
    onMarkAll: () -> Unit,
    onUnmarkAll: () -> Unit,
    onCancel: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TextButton(onClick = onMarkAll) { Text("Marcar") }
                TextButton(onClick = onUnmarkAll) { Text("Desmarcar") }
                TextButton(onClick = onCancel) { Text("Cancelar") }
            }
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