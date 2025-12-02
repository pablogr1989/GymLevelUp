package com.gymlog.app.ui.screens.calendars

import android.util.Log
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInParent
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.hilt.navigation.compose.hiltViewModel
import com.gymlog.app.data.local.entity.DayCategory
import com.gymlog.app.data.local.entity.MuscleGroup
import com.gymlog.app.domain.model.Exercise
import com.gymlog.app.domain.model.Set as GymSet
import com.gymlog.app.ui.theme.*

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

    var showBottomSheet by remember { mutableStateOf(false) }

    // CORREGIDO: rememberModalBottomSheetState no lleva containerColor
    val bottomSheetState = rememberModalBottomSheetState(
        skipPartiallyExpanded = true
    )

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
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        daySlot?.dayOfWeek?.displayName?.uppercase() ?: "PLANIFICACIÓN",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Black,
                            letterSpacing = 1.sp
                        )
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Volver", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = Color.White
                )
            )
        }
    ) { paddingValues ->
        if (isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
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
                // 1. ESTADO DE LA MISIÓN
                HunterMissionStatus(
                    completed = completed,
                    onToggle = viewModel::toggleCompleted
                )

                // 2. BOTÓN DE ACCIÓN (INICIAR)
                if (selectedExercisesWithSets.isNotEmpty()) {
                    HunterButton(
                        text = "INICIAR SECUENCIA",
                        onClick = viewModel::startTraining,
                        color = if (completed) HunterCyan else HunterPurple,
                        textColor = Color.White,
                        icon = {
                            Icon(Icons.Default.PlayArrow, null, tint = Color.White)
                        }
                    )
                }

                // 3. CLASE DE COMBATE (CATEGORÍAS)
                HunterExpandableSection(
                    title = "CLASE DE COMBATE",
                    isExpanded = isCategoriesExpanded,
                    onToggle = viewModel::toggleCategoriesExpansion
                ) {
                    CategorySelectorGrid(
                        selectedCategories = selectedCategories,
                        onToggleCategory = viewModel::toggleCategory
                    )
                }

                // 4. LOADOUT (EJERCICIOS)
                HunterExpandableSection(
                    title = "EQUIPAMIENTO (EJERCICIOS)",
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
                        onRemoveExercise = viewModel::removeExercise,
                        onMoveExercise = viewModel::moveExercise
                    )
                }

                // 5. GUARDAR
                HunterButton(
                    text = "CONFIRMAR CAMBIOS",
                    onClick = viewModel::saveDaySlot,
                    enabled = !isLoading,
                    color = HunterPrimary
                )

                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }

    if (showBottomSheet) {
        ModalBottomSheet(
            onDismissRequest = { showBottomSheet = false },
            sheetState = bottomSheetState,
            containerColor = MaterialTheme.colorScheme.surface, // Aquí es donde va el color
            contentColor = Color.White
        ) {
            HunterInventorySheet(
                exercises = filteredExercises,
                onExerciseSetSelect = { exercise, set ->
                    viewModel.addExercise(exercise.id, set.id)
                    showBottomSheet = false
                },
                onDismiss = { showBottomSheet = false },
                selectedCategories = selectedCategories
            )
        }
    }
}

// ============ COMPONENTES HUNTER ============

@Composable
private fun HunterMissionStatus(
    completed: Boolean,
    onToggle: () -> Unit
) {
    HunterCard(
        onClick = onToggle,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(
                    text = "ESTADO DE MISIÓN",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = if (completed) "CUMPLIDA" else "PENDIENTE",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Black, letterSpacing = 1.sp),
                    color = if (completed) HunterCyan else HunterSecondary
                )
            }

            // Switch estilo Hunter
            Box(
                modifier = Modifier
                    .size(width = 60.dp, height = 32.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(if (completed) HunterCyan.copy(alpha = 0.2f) else MaterialTheme.colorScheme.surfaceVariant)
                    .border(1.dp, if (completed) HunterCyan else MaterialTheme.colorScheme.outline.copy(0.3f), RoundedCornerShape(16.dp))
            ) {
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .align(if (completed) Alignment.CenterEnd else Alignment.CenterStart)
                        .padding(4.dp)
                        .clip(CircleShape)
                        .background(if (completed) HunterCyan else MaterialTheme.colorScheme.onSurfaceVariant)
                )
            }
        }
    }
}

