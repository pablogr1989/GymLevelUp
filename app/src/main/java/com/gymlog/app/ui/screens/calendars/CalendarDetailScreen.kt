package com.gymlog.app.ui.screens.calendars

import android.util.Log
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
                    if (!isSelectionMode) {
                        IconButton(onClick = viewModel::showClearAllDialog) {
                            Icon(Icons.Default.ClearAll, contentDescription = "Limpiar checks")
                        }
                    }
                }
            )
        }
    ) { paddingValues ->
        calendarWithMonths?.let { data ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                // Navigation between months
                Log.d("CalendarScreen", "Mostrando mes: $currentMonthIndex")
                MonthNavigation(
                    currentMonth = data.months.getOrNull(currentMonthIndex),
                    onPreviousMonth = { viewModel.changeMonth(-1) },
                    onNextMonth = { viewModel.changeMonth(1) },
                    canGoNext = currentMonthIndex < data.months.size - 1,
                    canGoPrevious = currentMonthIndex > 0
                )

                // Days of week header
                DaysOfWeekHeader()

                Column(
                    modifier = Modifier.fillMaxSize()
                ) {
                    // Current month grid
                    data.months.getOrNull(currentMonthIndex)?.let { monthWithWeeks ->
                        MonthGrid(
                            monthWithWeeks = monthWithWeeks,
                            modifier = Modifier.weight(1f),
                            isSelectionMode = isSelectionMode,
                            selectedDayIds = selectedDayIds,
                            onDayClick = { daySlot ->
                                if (isSelectionMode) {
                                    viewModel.toggleDaySelection(daySlot.id)
                                } else {
                                    onNavigateToEdit(daySlot.id)
                                }
                            },
                            onDayLongPress = { daySlot ->
                                viewModel.toggleDaySelection(daySlot.id)
                            }
                        )
                    }

                    // Multi-select controls
                    if (isSelectionMode) {
                        MultiSelectControls(
                            selectedCount = selectedDayIds.size,
                            onMarkAll = viewModel::markSelectedAsCompleted,
                            onUnmarkAll = viewModel::clearSelectedCompleted,
                            onCancel = viewModel::clearSelection
                        )
                    }
                }
            }
        }
    }

    // Clear all dialog
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
    onDayClick: (DaySlot) -> Unit,
    onDayLongPress: (DaySlot) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier,
        contentPadding = PaddingValues(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        var i = 0
        items(monthWithWeeks.weeks) { weekWithDays ->
            Log.d("CalendarScreen", "Mostrando semana: $i")
            WeekRow(
                weekWithDays = weekWithDays,
                isSelectionMode = isSelectionMode,
                selectedDayIds = selectedDayIds,
                onDayClick = onDayClick,
                onDayLongPress = onDayLongPress
            )
            i = i + 1
        }
    }
}

@Composable
private fun WeekRow(
    weekWithDays: WeekWithDays,
    isSelectionMode: Boolean,
    selectedDayIds: Set<String>,
    onDayClick: (DaySlot) -> Unit,
    onDayLongPress: (DaySlot) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        weekWithDays.days.forEach { daySlot ->
            Log.d("CalendarScreen", "Mostrando dia: ${daySlot.dayOfWeek.displayName}")
            DayBox(
                daySlot = daySlot,
                isSelectionMode = isSelectionMode,
                isSelected = selectedDayIds.contains(daySlot.id),
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
    onDayClick: (DaySlot) -> Unit,
    onDayLongPress: (DaySlot) -> Unit,
    modifier: Modifier = Modifier
) {
    val iconRes = getCategoryIcon(daySlot.categories.firstOrNull())

    Card(
        modifier = modifier
            .aspectRatio(1f)
            .combinedClickable(
                onClick = { onDayClick(daySlot) },
                onLongClick = { onDayLongPress(daySlot) }
            ),
        colors = CardDefaults.cardColors(
            containerColor = when {
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
                    isSelected -> MaterialTheme.colorScheme.onPrimaryContainer
                    daySlot.completed -> MaterialTheme.colorScheme.onSecondaryContainer
                    else -> MaterialTheme.colorScheme.surface
                }
            )

            /*if (daySlot.completed) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = "Completado",
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(4.dp)
                        .size(16.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
            }*/
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
            //Text("$selectedCount seleccionados")
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TextButton(onClick = onMarkAll) {
                    Text("Marcar")
                }
                TextButton(onClick = onUnmarkAll) {
                    Text("Desmarcar")
                }
                TextButton(onClick = onCancel) {
                    Text("Cancelar")
                }
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