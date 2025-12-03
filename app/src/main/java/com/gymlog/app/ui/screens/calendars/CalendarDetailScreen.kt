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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.gymlog.app.R
import com.gymlog.app.data.local.entity.DayCategory
import com.gymlog.app.domain.model.DaySlot
import com.gymlog.app.domain.model.MonthWithWeeks
import com.gymlog.app.domain.model.WeekWithDays
import com.gymlog.app.ui.theme.*

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
        containerColor = HunterBlack,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        calendarWithMonths?.calendar?.name?.uppercase() ?: stringResource(R.string.calendar_detail_default_title),
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Black)
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = stringResource(R.string.common_back), tint = HunterTextPrimary)
                    }
                },
                actions = {
                    if (!isSelectionMode && swapSourceDayId == null) {
                        IconButton(onClick = viewModel::showClearAllDialog) {
                            Icon(Icons.Default.CleaningServices, contentDescription = stringResource(R.string.calendar_detail_cd_clean), tint = HunterTextSecondary)
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = HunterBlack,
                    titleContentColor = HunterTextPrimary
                )
            )
        },
        bottomBar = {
            if (swapSourceDayId != null) {
                // Barra de intercambio Hunter Style
                Surface(
                    color = HunterSurface,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = stringResource(R.string.calendar_detail_swap_prompt),
                            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                            color = ScreenColors.CalendarDetail.DaySwapSourceBorder // Usando color de swap como acento
                        )
                        TextButton(onClick = viewModel::cancelSwap) {
                            Text(stringResource(R.string.common_cancel), fontWeight = FontWeight.Bold, color = HunterTextPrimary)
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
            title = stringResource(R.string.calendar_detail_dialog_reset_title),
            text = stringResource(R.string.calendar_detail_dialog_reset_text),
            confirmText = stringResource(R.string.calendar_detail_cd_clean).uppercase(),
            onConfirm = viewModel::clearAllCompleted,
            onDismiss = viewModel::dismissClearAllDialog
        )
    }
}

@Composable
private fun DaysOfWeekHeader() {
    val days = listOf(
        stringResource(R.string.day_mon_short),
        stringResource(R.string.day_tue_short),
        stringResource(R.string.day_wed_short),
        stringResource(R.string.day_thu_short),
        stringResource(R.string.day_fri_short),
        stringResource(R.string.day_sat_short),
        stringResource(R.string.day_sun_short)
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(HunterSurface)
            .padding(vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        days.forEach { day ->
            Text(
                text = day,
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                color = HunterPrimary,
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
            Icon(Icons.Default.ChevronLeft, null, tint = if (canGoPrevious) HunterTextPrimary else HunterTextSecondary.copy(alpha = 0.5f))
        }

        Text(
            text = currentMonth?.month?.name?.uppercase() ?: "",
            style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Black, letterSpacing = 2.sp),
            color = HunterPrimary
        )

        IconButton(onClick = onNextMonth, enabled = canGoNext) {
            Icon(Icons.Default.ChevronRight, null, tint = if (canGoNext) HunterTextPrimary else HunterTextSecondary.copy(alpha = 0.5f))
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

    // Colores y Bordes dinámicos usando ScreenColors
    val borderColor = when {
        isSwapSource -> ScreenColors.CalendarDetail.DaySwapSourceBorder // Ámbar/Destacado
        isSelected -> ScreenColors.CalendarDetail.DaySelectedBorder // Azul
        daySlot.completed -> ScreenColors.CalendarDetail.DayCompletedIcon // Verde/RankB
        else -> HunterPrimary.copy(alpha = 0.1f) // Normal
    }

    val backgroundColor = when {
        daySlot.completed -> ScreenColors.CalendarDetail.DayCompletedBg // Fondo sutil verde
        else -> HunterSurface
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
                tint = if (daySlot.completed) ScreenColors.CalendarDetail.DayCompletedIcon else HunterTextSecondary
            )
        } else {
            // Día vacío (Punto discreto)
            Box(
                modifier = Modifier
                    .size(4.dp)
                    .background(ScreenColors.CalendarDetail.DayEmptyDot, CircleShape)
            )
        }
    }
}

@Composable
private fun MultiSelectControls(
    onMarkAll: () -> Unit,
    onUnmarkAll: () -> Unit,
    onCancel: () -> Unit
) {
    Surface(
        color = ScreenColors.CalendarDetail.MultiSelectBarBg,
        tonalElevation = 8.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            TextButton(onClick = onMarkAll) {
                Text(stringResource(R.string.calendar_detail_btn_mark), color = HunterPrimary, fontWeight = FontWeight.Bold)
            }
            TextButton(onClick = onUnmarkAll) {
                Text(stringResource(R.string.calendar_detail_btn_unmark), color = HunterSecondary)
            }
            TextButton(onClick = onCancel) {
                Text(stringResource(R.string.calendar_detail_btn_ready), color = HunterTextPrimary)
            }
        }
    }
}

@DrawableRes
private fun getCategoryIcon(category: DayCategory?): Int {
    return when (category) {
        DayCategory.BACK -> R.drawable.ic_espalda
        DayCategory.BICEPS -> R.drawable.ic_biceps
        DayCategory.LEGS -> R.drawable.ic_pierna
        DayCategory.GLUTES -> R.drawable.ic_gluteos
        DayCategory.CHEST -> R.drawable.ic_torso
        DayCategory.TRICEPS -> R.drawable.ic_triceps
        DayCategory.SHOULDERS -> R.drawable.ic_hombros
        DayCategory.CARDIO -> R.drawable.ic_cardio
        DayCategory.REST -> R.drawable.ic_rest
        DayCategory.FULL_BODY -> R.drawable.ic_fullbody
        else -> R.drawable.ic_exercise_placeholder
    }
}