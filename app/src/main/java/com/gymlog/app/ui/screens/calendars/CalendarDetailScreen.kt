package com.gymlog.app.ui.screens.calendars

import androidx.annotation.DrawableRes
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalendarDetailScreen(
    onNavigateBack: () -> Unit,
    onNavigateToEdit: (String) -> Unit,
    onNavigateToExercise: (String) -> Unit = {},
    viewModel: CalendarDetailViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        containerColor = HunterBlack,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        uiState.calendarWithMonths?.calendar?.name?.uppercase() ?: stringResource(R.string.calendar_detail_default_title),
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Black)
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, stringResource(R.string.common_back), tint = HunterTextPrimary)
                    }
                },
                actions = {
                    if (!uiState.isSelectionMode && uiState.toolAction == ToolAction.NONE) {
                        IconButton(onClick = viewModel::showClearAllDialog) {
                            Icon(Icons.Default.CleaningServices, stringResource(R.string.calendar_detail_cd_clean), tint = HunterTextSecondary)
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = HunterBlack)
            )
        },
        bottomBar = {
            if (uiState.toolAction != ToolAction.NONE) {
                ActiveToolBar(
                    uiState = uiState,
                    onCancel = viewModel::cancelTool,
                    onConfirmSource = viewModel::confirmSourceSelection,
                    onApply = viewModel::executeTool
                )
            } else if (uiState.isSelectionMode) {
                MultiSelectControls(
                    onMarkAll = viewModel::markSelectedAsCompleted,
                    onUnmarkAll = viewModel::clearSelectedCompleted,
                    onCancel = viewModel::clearSelection
                )
            } else {
                ToolSelectionBar(onToolSelected = viewModel::startTool)
            }
        }
    ) { paddingValues ->
        uiState.calendarWithMonths?.let { data ->
            val currentMonth = data.months.getOrNull(uiState.currentMonthIndex)
            val currentMonthId = currentMonth?.month?.id ?: ""

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                MonthNavigation(
                    currentMonth = currentMonth,
                    onPreviousMonth = { viewModel.changeMonth(-1) },
                    onNextMonth = { viewModel.changeMonth(1) },
                    canGoNext = uiState.currentMonthIndex < data.months.size - 1,
                    canGoPrevious = uiState.currentMonthIndex > 0
                )

                DaysOfWeekHeader()

                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    currentMonth?.let { monthWithWeeks ->
                        items(monthWithWeeks.weeks) { weekWithDays ->
                            WeekRow(
                                weekWithDays = weekWithDays,
                                currentMonthId = currentMonthId,
                                uiState = uiState,
                                onDayClick = { daySlot ->
                                    viewModel.onDayClick(daySlot.id, weekWithDays.week.id, currentMonthId) { id -> onNavigateToEdit(id) }
                                },
                                onDayLongPress = { daySlot -> viewModel.onDayLongPress(daySlot.id) }
                            )
                        }
                    }
                }
            }
        }
    }

    if (uiState.showClearAllDialog) {
        HunterConfirmDialog(
            title = stringResource(R.string.calendar_detail_dialog_reset_title),
            text = stringResource(R.string.calendar_detail_dialog_reset_text),
            confirmText = stringResource(R.string.calendar_detail_cd_clean).uppercase(),
            onConfirm = viewModel::clearAllCompleted,
            onDismiss = viewModel::dismissClearAllDialog
        )
    }
}

// --- BARRAS INFERIORES DE HERRAMIENTAS ---