@Composable
private fun HunterExpandableSection(
    title: String,
    isExpanded: Boolean,
    onToggle: () -> Unit,
    badgeCount: Int? = null,
    content: @Composable () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f), RoundedCornerShape(16.dp))
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surface)
    ) {
        // Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onToggle() }
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                    color = HunterPrimary
                )
                if (badgeCount != null && badgeCount > 0) {
                    Spacer(modifier = Modifier.width(8.dp))
                    Badge(containerColor = HunterPrimary) { Text("$badgeCount") }
                }
            }
            Icon(
                imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        // Content
        if (isExpanded) {
            Box(modifier = Modifier.padding(16.dp)) {
                content()
            }
        }
    }
}

@Composable
private fun CategorySelectorGrid(
    selectedCategories: Set<DayCategory>,
    onToggleCategory: (DayCategory) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        val categories = DayCategory.values().toList()
        categories.chunked(2).forEach { rowCategories ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                rowCategories.forEach { category ->
                    val isSelected = selectedCategories.contains(category)

                    // Chip personalizado
                    Surface(
                        modifier = Modifier
                            .weight(1f)
                            .height(40.dp)
                            .clickable { onToggleCategory(category) },
                        shape = RoundedCornerShape(8.dp),
                        color = if (isSelected) HunterPrimary.copy(alpha = 0.2f) else MaterialTheme.colorScheme.background,
                        border = BorderStroke(1.dp, if (isSelected) HunterPrimary else MaterialTheme.colorScheme.outline.copy(0.3f))
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = category.displayName.uppercase(),
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = if (isSelected) HunterPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
                if (rowCategories.size == 1) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun LoadoutContent(
    exercisesWithSets: List<ExerciseWithSelectedSet>,
    hasCategories: Boolean,
    isCardioOrRest: Boolean,
    onAddExercise: () -> Unit,
    onExerciseClick: (String) -> Unit,
    onRemoveExercise: (String) -> Unit,
    onMoveExercise: (Int, Int) -> Unit
) {
    when {
        !hasCategories -> {
            Text(
                "⚠️ Selecciona una clase de combate primero.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        isCardioOrRest -> {
            Text(
                "Día de recuperación o resistencia. No se requiere equipamiento pesado.",
                style = MaterialTheme.typography.bodyMedium,
                color = HunterCyan
            )
        }
        else -> {
            // Botón Añadir (Estilo Slot vacío)
            OutlinedButton(
                onClick = onAddExercise,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, HunterPrimary.copy(alpha = 0.5f)),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = HunterPrimary)
            ) {
                Icon(Icons.Default.Add, null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("EQUIPAR HABILIDAD", fontWeight = FontWeight.Bold)
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (exercisesWithSets.isNotEmpty()) {
                DraggableLoadoutList(
                    items = exercisesWithSets,
                    onExerciseClick = onExerciseClick,
                    onRemoveExercise = onRemoveExercise,
                    onMoveExercise = onMoveExercise
                )
            } else {
                Text(
                    "Slots vacíos.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
            }
        }
    }
}

@Composable
private fun DraggableLoadoutList(
    items: List<ExerciseWithSelectedSet>,
    onExerciseClick: (String) -> Unit,
    onRemoveExercise: (String) -> Unit,
    onMoveExercise: (Int, Int) -> Unit
) {
    var draggedIndex by remember { mutableStateOf<Int?>(null) }
    var targetIndex by remember { mutableStateOf<Int?>(null) }
    val cardPositions = remember { mutableStateMapOf<Int, Float>() }
    var currentDragY by remember { mutableFloatStateOf(0f) }

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items.forEachIndexed { index, item ->
            HunterDraggableCard(
                item = item,
                index = index,
                isDragged = draggedIndex == index,
                isDraggedOver = targetIndex == index,
                onClick = { onExerciseClick(item.exercise.id) },
                onRemove = { onRemoveExercise(item.compositeId) },
                onDragStart = { positionY ->
                    draggedIndex = index
                    targetIndex = index
                    currentDragY = positionY
                },
                onDragEnd = {
                    draggedIndex?.let { from ->
                        targetIndex?.let { to ->
                            if (from != to) onMoveExercise(from, to)
                        }
                    }
                    draggedIndex = null
                    targetIndex = null
                    currentDragY = 0f
                },
                onDrag = { dragAmount ->
                    currentDragY += dragAmount
                    draggedIndex?.let { draggedIdx ->
                        val draggedCenter = currentDragY + 35f // Mitad aprox de la altura
                        var closestIndex = draggedIdx
                        var minDistance = Float.MAX_VALUE

                        cardPositions.forEach { (idx, posY) ->
                            if (idx != draggedIdx) {
                                val cardCenter = posY + 35f
                                val distance = kotlin.math.abs(draggedCenter - cardCenter)
                                if (distance < minDistance && distance < 60f) {
                                    minDistance = distance
                                    closestIndex = idx
                                }
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HunterDraggableCard(
    item: ExerciseWithSelectedSet,
    index: Int,
    isDragged: Boolean,
    isDraggedOver: Boolean,
    onClick: () -> Unit,
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
        else -> MaterialTheme.colorScheme.background
    }

    val borderColor = if (isDragged) HunterPrimary else MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
    val zIndex = if (isDragged) 10f else 0f

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .zIndex(zIndex)
            .offset(y = with(density) { localDragOffset.toDp() })
            .onGloballyPositioned { coordinates ->
                cardPositionY = coordinates.positionInParent().y
                onPositionCalculated(cardPositionY)
            },
        shape = RoundedCornerShape(12.dp),
        color = backgroundColor,
        border = BorderStroke(1.dp, borderColor)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Drag Handle
            Icon(
                imageVector = Icons.Default.DragHandle,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier
                    .size(24.dp)
                    .pointerInput(item.compositeId) {
                        detectDragGesturesAfterLongPress(
                            onDragStart = {
                                onDragStart(cardPositionY)
                                localDragOffset = 0f
                            },
                            onDragEnd = {
                                onDragEnd()
                                localDragOffset = 0f
                            },
                            onDragCancel = {
                                onDragEnd()
                                localDragOffset = 0f
                            },
                            onDrag = { change, dragAmount ->
                                change.consume()
                                localDragOffset += dragAmount.y
                                onDrag(dragAmount.y)
                            }
                        )
                    }
            )

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f).clickable { onClick() }) {
                Text(
                    text = item.exercise.name.uppercase(),
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                if (item.selectedSet != null) {
                    Text(
                        text = "VARIANTE: ${item.selectedSet.series}×${item.selectedSet.reps} @ ${item.selectedSet.weightKg}KG",
                        style = MaterialTheme.typography.labelSmall,
                        color = HunterPrimary
                    )
                } else {
                    Text(
                        text = "SIN CONFIGURAR",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }

            IconButton(onClick = onRemove) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Eliminar",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

@Composable
private fun HunterInventorySheet(
    exercises: List<Exercise>,
    onExerciseSetSelect: (Exercise, GymSet) -> Unit,
    onDismiss: () -> Unit,
    selectedCategories: Set<DayCategory>
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "INVENTARIO DE HABILIDADES",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Black),
                color = Color.White
            )
            IconButton(onClick = onDismiss) {
                Icon(Icons.Default.Close, contentDescription = "Cerrar", tint = Color.White)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (exercises.isEmpty()) {
            Text("No hay ejercicios disponibles para esta clase.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }

        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.heightIn(max = 400.dp)
        ) {
            itemsIndexed(exercises) { index, exercise ->
                var isSetsExpanded by remember { mutableStateOf(false) }
                val hasSets = exercise.sets.isNotEmpty()

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.background,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)),
                    modifier = Modifier.clickable {
                        if (hasSets) isSetsExpanded = !isSetsExpanded
                    }
                ) {
                    Column {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = exercise.name,
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                color = Color.White
                            )
                            Icon(
                                imageVector = if (isSetsExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                contentDescription = null,
                                tint = if (hasSets) HunterPrimary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)
                            )
                        }

                        if (isSetsExpanded) {
                            Divider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.1f))
                            exercise.sets.forEach { set ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { onExerciseSetSelect(exercise, set) }
                                        .padding(16.dp)
                                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.1f)),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("VARIANTE", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text(
                                        "${set.series}×${set.reps} @ ${set.weightKg}KG",
                                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                                        color = HunterPrimary
                                    )
                                }
                                Divider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.05f))
                            }
                        }
                    }
                }
            }
        }
        Spacer(modifier = Modifier.height(32.dp))
    }
}