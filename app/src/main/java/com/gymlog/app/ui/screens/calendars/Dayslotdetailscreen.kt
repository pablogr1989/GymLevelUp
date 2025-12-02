package com.gymlog.app.ui.screens.calendars

import android.util.Log
import androidx.compose.foundation.clickable // <--- AÑADIDO
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInParent
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.hilt.navigation.compose.hiltViewModel
import com.gymlog.app.data.local.entity.DayCategory
import com.gymlog.app.domain.model.Exercise
// ALIAS PARA EVITAR CONFLICTO EN LA UI
import com.gymlog.app.domain.model.Set as GymSet

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
    val bottomSheetState = rememberModalBottomSheetState()

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
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        daySlot?.dayOfWeek?.displayName ?: "Editar Día"
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Volver")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimary
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
                CircularProgressIndicator()
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Estado completado
                CompletedSection(
                    completed = completed,
                    onToggleCompleted = viewModel::toggleCompleted
                )

                // Botón comenzar entrenamiento
                if (selectedExercisesWithSets.isNotEmpty()) {
                    Button(
                        onClick = viewModel::startTraining,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF4CAF50)
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = null,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Comenzar Entrenamiento",
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                // Selección de categorías (expandible)
                ExpandableCategoriesSection(
                    selectedCategories = selectedCategories,
                    onToggleCategory = viewModel::toggleCategory,
                    isExpanded = isCategoriesExpanded,
                    onToggleExpansion = viewModel::toggleCategoriesExpansion
                )

                // Ejercicios seleccionados (expandible con drag & drop)
                ExpandableExercisesSection(
                    exercisesWithSets = selectedExercisesWithSets,
                    onRemoveExercise = viewModel::removeExercise,
                    onExerciseClick = onNavigateToExercise,
                    onAddExercise = { showBottomSheet = true },
                    onMoveExercise = viewModel::moveExercise,
                    hasCategories = selectedCategories.isNotEmpty(),
                    selectedCategories = selectedCategories,
                    isExpanded = isExercisesExpanded,
                    onToggleExpansion = viewModel::toggleExercisesExpansion
                )

                // Botón guardar
                Button(
                    onClick = viewModel::saveDaySlot,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    enabled = !isLoading
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            color = MaterialTheme.colorScheme.onPrimary,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.Save,
                            contentDescription = null,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Guardar cambios",
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }

    // Bottom Sheet para seleccionar ejercicios y sus sets
    if (showBottomSheet) {
        ModalBottomSheet(
            onDismissRequest = { showBottomSheet = false },
            sheetState = bottomSheetState
        ) {
            ExercisePickerBottomSheet(
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

@Composable
private fun CompletedSection(
    completed: Boolean,
    onToggleCompleted: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .toggleable(
                    value = completed,
                    role = Role.Checkbox,
                    onValueChange = { onToggleCompleted() }
                )
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(
                checked = completed,
                onCheckedChange = null
            )
            Column {
                Text(
                    text = "Día completado",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = if (completed) "Este día ha sido marcado como completado"
                    else "Marca como completado cuando termines el entrenamiento",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun ExpandableCategoriesSection(
    selectedCategories: Set<DayCategory>,
    onToggleCategory: (DayCategory) -> Unit,
    isExpanded: Boolean,
    onToggleExpansion: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .toggleable(
                        value = isExpanded,
                        onValueChange = { onToggleExpansion() }
                    ),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Categorías de entrenamiento",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Icon(
                    imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = if (isExpanded) "Contraer" else "Expandir"
                )
            }

            if (isExpanded) {
                Text(
                    text = "Selecciona una o más categorías para este día",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                val categories = DayCategory.values().toList()
                categories.chunked(2).forEach { rowCategories ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        rowCategories.forEach { category ->
                            FilterChip(
                                onClick = { onToggleCategory(category) },
                                label = { Text(category.displayName) },
                                selected = selectedCategories.contains(category),
                                modifier = Modifier.weight(1f)
                            )
                        }
                        if (rowCategories.size == 1) {
                            Spacer(modifier = Modifier.weight(1f))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ExpandableExercisesSection(
    exercisesWithSets: List<ExerciseWithSelectedSet>,
    onRemoveExercise: (String) -> Unit,
    onExerciseClick: (String) -> Unit,
    onAddExercise: () -> Unit,
    onMoveExercise: (Int, Int) -> Unit,
    hasCategories: Boolean,
    selectedCategories: Set<DayCategory>,
    isExpanded: Boolean,
    onToggleExpansion: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .toggleable(
                        value = isExpanded,
                        onValueChange = { onToggleExpansion() }
                    ),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Ejercicios del día",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    if (exercisesWithSets.isNotEmpty()) {
                        Badge {
                            Text(exercisesWithSets.size.toString())
                        }
                    }
                }
                Icon(
                    imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = if (isExpanded) "Contraer" else "Expandir"
                )
            }

            if (isExpanded) {
                when {
                    !hasCategories -> {
                        Text(
                            text = "Selecciona primero una categoría para añadir ejercicios",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    selectedCategories.all { it == DayCategory.CARDIO || it == DayCategory.REST } -> {
                        Text(
                            text = "Días de cardio o descanso - No se requieren ejercicios específicos de musculación",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    else -> {
                        OutlinedButton(
                            onClick = onAddExercise,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Añadir ejercicio")
                        }

                        if (exercisesWithSets.isNotEmpty()) {
                            DraggableExerciseList(
                                items = exercisesWithSets,
                                onExerciseClick = onExerciseClick,
                                onRemoveExercise = onRemoveExercise,
                                onMoveExercise = onMoveExercise
                            )
                        } else {
                            Text(
                                text = "No hay ejercicios añadidos para este día",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}


@Composable
private fun DraggableExerciseList(
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
            DraggableExerciseCard(
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
                            if (from != to) {
                                onMoveExercise(from, to)
                            }
                        }
                    }
                    draggedIndex = null
                    targetIndex = null
                    currentDragY = 0f
                },
                onDrag = { dragAmount ->
                    currentDragY += dragAmount
                    draggedIndex?.let { draggedIdx ->
                        val draggedCenter = currentDragY + 40f
                        var closestIndex = draggedIdx
                        var minDistance = Float.MAX_VALUE

                        cardPositions.forEach { (idx, posY) ->
                            if (idx != draggedIdx) {
                                val cardCenter = posY + 40f
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
private fun DraggableExerciseCard(
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
        isDragged -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f)
        isDraggedOver -> MaterialTheme.colorScheme.secondaryContainer
        else -> MaterialTheme.colorScheme.surface
    }

    Card(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .zIndex(if (isDragged) 10f else 0f)
            .offset(y = with(density) { localDragOffset.toDp() })
            .onGloballyPositioned { coordinates ->
                cardPositionY = coordinates.positionInParent().y
                onPositionCalculated(cardPositionY)
            }
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
            },
        colors = CardDefaults.cardColors(containerColor = backgroundColor),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isDragged) 8.dp else 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.DragHandle,
                contentDescription = null,
                tint = if (isDragged) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(24.dp)
            )

            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 12.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = item.exercise.name,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    Text(
                        text = item.exercise.muscleGroup.displayName,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    // Mostrar info del set seleccionado
                    if (item.selectedSet != null) {
                        Text(
                            text = "${item.selectedSet.series}×${item.selectedSet.reps} @ ${item.selectedSet.weightKg}kg",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold
                        )
                    } else {
                        Text(
                            text = "Set sin definir",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }

            IconButton(onClick = onRemove) {
                Icon(
                    imageVector = Icons.Default.Remove,
                    contentDescription = "Eliminar",
                    tint = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}

@Composable
private fun ExercisePickerBottomSheet(
    exercises: List<Exercise>,
    onExerciseSetSelect: (Exercise, GymSet) -> Unit, // Usamos el alias GymSet
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
                text = "Seleccionar ejercicio",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            IconButton(onClick = onDismiss) {
                Icon(Icons.Default.Close, contentDescription = "Cerrar")
            }
        }

        if (selectedCategories.contains(DayCategory.FULL_BODY)) {
            Text(
                text = "Mostrando todos los ejercicios (Full Body)",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(bottom = 16.dp)
            )
        } else {
            Text(
                text = "Ejercicios filtrados por: ${selectedCategories.joinToString { it.displayName }}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 16.dp)
            )
        }

        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.heightIn(max = 400.dp)
        ) {
            itemsIndexed(exercises) { index, exercise ->
                // Estado para expandir los sets de este ejercicio
                var isSetsExpanded by remember { mutableStateOf(false) }

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Column {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { isSetsExpanded = !isSetsExpanded }
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = exercise.name,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Medium
                                )
                                Text(
                                    text = exercise.muscleGroup.displayName,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Icon(
                                imageVector = if (isSetsExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                contentDescription = null
                            )
                        }

                        if (isSetsExpanded) {
                            Divider()
                            if (exercise.sets.isEmpty()) {
                                Text(
                                    text = "Este ejercicio no tiene sets configurados.",
                                    modifier = Modifier.padding(16.dp),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.error
                                )
                            } else {
                                exercise.sets.forEachIndexed { idx, set ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable { onExerciseSetSelect(exercise, set) }
                                            .padding(horizontal = 24.dp, vertical = 12.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text("Variante #${idx + 1}", style = MaterialTheme.typography.bodySmall)
                                        Text(
                                            "${set.series}x${set.reps} @ ${set.weightKg}kg",
                                            style = MaterialTheme.typography.bodySmall,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                    if (idx < exercise.sets.size - 1) Divider(modifier = Modifier.padding(horizontal = 16.dp))
                                }
                            }
                        }
                    }
                }
            }
        }
        Spacer(modifier = Modifier.height(32.dp))
    }
}