@Composable
private fun ToolSelectionBar(onToolSelected: (ToolAction) -> Unit) {
    Surface(color = HunterSurface, modifier = Modifier.fillMaxWidth()) {
        LazyRow(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item { ToolChip("Mover Día", Icons.Default.SwapHoriz) { onToolSelected(ToolAction.MOVE_DAY) } }
            item { ToolChip("Copiar Días", Icons.Default.ContentCopy) { onToolSelected(ToolAction.COPY_DAYS) } }
            item { ToolChip("Mover Sem.", Icons.Default.LowPriority) { onToolSelected(ToolAction.MOVE_WEEK) } }
            item { ToolChip("Copiar Sem.", Icons.Default.LibraryAdd) { onToolSelected(ToolAction.COPY_WEEKS) } }
            item { ToolChip("Mover Mes", Icons.Default.Event) { onToolSelected(ToolAction.MOVE_MONTH) } }
            item { ToolChip("Copiar Mes", Icons.Default.EventAvailable) { onToolSelected(ToolAction.COPY_MONTH) } }
        }
    }
}

@Composable
private fun ToolChip(text: String, icon: androidx.compose.ui.graphics.vector.ImageVector, onClick: () -> Unit) {
    Surface(
        color = HunterBlack,
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, HunterPrimary.copy(alpha = 0.5f)),
        modifier = Modifier.clickable(onClick = onClick)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
        ) {
            Icon(icon, null, tint = HunterPrimary, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text(text, color = HunterTextPrimary, style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold))
        }
    }
}

