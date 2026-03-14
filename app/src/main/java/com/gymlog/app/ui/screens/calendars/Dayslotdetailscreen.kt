package com.gymlog.app.ui.screens.calendars

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInParent
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.hilt.navigation.compose.hiltViewModel
import com.gymlog.app.R
import com.gymlog.app.data.local.entity.DayCategory
import com.gymlog.app.domain.model.Exercise
import com.gymlog.app.domain.model.TrainingAssignment
import com.gymlog.app.ui.theme.*
import com.gymlog.app.ui.util.UiMappers

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DaySlotDetailScreen(
    onNavigateBack: () -> Unit,
    onNavigateToExercise: (String) -> Unit,
    onNavigateToTraining: (String) -> Unit,
    viewModel: DaySlotDetailViewModel = hiltViewModel()
) {
    val daySlot by viewModel.daySlot.collectAsState()
    val selectedCategories by viewModel.selectedCategories.collectAsState()
    val selectedExercisesWithSets by viewModel.selectedExercisesWithSets.collectAsState()
    val filteredExercises by viewModel.filteredExercises.collectAsState()
    val completed by viewModel.completed.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val navigateBack by viewModel.navigateBack.collectAsState()
    val navigateToTraining by viewModel.navigateToTraining.collectAsState()
    val isCategoriesExpanded by viewModel.isCategoriesExpanded.collectAsState()
    val isExercisesExpanded by viewModel.isExercisesExpanded.collectAsState()

    // Estado de Edición
    val editingAssignmentItem by viewModel.editingAssignmentItem.collectAsState()

    var showBottomSheet by remember { mutableStateOf(false) }
    val bottomSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    LaunchedEffect(navigateBack) {
        if (navigateBack) {
            onNavigateBack()
            viewModel.resetNavigation()
        }
    }

    LaunchedEffect(navigateToTraining) {
        if (navigateToTraining && daySlot != null) {
            onNavigateToTraining(daySlot!!.id)
            viewModel.resetNavigation()
        }
    }

    Scaffold(
        containerColor = HunterBlack,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        daySlot?.dayOfWeek?.let { stringResource(UiMappers.getDisplayNameRes(it)).uppercase() }
                            ?: stringResource(R.string.day_slot_default_title),
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Black, letterSpacing = 1.sp)
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = stringResource(R.string.common_back), tint = HunterTextPrimary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = HunterBlack, titleContentColor = HunterTextPrimary)
            )
        }
    ) { paddingValues ->
        if (isLoading) {
            Box(modifier = Modifier.fillMaxSize().padding(paddingValues), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = HunterPrimary)
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                DaySlotStatus(completed = completed, onToggle = viewModel::toggleCompleted)

                if (selectedExercisesWithSets.isNotEmpty()) {
                    HunterButton(
                        text = "Iniciar Entrenamiento",
                        onClick = viewModel::startTraining,
                        color = if (completed) HunterCyan else HunterPurple,
                        textColor = HunterTextPrimary,
                        icon = { Icon(Icons.Default.PlayArrow, null, tint = HunterTextPrimary) }
                    )
                }

                HunterExpandableSection(
                    title = "Categoría del día",
                    isExpanded = isCategoriesExpanded,
                    onToggle = viewModel::toggleCategoriesExpansion
                ) {
                    CategorySelectorGrid(selectedCategories = selectedCategories, onToggleCategory = viewModel::toggleCategory)
                }

                HunterExpandableSection(
                    title = "Ejercicios y Variantes",
                    isExpanded = isExercisesExpanded,
                    onToggle = viewModel::toggleExercisesExpansion,
                    badgeCount = selectedExercisesWithSets.size
                ) {
                    LoadoutContent(
                        exercisesWithSets = selectedExercisesWithSets,
                        hasCategories = selectedCategories.isNotEmpty(),
                        isCardioOrRest = selectedCategories.all { it == DayCategory.CARDIO || it == DayCategory.REST },
                        onAddExercise = { showBottomSheet = true },
                        onExerciseClick = onNavigateToExercise,
                        onExerciseLongClick = viewModel::startEditingAssignment, // NUEVO
                        onRemoveExercise = viewModel::removeExercise,
                        onMoveExercise = viewModel::moveExercise
                    )
                }

                HunterButton(
                    text = "Guardar Día",
                    onClick = viewModel::saveDaySlot,
                    enabled = !isLoading,
                    color = HunterPrimary
                )
                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }

    // Modal para Añadir Nuevo Ejercicio
    if (showBottomSheet) {
        ModalBottomSheet(
            onDismissRequest = { showBottomSheet = false },
            sheetState = bottomSheetState,
            containerColor = HunterSurface,
            contentColor = HunterTextPrimary
        ) {
            HunterInventorySheet(
                exercises = filteredExercises,
                onAddExercises = { exerciseId, setIds ->
                    viewModel.addExercises(exerciseId, setIds)
                    showBottomSheet = false
                },
                onDismiss = { showBottomSheet = false }
            )
        }
    }

    // Modal para Editar las Series de un Ejercicio ya asignado
    if (editingAssignmentItem != null) {
        ModalBottomSheet(
            onDismissRequest = viewModel::cancelEditingAssignment,
            sheetState = bottomSheetState,
            containerColor = HunterSurface,
            contentColor = HunterTextPrimary
        ) {
            HunterEditAssignmentSheet(
                item = editingAssignmentItem!!,
                onSave = { newSets ->
                    viewModel.updateAssignmentSets(editingAssignmentItem!!.assignment, newSets)
                },
                onRemove = {
                    viewModel.confirmRemoveAssignment(editingAssignmentItem!!.assignment)
                },
                onDismiss = viewModel::cancelEditingAssignment
            )
        }
    }
}