@Composable
private fun ActiveToolBar(
    uiState: CalendarUiState,
    onCancel: () -> Unit,
    onConfirmSource: () -> Unit,
    onApply: () -> Unit
) {
    Surface(color = HunterSurface, modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                if (uiState.actionPhase == ActionPhase.SELECT_SOURCE) {
                    Text("Seleccionar Origen", style = MaterialTheme.typography.labelLarge, color = HunterPrimary, fontWeight = FontWeight.Bold)
                    Text("${uiState.sourceSelections.size} seleccionados", style = MaterialTheme.typography.labelSmall, color = HunterTextSecondary)
                } else {
                    Text("Seleccionar Destino", style = MaterialTheme.typography.labelLarge, color = HunterCyan, fontWeight = FontWeight.Bold)
                    Text("${uiState.targetSelections.size} / ${uiState.sourceSelections.size}", style = MaterialTheme.typography.labelSmall, color = HunterTextSecondary)
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TextButton(onClick = onCancel) {
                    Text("Cancelar", color = HunterTextSecondary)
                }

                if (uiState.actionPhase == ActionPhase.SELECT_SOURCE && uiState.sourceSelections.isNotEmpty() && !uiState.toolAction.isMove) {
                    Button(onClick = onConfirmSource, colors = ButtonDefaults.buttonColors(containerColor = HunterPrimary)) {
                        Text("Siguiente", color = HunterBlack, fontWeight = FontWeight.Bold)
                    }
                }

                if (uiState.actionPhase == ActionPhase.SELECT_TARGET && uiState.targetSelections.size == uiState.sourceSelections.size && !uiState.toolAction.isMove) {
                    Button(onClick = onApply, colors = ButtonDefaults.buttonColors(containerColor = HunterCyan)) {
                        Text("Aplicar", color = HunterBlack, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

// --- RESTO DE LA UI ORIGINAL ---

@Composable
private fun DaysOfWeekHeader() {
    val days = listOf("Lun", "Mar", "Mié", "Jue", "Vie", "Sáb", "Dom")
    Row(
        modifier = Modifier.fillMaxWidth().background(HunterSurface).padding(vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        days.forEach { day ->
            Text(day, style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold), color = HunterPrimary, modifier = Modifier.weight(1f), textAlign = TextAlign.Center)
        }
    }
}

@Composable
private fun MonthNavigation(currentMonth: MonthWithWeeks?, onPreviousMonth: () -> Unit, onNextMonth: () -> Unit, canGoNext: Boolean, canGoPrevious: Boolean) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onPreviousMonth, enabled = canGoPrevious) { Icon(Icons.Default.ChevronLeft, null, tint = if (canGoPrevious) HunterTextPrimary else HunterTextSecondary.copy(alpha = 0.5f)) }
        Text(currentMonth?.month?.name?.uppercase() ?: "", style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Black, letterSpacing = 2.sp), color = HunterPrimary)
        IconButton(onClick = onNextMonth, enabled = canGoNext) { Icon(Icons.Default.ChevronRight, null, tint = if (canGoNext) HunterTextPrimary else HunterTextSecondary.copy(alpha = 0.5f)) }
    }
}

@Composable
private fun WeekRow(
    weekWithDays: WeekWithDays,
    currentMonthId: String,
    uiState: CalendarUiState,
    onDayClick: (DaySlot) -> Unit,
    onDayLongPress: (DaySlot) -> Unit
) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        weekWithDays.days.forEach { daySlot ->

            val selectionId = when (uiState.toolAction) {
                ToolAction.MOVE_DAY, ToolAction.COPY_DAYS -> daySlot.id
                ToolAction.MOVE_WEEK, ToolAction.COPY_WEEKS -> weekWithDays.week.id
                ToolAction.MOVE_MONTH, ToolAction.COPY_MONTH -> currentMonthId
                else -> daySlot.id
            }

            val isSource = uiState.toolAction != ToolAction.NONE && uiState.sourceSelections.contains(selectionId)
            val isTarget = uiState.toolAction != ToolAction.NONE && uiState.actionPhase == ActionPhase.SELECT_TARGET && uiState.targetSelections.contains(selectionId)
            val isClassicSelected = uiState.isSelectionMode && uiState.selectedDayIds.contains(daySlot.id)

            DayBox(
                daySlot = daySlot,
                isSelected = isClassicSelected,
                isSource = isSource,
                isTarget = isTarget,
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
    isSource: Boolean,
    isTarget: Boolean,
    onDayClick: (DaySlot) -> Unit,
    onDayLongPress: (DaySlot) -> Unit,
    modifier: Modifier = Modifier
) {
    val category = daySlot.categories.firstOrNull()

    val borderColor = when {
        isTarget -> HunterCyan // Destino
        isSource -> HunterSecondary // Origen (Ambar)
        isSelected -> ScreenColors.CalendarDetail.DaySelectedBorder
        daySlot.completed -> ScreenColors.CalendarDetail.DayCompletedIcon
        else -> HunterPrimary.copy(alpha = 0.1f)
    }

    val backgroundColor = when {
        daySlot.completed && !isSource && !isTarget -> ScreenColors.CalendarDetail.DayCompletedBg
        else -> HunterSurface
    }

    val glowModifier = if (isSource || isTarget || isSelected) {
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
            Icon(
                painter = painterResource(id = getCategoryIcon(category)),
                contentDescription = null,
                modifier = Modifier.size(24.dp),
                tint = if (daySlot.completed && !isSource && !isTarget) ScreenColors.CalendarDetail.DayCompletedIcon else HunterTextSecondary
            )
        } else {
            Box(modifier = Modifier.size(4.dp).background(ScreenColors.CalendarDetail.DayEmptyDot, CircleShape))
        }
    }
}

@Composable
private fun MultiSelectControls(onMarkAll: () -> Unit, onUnmarkAll: () -> Unit, onCancel: () -> Unit) {
    Surface(color = ScreenColors.CalendarDetail.MultiSelectBarBg, tonalElevation = 8.dp) {
        Row(modifier = Modifier.fillMaxWidth().padding(8.dp), horizontalArrangement = Arrangement.SpaceEvenly) {
            TextButton(onClick = onMarkAll) { Text(stringResource(R.string.calendar_detail_btn_mark), color = HunterPrimary, fontWeight = FontWeight.Bold) }
            TextButton(onClick = onUnmarkAll) { Text(stringResource(R.string.calendar_detail_btn_unmark), color = HunterSecondary) }
            TextButton(onClick = onCancel) { Text(stringResource(R.string.calendar_detail_btn_ready), color = HunterTextPrimary) }
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