@Composable
private fun DaySlotStatus(completed: Boolean, onToggle: () -> Unit) {
    HunterCard(onClick = onToggle, modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(text = "Estado", style = MaterialTheme.typography.labelSmall, color = HunterTextSecondary)
                Text(
                    text = if (completed) stringResource(R.string.day_slot_status_completed) else stringResource(R.string.day_slot_status_pending),
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Black, letterSpacing = 1.sp),
                    color = if (completed) ScreenColors.DaySlotDetail.StatusCompleted else ScreenColors.DaySlotDetail.StatusPending
                )
            }
            Box(
                modifier = Modifier
                    .size(width = 60.dp, height = 32.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(if (completed) HunterCyan.copy(alpha = 0.2f) else HunterSurface)
                    .border(1.dp, if (completed) HunterCyan else HunterPrimary.copy(0.3f), RoundedCornerShape(16.dp))
            ) {
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .align(if (completed) Alignment.CenterEnd else Alignment.CenterStart)
                        .padding(4.dp)
                        .clip(CircleShape)
                        .background(if (completed) HunterCyan else HunterTextSecondary)
                )
            }
        }
    }
}

@Composable
private fun HunterExpandableSection(title: String, isExpanded: Boolean, onToggle: () -> Unit, badgeCount: Int? = null, content: @Composable () -> Unit) {
    Column(
        modifier = Modifier.fillMaxWidth().border(1.dp, ScreenColors.DaySlotDetail.CardBorder, RoundedCornerShape(16.dp)).clip(RoundedCornerShape(16.dp)).background(HunterSurface)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().clickable { onToggle() }.background(HunterSurface).padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(text = title, style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold), color = HunterPrimary)
                if (badgeCount != null && badgeCount > 0) {
                    Spacer(modifier = Modifier.width(8.dp))
                    Badge(containerColor = HunterPrimary) { Text("$badgeCount") }
                }
            }
            Icon(
                imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                contentDescription = null,
                tint = HunterTextSecondary
            )
        }
        if (isExpanded) Box(modifier = Modifier.padding(16.dp)) { content() }
    }
}

@Composable
private fun CategorySelectorGrid(selectedCategories: Set<DayCategory>, onToggleCategory: (DayCategory) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        val categories = DayCategory.values().toList()
        categories.chunked(2).forEach { rowCategories ->
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                rowCategories.forEach { category ->
                    val isSelected = selectedCategories.contains(category)
                    Surface(
                        modifier = Modifier.weight(1f).height(40.dp).clickable { onToggleCategory(category) },
                        shape = RoundedCornerShape(8.dp),
                        color = if (isSelected) HunterPrimary.copy(alpha = 0.2f) else HunterBlack,
                        border = BorderStroke(1.dp, if (isSelected) HunterPrimary else HunterPrimary.copy(0.3f))
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = stringResource(UiMappers.getDisplayNameRes(category)).uppercase(),
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = if (isSelected) HunterPrimary else HunterTextSecondary
                            )
                        }
                    }
                }
                if (rowCategories.size == 1) Spacer(modifier = Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun LoadoutContent(
    exercisesWithSets: List<ExerciseWithSelectedSets>,
    hasCategories: Boolean,
    isCardioOrRest: Boolean,
    onAddExercise: () -> Unit,
    onExerciseClick: (String) -> Unit,
    onExerciseLongClick: (ExerciseWithSelectedSets) -> Unit,
    onRemoveExercise: (TrainingAssignment) -> Unit,
    onMoveExercise: (Int, Int) -> Unit
) {
    when {
        !hasCategories -> Text("Añade una categoría primero.", style = MaterialTheme.typography.bodyMedium, color = HunterTextSecondary)
        isCardioOrRest -> Text(stringResource(R.string.day_slot_msg_cardio_rest), style = MaterialTheme.typography.bodyMedium, color = HunterCyan)
        else -> {
            Column(modifier = Modifier.fillMaxWidth()) {
                if (exercisesWithSets.isNotEmpty()) {
                    DraggableLoadoutList(
                        items = exercisesWithSets,
                        onExerciseClick = onExerciseClick,
                        onExerciseLongClick = onExerciseLongClick, // Pasamos el long click
                        onRemoveExercise = onRemoveExercise,
                        onMoveExercise = onMoveExercise
                    )
                } else {
                    Text(
                        stringResource(R.string.day_slot_msg_empty),
                        style = MaterialTheme.typography.bodySmall,
                        color = HunterTextSecondary.copy(alpha = 0.5f),
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                }
                Spacer(modifier = Modifier.height(16.dp))
                OutlinedButton(
                    onClick = onAddExercise,
                    modifier = Modifier.fillMaxWidth().height(50.dp),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, HunterPrimary.copy(alpha = 0.5f)),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = HunterPrimary)
                ) {
                    Icon(Icons.Default.Add, null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Añadir Ejercicio", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun DraggableLoadoutList(
    items: List<ExerciseWithSelectedSets>,
    onExerciseClick: (String) -> Unit,
    onExerciseLongClick: (ExerciseWithSelectedSets) -> Unit,
    onRemoveExercise: (TrainingAssignment) -> Unit,
    onMoveExercise: (Int, Int) -> Unit
) {
    var draggedIndex by remember { mutableStateOf<Int?>(null) }
    var targetIndex by remember { mutableStateOf<Int?>(null) }
    val cardPositions = remember { mutableStateMapOf<Int, Float>() }
    var currentDragY by remember { mutableFloatStateOf(0f) }

    Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        items.forEachIndexed { index, item ->
            HunterDraggableCard(
                item = item,
                index = index,
                isDragged = draggedIndex == index,
                isDraggedOver = targetIndex == index,
                onClick = { onExerciseClick(item.exercise.id) },
                onLongClick = { onExerciseLongClick(item) }, // Recibimos el long click
                onRemove = { onRemoveExercise(item.assignment) },
                onDragStart = { positionY -> draggedIndex = index; targetIndex = index; currentDragY = positionY },
                onDragEnd = {
                    draggedIndex?.let { from -> targetIndex?.let { to -> if (from != to) onMoveExercise(from, to) } }
                    draggedIndex = null; targetIndex = null; currentDragY = 0f
                },
                onDrag = { dragAmount ->
                    currentDragY += dragAmount
                    draggedIndex?.let { draggedIdx ->
                        val draggedCenter = currentDragY + 35f
                        var closestIndex = draggedIdx
                        var minDistance = Float.MAX_VALUE
                        cardPositions.forEach { (idx, posY) ->
                            if (idx != draggedIdx) {
                                val cardCenter = posY + 35f
                                val distance = kotlin.math.abs(draggedCenter - cardCenter)
                                if (distance < minDistance && distance < 60f) { minDistance = distance; closestIndex = idx }
                            }
                        }
                        if (closestIndex != targetIndex) targetIndex = closestIndex
                    }
                },
                onPositionCalculated = { posY -> cardPositions[index] = posY }
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun HunterDraggableCard(
    item: ExerciseWithSelectedSets,
    index: Int,
    isDragged: Boolean,
    isDraggedOver: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    onRemove: () -> Unit,
    onDragStart: (Float) -> Unit,
    onDragEnd: () -> Unit,
    onDrag: (Float) -> Unit,
    onPositionCalculated: (Float) -> Unit
) {
    var localDragOffset by remember { mutableFloatStateOf(0f) }
    var cardPositionY by remember { mutableFloatStateOf(0f) }
    val density = LocalDensity.current

    val backgroundColor = when {
        isDragged -> HunterPrimary.copy(alpha = 0.2f)
        isDraggedOver -> MaterialTheme.colorScheme.secondaryContainer
        else -> HunterBlack
    }
    val borderColor = if (isDragged) HunterPrimary else HunterPrimary.copy(alpha = 0.2f)
    val zIndex = if (isDragged) 10f else 0f

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .zIndex(zIndex)
            .offset(y = with(density) { localDragOffset.toDp() })
            .onGloballyPositioned { coordinates -> cardPositionY = coordinates.positionInParent().y; onPositionCalculated(cardPositionY) },
        shape = RoundedCornerShape(12.dp),
        color = backgroundColor,
        border = BorderStroke(1.dp, borderColor)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Icon(
                imageVector = Icons.Default.DragHandle,
                contentDescription = null,
                tint = HunterTextSecondary,
                modifier = Modifier
                    .size(24.dp)
                    .pointerInput(item.assignment) {
                        detectDragGesturesAfterLongPress(
                            onDragStart = { onDragStart(cardPositionY); localDragOffset = 0f },
                            onDragEnd = { onDragEnd(); localDragOffset = 0f },
                            onDragCancel = { onDragEnd(); localDragOffset = 0f },
                            onDrag = { change, dragAmount -> change.consume(); localDragOffset += dragAmount.y; onDrag(dragAmount.y) }
                        )
                    }
            )

            Spacer(modifier = Modifier.width(16.dp))

            // APLICAMOS COMBINED CLICKABLE PARA DETECTAR PULSACIÓN LARGA
            Column(
                modifier = Modifier
                    .weight(1f)
                    .combinedClickable(
                        onClick = onClick,
                        onLongClick = onLongClick
                    )
            ) {
                Text(
                    text = item.exercise.name.uppercase(),
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                    color = HunterTextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                if (item.selectedSets.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    item.selectedSets.forEachIndexed { idx, set ->
                        val repsText = if (set.minReps == set.maxReps) "${set.minReps}" else "${set.minReps}-${set.maxReps}"
                        val rirText = if (set.minRir != null && set.maxRir != null) {
                            if (set.minRir == set.maxRir) " | RIR ${set.minRir}" else " | RIR ${set.minRir}-${set.maxRir}"
                        } else ""

                        Text(
                            text = "Var ${idx + 1}: ${set.series}×$repsText @ ${set.weightKg}kg$rirText",
                            style = MaterialTheme.typography.labelSmall,
                            color = HunterPrimary
                        )
                    }
                } else {
                    Text(
                        text = "Ninguna variante configurada",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }

            IconButton(onClick = onRemove) {
                Icon(Icons.Default.Close, stringResource(R.string.common_delete), tint = HunterTextSecondary.copy(alpha = 0.5f), modifier = Modifier.size(20.dp))
            }
        }
    }
}

// === NUEVO MODAL DE EDICIÓN ===
@Composable
private fun HunterEditAssignmentSheet(
    item: ExerciseWithSelectedSets,
    onSave: (List<String>) -> Unit,
    onRemove: () -> Unit,
    onDismiss: () -> Unit
) {
    var selectedVariantIds by remember { mutableStateOf(item.selectedSets.map { it.id }.toSet()) }
    var showWarning by remember { mutableStateOf(false) }

    if (showWarning) {
        AlertDialog(
            onDismissRequest = { showWarning = false },
            containerColor = HunterSurface,
            title = { Text("¿Eliminar Ejercicio?", color = HunterTextPrimary, fontWeight = FontWeight.Bold) },
            text = { Text("Has desmarcado todas las variantes. Si guardas, el ejercicio se eliminará de este día.", color = HunterTextSecondary) },
            confirmButton = {
                Button(onClick = { showWarning = false; onRemove() }, colors = ButtonDefaults.buttonColors(containerColor = ScreenColors.Global.ErrorRed)) {
                    Text("Eliminar", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showWarning = false }) { Text("Cancelar", color = HunterTextPrimary) }
            }
        )
    }

    Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Modificar Variantes", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Black), color = HunterTextPrimary)
            IconButton(onClick = onDismiss) {
                Icon(Icons.Default.Close, stringResource(R.string.common_close), tint = HunterTextPrimary)
            }
        }

        Spacer(modifier = Modifier.height(12.dp))
        Text(item.exercise.name.uppercase(), style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold), color = HunterPrimary)
        Spacer(modifier = Modifier.height(16.dp))

        if (item.exercise.sets.isEmpty()) {
            Text("Este ejercicio no tiene variantes creadas.", color = HunterTextSecondary)
        }

        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.weight(1f, fill = false).heightIn(max = 400.dp)
        ) {
            itemsIndexed(item.exercise.sets) { _, set ->
                val isSelected = selectedVariantIds.contains(set.id)
                val repsText = if (set.minReps == set.maxReps) "${set.minReps}" else "${set.minReps}-${set.maxReps}"
                val rirText = if (set.minRir != null && set.maxRir != null) {
                    if (set.minRir == set.maxRir) " | RIR ${set.minRir}" else " | RIR ${set.minRir}-${set.maxRir}"
                } else ""

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (isSelected) HunterPrimary.copy(alpha = 0.15f) else HunterBlack)
                        .border(1.dp, if (isSelected) HunterPrimary else HunterPrimary.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                        .clickable {
                            selectedVariantIds = if (isSelected) selectedVariantIds - set.id else selectedVariantIds + set.id
                        }
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked = isSelected,
                        onCheckedChange = null,
                        colors = CheckboxDefaults.colors(checkedColor = HunterPrimary, uncheckedColor = HunterTextSecondary)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        "${set.series}×$repsText @ ${set.weightKg}kg$rirText",
                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                        color = if (isSelected) HunterPrimary else HunterTextSecondary
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        HunterButton(
            text = "Guardar Cambios",
            onClick = {
                if (selectedVariantIds.isEmpty()) showWarning = true
                else onSave(selectedVariantIds.toList())
            },
            color = HunterPrimary
        )
        Spacer(modifier = Modifier.height(32.dp))
    }
}

// === MODAL DE INVENTARIO (ORIGINAL) ===
@Composable
private fun HunterInventorySheet(
    exercises: List<Exercise>,
    onAddExercises: (String, List<String>) -> Unit,
    onDismiss: () -> Unit
) {
    var expandedExerciseId by remember { mutableStateOf<String?>(null) }
    var selectedVariantIds by remember { mutableStateOf(setOf<String>()) }

    Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Seleccionar Ejercicio y Variantes", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Black), color = HunterTextPrimary)
            IconButton(onClick = onDismiss) {
                Icon(Icons.Default.Close, stringResource(R.string.common_close), tint = HunterTextPrimary)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (exercises.isEmpty()) {
            Text(stringResource(R.string.day_slot_sheet_no_exercises), color = HunterTextSecondary)
        }

        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.weight(1f, fill = false).heightIn(max = 400.dp)
        ) {
            itemsIndexed(exercises) { _, exercise ->
                val isExpanded = expandedExerciseId == exercise.id
                val hasSets = exercise.sets.isNotEmpty()

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = HunterBlack,
                    border = BorderStroke(1.dp, HunterPrimary.copy(alpha = 0.2f)),
                    modifier = Modifier.clickable {
                        if (hasSets) {
                            if (isExpanded) {
                                expandedExerciseId = null
                                selectedVariantIds = emptySet()
                            } else {
                                expandedExerciseId = exercise.id
                                selectedVariantIds = emptySet()
                            }
                        }
                    }
                ) {
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(text = exercise.name, style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold), color = HunterTextPrimary)
                            Icon(
                                imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                contentDescription = null,
                                tint = if (hasSets) HunterPrimary else HunterTextSecondary.copy(alpha = 0.3f)
                            )
                        }

                        if (isExpanded) {
                            Divider(color = HunterPrimary.copy(alpha = 0.1f))
                            exercise.sets.forEach { set ->
                                val isSelected = selectedVariantIds.contains(set.id)
                                val repsText = if (set.minReps == set.maxReps) "${set.minReps}" else "${set.minReps}-${set.maxReps}"
                                val rirText = if (set.minRir != null && set.maxRir != null) {
                                    if (set.minRir == set.maxRir) " | RIR ${set.minRir}" else " | RIR ${set.minRir}-${set.maxRir}"
                                } else ""

                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            selectedVariantIds = if (isSelected) selectedVariantIds - set.id else selectedVariantIds + set.id
                                        }
                                        .padding(horizontal = 16.dp, vertical = 8.dp)
                                        .background(if (isSelected) HunterPrimary.copy(alpha = 0.1f) else Color.Transparent),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Checkbox(
                                        checked = isSelected,
                                        onCheckedChange = null,
                                        colors = CheckboxDefaults.colors(checkedColor = HunterPrimary, uncheckedColor = HunterTextSecondary)
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Text(
                                        "${set.series}×$repsText @ ${set.weightKg}kg$rirText",
                                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                                        color = if (isSelected) HunterPrimary else HunterTextSecondary
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        if (expandedExerciseId != null && selectedVariantIds.isNotEmpty()) {
            Spacer(modifier = Modifier.height(16.dp))
            HunterButton(
                text = "Añadir ${selectedVariantIds.size} variantes",
                onClick = { onAddExercises(expandedExerciseId!!, selectedVariantIds.toList()) },
                color = HunterPrimary
            )
        }
        Spacer(modifier = Modifier.height(32.dp))
    }
}